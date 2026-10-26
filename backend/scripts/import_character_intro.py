"""从萌娘百科导入角色简介到 tb_acgnuser.intro / introsource。

用法：
  python import_character_intro.py --dry-run --limit 5
  python import_character_intro.py [--force] [--batch 20]

内容来自萌娘百科（CC BY-NC-SA 3.0 CN），只取导语和「简介」小节，并记录来源链接。
同名/消歧义页会再单独解析一次。
"""
import argparse
import html
import json
import os
import re
import sys
import time
import urllib.parse
from pathlib import Path

import pymysql
import requests

API = "https://zh.moegirl.org.cn/api.php"
ARTICLE = "https://zh.moegirl.org.cn/"
HEADERS = {"User-Agent": "0721acgn-character-import/1.0 (site: 0721acgn; contact: site owner)"}
# 数据库口令只从环境变量读，不写进仓库。
DB = dict(host=os.getenv("MYSQL_HOST", "127.0.0.1"),
          port=int(os.getenv("MYSQL_PORT", "3306")),
          user=os.getenv("MYSQL_USER", "root"),
          password=os.environ["MYSQL_PASSWORD"],
          database=os.getenv("MYSQL_DATABASE", "0721acgn"),
          charset="utf8mb4")
INTRO_SECTIONS = ("简介", "角色简介", "角色介绍", "人物简介", "人物介绍", "角色背景", "人物设定", "设定")


def api(params, retries=4):
    params = {**params, "action": "query", "format": "json", "formatversion": "2"}
    for attempt in range(retries):
        try:
            response = requests.get(API, params=params, headers=HEADERS, timeout=40)
            if response.status_code == 429:
                time.sleep(6 * (attempt + 1))
                continue
            response.raise_for_status()
            return response.json()
        except Exception:
            if attempt == retries - 1:
                raise
            time.sleep(3 * (attempt + 1))
    return {}


def _useful(line):
    return not line.startswith(("{{", "|", "[[", "<", "分类:", "Category:"))


def clean(text, limit=900):
    """取导语 + 「简介」小节，够一段像样的介绍，又不整篇搬运。"""
    text = re.sub(r"\n{3,}", "\n\n", text or "")
    lead, blocks, heading = [], {}, None
    for raw in (line.strip() for line in text.split("\n")):
        if not raw:
            continue
        if raw.startswith("=="):
            heading = raw.strip("= ").strip()
            blocks.setdefault(heading, [])
            continue
        if not _useful(raw):
            continue
        if heading is None:
            lead.append(raw)
        else:
            blocks[heading].append(raw)

    paragraphs = list(lead)
    for name in INTRO_SECTIONS:
        if blocks.get(name):
            paragraphs.extend(blocks[name])
            break

    kept, total = [], 0
    for paragraph in paragraphs:
        kept.append(paragraph)
        total += len(paragraph)
        if total >= limit:
            break
    return "\n\n".join(kept)


def is_disambiguation(text):
    head = (text or "")[:200]
    return "可以指" in head or "消歧义" in head


ROLE_WORDS = ("角色", "登场", "人物", "女主", "男主", "主角", "主人公", "声优", "CV")


def plausible(text):
    """只认像角色条目的正文，避免把同名歌曲/公司/作品条目塞进角色页。"""
    head = (text or "")[:300]
    return any(word in head for word in ROLE_WORDS)


def disambig_candidates(text, name):
    """从消歧义页取候选标题，兼容「角色(作品)」和「作品:角色」两种写法。"""
    found = []
    for line in (text or "").splitlines():
        line = line.strip()
        if "————" not in line:
            continue
        head, tail = line.split("————", 1)
        head = head.strip()
        if head == name or head.startswith(name + "(") or head.endswith(":" + name):
            found.append((head, tail))
    return found


def pick_from_disambiguation(text, name):
    candidates = disambig_candidates(text, name)
    for head, tail in candidates:
        if any(word in tail for word in ROLE_WORDS):
            return head
    return candidates[0][0] if candidates else None


def fetch_one(title):
    data = api({"titles": title, "prop": "extracts", "explaintext": "1", "redirects": "1"})
    pages = data.get("query", {}).get("pages", [])
    if not pages or pages[0].get("missing"):
        return None, None
    return pages[0].get("title"), pages[0].get("extract", "")


def fetch_batch(names):
    """一次请求拿多个条目，返回 {name: page}。"""
    data = api({"titles": "|".join(names), "prop": "extracts", "explaintext": "1",
                "redirects": "1", "exlimit": "max"})
    query = data.get("query", {})
    pages = {page["title"]: page for page in query.get("pages", [])}
    normalized = {item["from"]: item["to"] for item in query.get("normalized", [])}
    redirected = {item["from"]: item["to"] for item in query.get("redirects", [])}
    result = {}
    for name in names:
        title = normalized.get(name, name)
        title = redirected.get(title, title)
        result[name] = pages.get(title)
    return result


