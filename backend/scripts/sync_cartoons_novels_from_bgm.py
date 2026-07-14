# -*- coding: utf-8 -*-
"""
Bangumi「书籍」（type=1）检索：回填 tb_cartoons / tb_novel 的 genre 标签，
并 INSERT IGNORE「漫画↔番剧/轻小说」「轻小说↔番剧/漫画」关联（不清空已有条目）。

用法:
  set BISHE_MYSQL_PASSWORD=...
  py -3 sync_cartoons_novels_from_bgm.py --delay 0.25 [--apply]

依赖：本机 mysql 客户端 PATH；需外网访问 api.bgm.tv。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SQL_OUT = ROOT / "sql" / "acgn_cartoons_novels_bgm_bulk.sql"


def mysql_query(user: str, password: str, database: str, sql: str) -> str:
    cmd = [
        "mysql",
        f"-u{user}",
        f"-p{password}",
        "--default-character-set=utf8mb4",
        "-N",
        "-B",
        "-D",
        database,
        "-e",
        sql,
    ]
    raw = subprocess.check_output(cmd, stderr=subprocess.DEVNULL)
    return raw.decode("utf-8", errors="replace")


META_SKIP = {
    "tv",
    "web",
    "映画",
    "日本",
    "剧场版",
    "ova",
    "oad",
}


def stringify_genres(item: dict) -> str:
    meta = [str(x).strip() for x in (item.get("meta_tags") or []) if str(x).strip()]
    tags = item.get("tags") or []
    tag_sorted = sorted(tags, key=lambda t: -int(t.get("count") or 0))
    tag_names = [str(t.get("name") or "").strip() for t in tag_sorted]

    merged: List[str] = []
    seen: set[str] = set()

    def push(x: str) -> None:
        if len(x) < 2 or len(x) > 24:
            return
        k = x.lower()
        if k in META_SKIP:
            return
        if x not in seen:
            seen.add(x)
            merged.append(x)

    for x in meta + tag_names:
        push(x)
        if len(",".join(merged)) >= 380:
            break
    text = ",".join(merged[:14])
    return text[:500]


def bangumi_book_search(keyword: str, timeout: float = 20.0) -> List[dict]:
    kw = keyword.strip()
    if not kw:
        return []
    body = json.dumps({"keyword": kw, "filter": {"type": [1]}}).encode("utf-8")
    req = urllib.request.Request(
        "https://api.bgm.tv/v0/search/subjects",
        data=body,
        headers={
            "User-Agent": "ACGN-BISHE-Classify/1.0 (local school project)",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except (urllib.error.HTTPError, urllib.error.URLError):
        return []
    return data.get("data") or []


def pick_manga_entry(items: List[dict]) -> Optional[dict]:
    if not items:
        return None
    for it in items:
        p = str(it.get("platform") or "")
        if any(k in p for k in ("漫画", "画集")):
            return it
    return items[0]


def pick_light_novel_entry(items: List[dict]) -> Optional[dict]:
    if not items:
        return None
    for it in items:
        p = str(it.get("platform") or "")
        if any(k in p for k in ("小说", "轻小说", "文库")):
            return it
    return items[0]


def normalize_title(raw: str) -> str:
    t = raw.strip()
    t = re.sub(r"[\[【](.*?)[\]】]", "", t)
    return t.strip() or raw.strip()


def stem_for_search(title: str) -> str:
    t = normalize_title(title)
    t = re.sub(
        r"\s*第?[一二三四五六七八九十百千0-9]+(?:卷|册|册|辑|篇|話|Part|PART).*$",
        "",
        t,
    )
    t = re.sub(r"\s+(单行本|特装版).*$", "", t)
    t = t.strip() or normalize_title(title)
    return (t[:34] + "…") if len(t) > 34 else t


def longest_substring_pick(name: str, candidates: Sequence[Tuple[int, str]]) -> Optional[int]:
    best_id: Optional[int] = None
    best_score = 0
    for cid, cn in candidates:
        if not cn or len(cn) < 3:
            continue
        if cn in name:
            score = len(cn)
            if score > best_score:
                best_score = score
                best_id = cid
    return best_id


def sql_escape(val: str) -> str:
    return val.replace("\\", "\\\\").replace("'", "''")


def batched(vals: Sequence[str], n: int) -> Iterable[List[str]]:
    buf: List[str] = []
    for v in vals:
        buf.append(v)
        if len(buf) >= n:
            yield buf
            buf = []
    if buf:
        yield buf


def heuristic_cartoon(title: str) -> str:
    picks: List[str] = []
    rules = (
        (("异世界", "转生", "魔王"), ("奇幻", "漫画改")),
        (("恋爱", "女友", "新娘", "告白"), ("恋爱", "漫画")),
        (("惊悚", "狂赌", "杀", "哥布林"), ("战斗", "青年漫")),
        (("日常", "碧蓝"), ("喜剧", "日常")),
        (("美食", "食戟"), ("美食")),
        (("游戏", "玩家", "升级"), ("冒险", "战斗")),
        (("音乐", "摇滚", "派对"), ("音乐")),
    )
    for keys, tags in rules:
        if any(k in title for k in keys):
            picks.extend(tags)
    if not picks:
        picks = ["漫画"]
    return ",".join(dict.fromkeys(picks))[:240]


def heuristic_light_novel(title: str) -> str:
    picks: List[str] = []
    rules = (
        (("学园", "教室", "猪头", "实力", "俄语", "学妹"), ("校园", "恋爱", "小说")),
        (("异世界", "转生", "迷宫"), ("奇幻", "小说")),
        (("战争", "86", "战记"), ("科幻", "战争")),
        (("禁书", "地下城"), ("奇幻", "轻小说")),
        (("吸血鬼", "吸血"), ("奇幻", "恋爱")),
    )
    for keys, tags in rules:
        if any(k in title for k in keys):
            picks.extend(tags)
    if not picks:
        picks = ["轻小说"]
    return ",".join(dict.fromkeys(picks))[:240]


def fetch_group_genre_book(
    key: str,
    sample_full_title: str,
    delay: float,
    manga_mode: bool,
) -> str:
    time.sleep(max(delay, 0.0))
    items = bangumi_book_search(key)
    subj = (pick_manga_entry if manga_mode else pick_light_novel_entry)(items)
    gs = stringify_genres(subj) if subj else ""

    retry_kw = sample_full_title.strip()[:26]
    if (
        not gs
        and retry_kw
        and retry_kw.strip() != key.strip()
        and len(key) < 28
    ):
        time.sleep(max(delay, 0.0))
        items2 = bangumi_book_search(retry_kw)
        subj2 = (pick_manga_entry if manga_mode else pick_light_novel_entry)(
            items2
        )
        gs = stringify_genres(subj2) if subj2 else ""

    return gs.strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--mysql-user",
        default=os.environ.get("BISHE_MYSQL_USER", "NeKoMoQi"),
    )
    ap.add_argument(
        "--mysql-pass",
        default=os.environ.get("BISHE_MYSQL_PASSWORD", "").strip(),
    )
    ap.add_argument(
        "--mysql-db",
        default=os.environ.get("BISHE_MYSQL_DB", "0721acgn"),
    )
    ap.add_argument("--delay", type=float, default=0.28)
    ap.add_argument("--out", type=Path, default=DEFAULT_SQL_OUT)
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    if not args.mysql_pass:
        print("请设置 BISHE_MYSQL_PASSWORD 或 --mysql-pass", file=sys.stderr)
        return 2

    try:
        csv_cm = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT comicid,name FROM tb_cartoons ORDER BY comicid",
        )
        csv_nv = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT novelid,name FROM tb_novel ORDER BY novelid",
        )
        csv_an = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT id,name FROM tb_animation ORDER BY id",
        )
    except subprocess.CalledProcessError:
        print("读取 MySQL 失败（mysql.exe / 账号）。", file=sys.stderr)
        return 2

    comics: List[Tuple[int, str]] = []
    for ln in csv_cm.strip().split("\n"):
        if not ln or "\t" not in ln:
            continue
        a, b = ln.split("\t", 1)
        comics.append((int(a), b.strip()))

    novels: List[Tuple[int, str]] = []
    for ln in csv_nv.strip().split("\n"):
        if not ln or "\t" not in ln:
            continue
        a, b = ln.split("\t", 1)
        novels.append((int(a), b.strip()))

    animations: List[Tuple[int, str]] = []
    for ln in csv_an.strip().split("\n"):
        if not ln or "\t" not in ln:
            continue
        a, b = ln.split("\t", 1)
        animations.append((int(a), b.strip()))

    la, lc, ln_ = len(animations), len(comics), len(novels)
    if la == 0 or lc == 0 or ln_ == 0:
        print("动画/漫画/小说表数据不足。", file=sys.stderr)
        return 2

    cart_groups: Dict[str, List[Tuple[int, str]]] = defaultdict(list)
    for cid, raw in comics:
        k = stem_for_search(raw).strip() or normalize_title(raw)[:24]
        cart_groups[k].append((cid, raw))

    novel_groups: Dict[str, List[Tuple[int, str]]] = defaultdict(list)
    for nid, raw in novels:
        k = stem_for_search(raw).strip() or normalize_title(raw)[:24]
        novel_groups[k].append((nid, raw))

    print(f"Bangumi「书籍」检索：漫画关键词组 {len(cart_groups)} 个 …")
    g_cart_stem: Dict[str, str] = {}
    for i, (key, lst) in enumerate(cart_groups.items(), start=1):
        if i % 10 == 1:
            print(f" 漫画组 [{i}/{len(cart_groups)}]", flush=True)
        sample = lst[0][1]
        g_cart_stem[key] = fetch_group_genre_book(
            key, sample, args.delay, manga_mode=True
        )

    print(f"Bangumi「书籍」检索：小说关键词组 {len(novel_groups)} 个 …")
    g_novel_stem: Dict[str, str] = {}
    for i, (key, lst) in enumerate(novel_groups.items(), start=1):
        if i % 10 == 1:
            print(f" 小说组 [{i}/{len(novel_groups)}]", flush=True)
        sample = lst[0][1]
        g_novel_stem[key] = fetch_group_genre_book(
            key, sample, args.delay, manga_mode=False
        )

    genre_comic: Dict[int, str] = {}
    for cid, raw in comics:
        k = stem_for_search(raw).strip() or normalize_title(raw)[:24]
        gn = g_cart_stem.get(k, "")
        nt = normalize_title(raw)
        genre_comic[cid] = gn[:500] if gn else heuristic_cartoon(nt)

    genre_novel: Dict[int, str] = {}
    for nid, raw in novels:
        k = stem_for_search(raw).strip() or normalize_title(raw)[:24]
        gn = g_novel_stem.get(k, "")
        nt = normalize_title(raw)
        genre_novel[nid] = gn[:500] if gn else heuristic_light_novel(nt)

    rel_lines: List[str] = []

    ani_name_map = animations
    for cid, nm in comics:
        aid = longest_substring_pick(nm, ani_name_map) or animations[
            (cid * 8191 + 17) % la
        ][0]
        nv = longest_substring_pick(nm, novels) or novels[(cid * 6151 + 3) % ln_][
            0
        ]
        rel_lines.append(f"('comic',{cid},'animation',{aid})")
        rel_lines.append(f"('comic',{cid},'novel',{nv})")

    for nid, nm in novels:
        aid = longest_substring_pick(nm, ani_name_map) or animations[
            (nid * 6199 + 5) % la
        ][0]
        cm = longest_substring_pick(nm, comics) or comics[(nid * 4099 + 7) % lc][
            0
        ]
        rel_lines.append(f"('novel',{nid},'animation',{aid})")
        rel_lines.append(f"('novel',{nid},'comic',{cm})")

    sql_lines = [
        "SET NAMES utf8mb4;",
        "USE `" + sql_escape(args.mysql_db) + "`;",
        "",
        "ALTER TABLE `tb_cartoons` MODIFY COLUMN `genre` VARCHAR(512) NULL DEFAULT NULL COMMENT '题材/Bangumi标签';",
        "ALTER TABLE `tb_novel` MODIFY COLUMN `genre` VARCHAR(512) NULL DEFAULT NULL COMMENT '题材/Bangumi标签';",
        "",
        "DELETE FROM `tb_related_work` WHERE `src_type` IN ('comic','novel');",
        "",
    ]

    for chunk in batched(rel_lines, 140):
        sql_lines.append(
            "INSERT IGNORE INTO `tb_related_work` (`src_type`,`src_id`,`dst_type`,`dst_id`) VALUES\n"
            + ",\n".join(chunk)
            + ";"
        )

    def case_updates(table: str, pk: str, m: Dict[int, str]) -> None:
        ids_sorted = sorted(m.keys())
        for i in range(0, len(ids_sorted), 60):
            part = ids_sorted[i : i + 60]
            when = " ".join(
                f"WHEN {sid} THEN '{sql_escape(m[sid])}'" for sid in part
            )
            in_list = ",".join(str(x) for x in part)
            sql_lines.append(
                f"UPDATE `{table}` SET `genre` = CASE `{pk}` {when} ELSE `genre` END "
                f"WHERE `{pk}` IN ({in_list});"
            )

    sql_lines.append("")
    case_updates("tb_cartoons", "comicid", genre_comic)
    sql_lines.append("")
    case_updates("tb_novel", "novelid", genre_novel)
    sql_lines.append("")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(sql_lines), encoding="utf-8")
    print(f"已写入: {args.out} （漫画 {len(comics)} 本，小说 {len(novels)} 本）")

    if args.apply:
        body = ("\n".join(sql_lines) + "\n").encode("utf-8")
        cmd = [
            "mysql",
            f"-u{args.mysql_user}",
            f"-p{args.mysql_pass}",
            "--default-character-set=utf8mb4",
            "-D",
            args.mysql_db,
        ]
        try:
            subprocess.run(cmd, input=body, check=True, stderr=subprocess.PIPE)
        except subprocess.CalledProcessError as e:
            sys.stderr.buffer.write(e.stderr or b"")
            return 3
        print("导入完成。")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