def search(name, limit=20):
    """API 的 list=search 已关闭，改用网页搜索结果页解析候选标题。"""
    try:
        response = requests.get("https://zh.moegirl.org.cn/index.php",
                                params={"search": name, "fulltext": "1", "ns0": "1", "limit": str(limit)},
                                headers=HEADERS, timeout=40)
        response.raise_for_status()
    except Exception:
        return []
    titles = re.findall(r'class="mw-search-result-heading"><a href="[^"]+" title="([^"]+)"', response.text)
    return [html.unescape(title) for title in titles]


def work_of(title, name):
    """从候选标题里剥出作品名，如 碧蓝航线:光辉 -> 碧蓝航线。"""
    return re.sub(r"[（(:：].*$", "", title.replace(name, "")).strip(" :：()（）")


def catalog_works():
    connection = pymysql.connect(**DB)
    try:
        with connection.cursor() as cursor:
            cursor.execute("select name from tb_animation union select name from tb_cartoons "
                           "union select name from tb_novel")
            return {row[0].replace(" ", "") for row in cursor.fetchall() if row[0]}
    finally:
        connection.close()


def resolve(page, name, relaxed=False):
    """返回 (标题, 文本, 备注)，处理缺失和消歧义；默认只收像角色条目的正文。"""
    ok = (lambda text: bool(text) and not is_disambiguation(text) and (relaxed or plausible(text)))
    text = (page or {}).get("extract", "")
    title = (page or {}).get("title")
    if ok(text):
        return title, text, "direct"
    if text and is_disambiguation(text):
        target = pick_from_disambiguation(text, name)
        if target:
            title, text = fetch_one(target)
            if ok(text):
                return title, text, "disambig"
    base = re.split(r"[(（]", name)[0].strip()
    candidates = [name] if base == name else [base]
    for candidate in candidates + search(name)[:2]:
        title, text = fetch_one(candidate)
        if ok(text):
            return title, text, "search"
    return None, "", "not-found"


def run_catalog(args):
    """补漏：从消歧义页和搜索结果拼候选标题，优先本站目录里收录过的作品。"""
    works = catalog_works()
    connection = pymysql.connect(**DB)
    added = []
    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            cursor.execute("select id, name from tb_acgnuser where intro is null or trim(intro)='' "
                           "order by cast(id as unsigned)")
            targets = [row for row in cursor.fetchall() if (row["name"] or "").strip()]
            print(f"targets: {len(targets)} catalog works: {len(works)}", flush=True)
            for index, row in enumerate(targets, 1):
                name = row["name"].strip()
                try:
                    _, text = fetch_one(name)
                    candidates = disambig_candidates(text, name) if text and is_disambiguation(text) else []
                    if not candidates:
                        candidates = [(title, "") for title in search(name) if name in title]
                    # 本站目录里出现过的作品排前面
                    def rank(item):
                        work = work_of(item[0], name).replace(" ", "")
                        if not work:
                            return 1
                        return 0 if any(work == w or work in w or w in work for w in works if len(w) >= 2) else 2
                    chosen = None
                    for head, _ in sorted(candidates, key=rank)[:6]:
                        title, body = fetch_one(head)
                        if body and not is_disambiguation(body) and plausible(body):
                            chosen = (title, body)
                            break
                    if not chosen:
                        print(f"[{index}/{len(targets)}] {name}: 无候选 ({len(candidates)})", flush=True)
                        time.sleep(args.delay)
                        continue
                    intro = clean(chosen[1])
                    source = ARTICLE + urllib.parse.quote(chosen[0].replace(" ", "_"))
                    if not args.dry_run:
                        cursor.execute("update tb_acgnuser set intro=%s, introsource=%s where id=%s",
                                       (intro, source, row["id"]))
                        connection.commit()
                    added.append((name, chosen[0]))
                    print(f"[{index}/{len(targets)}] {name} -> {chosen[0]} ({len(intro)} 字)", flush=True)
                except Exception as exc:  # noqa: BLE001
                    print(f"[{index}/{len(targets)}] {name}: ERROR {type(exc).__name__}", flush=True)
                time.sleep(args.delay)
    finally:
        connection.close()
    print("added:", len(added), flush=True)
    for name, title in added:
        print("  ", name, "->", title, flush=True)
    return 0


def run_mapping(args):
    """按 intros.json 里人工指定的条目名抓取，不做任何猜测。"""
    path = Path(args.mapping)
    if not path.is_absolute():
        path = Path(__file__).with_name(path.name)
    if not path.is_file():
        print(f"映射表不存在：{path}", flush=True)
        return 2
    mapping = json.loads(path.read_text(encoding="utf-8-sig"))
    entries = {key: value for key, value in mapping.items() if not key.startswith("_")}

    filled, skipped, failed = [], [], []
    connection = pymysql.connect(**DB)
    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            for name, article in entries.items():
                article = (article or "").strip()
                if not article:
                    skipped.append(name)
                    continue
                cursor.execute("select id from tb_acgnuser where name=%s", (name,))
                rows = cursor.fetchall()
                if not rows:
                    failed.append((name, "库里没有这个角色名"))
                    continue
                try:
                    title, text = fetch_one(article)
                except Exception as exc:  # noqa: BLE001
                    failed.append((name, type(exc).__name__))
                    continue
                if not text or is_disambiguation(text):
                    failed.append((name, f"条目「{article}」为空或仍是消歧义页"))
                    continue
                intro = clean(text)
                if not intro:
                    failed.append((name, f"条目「{article}」正文为空"))
                    continue
                source = ARTICLE + urllib.parse.quote((title or article).replace(" ", "_"))
                if not args.dry_run:
                    for row in rows:
                        cursor.execute("update tb_acgnuser set intro=%s, introsource=%s where id=%s",
                                       (intro, source, row["id"]))
                    connection.commit()
                filled.append((name, title or article, len(intro)))
                print(f"{name} -> {title or article} ({len(intro)} 字)", flush=True)
                time.sleep(args.delay)
    finally:
        connection.close()

    print(f"filled {len(filled)}  skipped {len(skipped)}  failed {len(failed)}", flush=True)
    for name, reason in failed:
        print("  FAILED", name, "|", reason, flush=True)
    for name in skipped:
        print("  SKIP", name, "| 映射为空，保持不填", flush=True)
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--batch", type=int, default=20)
    parser.add_argument("--audit", action="store_true", help="重新校验已入库的简介，清掉不像角色条目的")
    parser.add_argument("--shard", default="", help="只处理 id%%M==N 的记录，例如 0/2、1/2")
    parser.add_argument("--relaxed", action="store_true", help="放宽校验：只要求非消歧义页（用于补漏，需人工抽查）")
    parser.add_argument("--catalog", action="store_true", help="按作品名拼候选标题补漏，优先本站目录里有的作品")
    parser.add_argument("--mapping", nargs="?", const="intros.json", default="", help="按人工映射表精确抓取，默认读脚本目录下 intros.json")
    parser.add_argument("--delay", type=float, default=1.0)
    args = parser.parse_args()

    if args.catalog:
        return run_catalog(args)

    if args.mapping:
        return run_mapping(args)

    if args.audit:
        with pymysql.connect(**DB) as conn, conn.cursor() as cur:
            cur.execute("select id, name, intro from tb_acgnuser where intro is not null and trim(intro)<>''")
            rows = cur.fetchall()
            dropped = 0
            for ident, name, intro in rows:
                if not plausible(intro):
                    cur.execute("update tb_acgnuser set intro=null, introsource=null where id=%s", (ident,))
                    dropped += 1
                    print("drop", name, "|", (intro or "")[:40].replace("\n", " "), flush=True)
            conn.commit()
        print(f"audited {len(rows)} dropped {dropped}", flush=True)
        return

    connection = pymysql.connect(**DB)
    stats = {"direct": 0, "disambig": 0, "search": 0, "not-found": 0, "empty": 0}
    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            where = "" if args.force else "where intro is null or trim(intro)=''"
            limit = f"limit {args.limit}" if args.limit else ""
            cursor.execute(f"select id, name from tb_acgnuser {where} order by cast(id as unsigned) {limit}")
            targets = [row for row in cursor.fetchall() if (row["name"] or "").strip()]
            if args.shard:
                index, total = (int(part) for part in args.shard.split("/"))
                targets = [row for row in targets if int(row["id"]) % total == index]
            print(f"targets: {len(targets)}", flush=True)

            for start, row in enumerate(targets):
                name = row["name"].strip()
                try:
                    title, text = fetch_one(name)
                    title, text, note = resolve({"title": title, "extract": text}, name, args.relaxed)
                except Exception as exc:  # noqa: BLE001
                    print(f"[{start + 1}] {name}: ERROR {type(exc).__name__}", flush=True)
                    time.sleep(args.delay)
                    continue
                intro = clean(text)
                if not intro:
                    stats["not-found"] += 1
                    print(f"[{start + 1}/{len(targets)}] {name}: no text ({note})", flush=True)
                    time.sleep(args.delay)
                    continue
                stats[note] = stats.get(note, 0) + 1
                source = ARTICLE + urllib.parse.quote(title.replace(" ", "_")) if title else ""
                if not args.dry_run:
                    cursor.execute("update tb_acgnuser set intro=%s, introsource=%s where id=%s",
                                   (intro, source, row["id"]))
                    connection.commit()
                print(f"[{start + 1}/{len(targets)}] {name} -> {title} ({note}, {len(intro)} 字)", flush=True)
                time.sleep(args.delay)
    finally:
        connection.close()
    print("stats:", json.dumps(stats, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()





