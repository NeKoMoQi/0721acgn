# -*- coding: utf-8 -*-
"""
从 Bangumi 开放检索拉取条目标签/分区，写入 tb_animation.genre，并为每条番剧补全 tb_related_work
（每条番剧关联 1 部漫画 + 1 部轻小说：优先标题子串命中，其次确定性散列）。
需已安装 mysql 客户端；运行： py -3 sync_animation_from_bgm.py [--apply]
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
from collections import defaultdict
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SQL_OUT = ROOT / "sql" / "acgn_animation_bgm_bulk.sql"


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


def bangumi_search(keyword: str, timeout: float = 18.0) -> Optional[dict]:
    keyword = keyword.strip()
    if not keyword:
        return None
    body = json.dumps({"keyword": keyword, "filter": {"type": [2]}}).encode("utf-8")
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
    except urllib.error.HTTPError:
        return None
    except urllib.error.URLError:
        return None
    items = data.get("data") or []
    return items[0] if items else None


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


def normalize_title(raw: str) -> str:
    t = raw.strip()
    t = re.sub(r"[\[【](.*?)[\]】]", "", t)
    t = t.replace(" 中配版", "").replace(" 粤配版", "").replace("日语版", "")
    return t.strip() or raw.strip()


def stem_for_search(title: str) -> str:
    t = normalize_title(title)
    t = re.sub(
        r"\s*第?[一二三四五六七八九十百千0-9]+(?:季|期|篇|話|Part|PART).*$",
        "",
        t,
    )
    t = re.sub(r"\s+(剧场版|总集篇|OVA\d*|OVA|SP).*$", "", t, flags=re.I)
    t = t.strip() or normalize_title(title)
    return (t[:36] + "…") if len(t) > 36 else t


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
    ap.add_argument("--delay", type=float, default=0.35, help="Bangumi 请求间隔(秒)")
    ap.add_argument(
        "--limit",
        type=int,
        default=0,
        help="仅处理前 N 条（0=全部），调试用",
    )
    ap.add_argument("--out", type=Path, default=DEFAULT_SQL_OUT)
    ap.add_argument(
        "--apply",
        action="store_true",
        help="成功后自动 mysql 导入（否则只写 sql 文件）",
    )
    args = ap.parse_args()
    if not args.mysql_pass:
        print(
            "未设置 MYSQL 密码：请传 --mysql-pass 或环境变量 BISHE_MYSQL_PASSWORD。",
            file=sys.stderr,
        )
        return 2

    try:
        out_anim = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT id,name FROM tb_animation ORDER BY id",
        )
        out_cm = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT comicid,name FROM tb_cartoons",
        )
        out_nv = mysql_query(
            args.mysql_user,
            args.mysql_pass,
            args.mysql_db,
            "SELECT novelid,name FROM tb_novel",
        )
    except subprocess.CalledProcessError:
        print("mysql 读取失败：请确认本机 PATH 里有 mysql.exe，用户名密码正确。", file=sys.stderr)
        return 2

    animations: List[Tuple[int, str]] = []
    for line in out_anim.strip().split("\n"):
        if not line or "\t" not in line:
            continue
        i, nm = line.split("\t", 1)
        animations.append((int(i), nm.strip()))
    if args.limit > 0:
        animations = animations[: args.limit]

    comics = []
    for line in out_cm.strip().split("\n"):
        if not line or "\t" not in line:
            continue
        i, nm = line.split("\t", 1)
        comics.append((int(i), nm.strip()))

    novels = []
    for line in out_nv.strip().split("\n"):
        if not line or "\t" not in line:
            continue
        i, nm = line.split("\t", 1)
        novels.append((int(i), nm.strip()))

    lc, ln = len(comics), len(novels)
    if not lc or not ln:
        print("漫画或小说表为空，中止。", file=sys.stderr)
        return 2

    genres_by_id: Dict[int, str] = {}
    rel_rows: List[Tuple[int, int, int]] = []

    for aid, raw_name in animations:
        name_norm = normalize_title(raw_name)

        cid = longest_substring_pick(name_norm, comics)
        nid = longest_substring_pick(name_norm, novels)
        if cid is None:
            cid = comics[(aid * 9973) % lc][0]
        if nid is None:
            nid = novels[(aid * 7919) % ln][0]
        rel_rows.append((aid, cid, nid))

    stems: defaultdict[str, List[Tuple[int, str]]] = defaultdict(list)
    for aid, raw_name in animations:
        key = stem_for_search(raw_name).strip()
        if not key:
            key = normalize_title(raw_name).strip()[:24] or raw_name.strip()[:24]
        stems[key].append((aid, raw_name))

    print(f"Bangumi 分组数（去重关键词）={len(stems)}，每条间隔 {args.delay}s，请耐心等待。")
    genre_by_stem: Dict[str, str] = {}
    for num, (key, members) in enumerate(stems.items(), start=1):
        if num % 25 == 0 or num == 1:
            print(f" … Bangumi [{num}/{len(stems)}]", flush=True)
        time.sleep(max(args.delay, 0.0))
        item = bangumi_search(key)
        gs = stringify_genres(item) if item else ""
        sample_title = members[0][1].strip()
        if not gs and sample_title.strip()[:28] != key:
            time.sleep(max(args.delay, 0.0))
            item2 = bangumi_search(sample_title.strip()[:28])
            gs = stringify_genres(item2) if item2 else ""
        genre_by_stem[key] = gs

    for aid, raw_name in animations:
        key = stem_for_search(raw_name).strip()
        if not key:
            key = normalize_title(raw_name).strip()[:24] or raw_name.strip()[:24]
        name_norm = normalize_title(raw_name)
        g = (genre_by_stem.get(key) or "").strip()
        genres_by_id[aid] = g[:500] if g else heuristic_genre(name_norm)

    sql_lines = [
        "SET NAMES utf8mb4;",
        "SET SESSION sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));",
        "USE `" + sql_escape(args.mysql_db) + "`;",
        "",
        "ALTER TABLE `tb_animation` MODIFY COLUMN `genre` VARCHAR(512) NULL DEFAULT NULL COMMENT '题材/Bangumi标签摘要';",
        "",
        "DELETE FROM `tb_related_work`;",
        "",
    ]

    ins_parts: List[str] = []
    for aid, cid, nid in rel_rows:
        ins_parts.append(
            f"('animation',{aid},'comic',{cid}),('animation',{aid},'novel',{nid})"
        )
    for chunk in batched(ins_parts, 120):
        sql_lines.append(
            "INSERT IGNORE INTO `tb_related_work` (`src_type`,`src_id`,`dst_type`,`dst_id`) VALUES\n"
            + ",\n".join(chunk)
            + ";"
        )

    ids = list(genres_by_id.keys())
    case_chunk = 80
    for i in range(0, len(ids), case_chunk):
        part = ids[i : i + case_chunk]
        when = " ".join(
            f"WHEN {sid} THEN '{sql_escape(genres_by_id[sid])}'" for sid in part
        )
        in_list = ",".join(str(x) for x in part)
        sql_lines.append(
            f"UPDATE `tb_animation` SET `genre` = CASE `id` {when} ELSE `genre` END WHERE `id` IN ({in_list});"
        )

    sql_lines.append("")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(sql_lines), encoding="utf-8")
    print(f"已写入: {args.out}  （番剧 {len(animations)} 条）")

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
            subprocess.run(
                cmd,
                input=body,
                check=True,
                stderr=subprocess.PIPE,
            )
        except subprocess.CalledProcessError as e:
            print(e.stderr.decode("utf-8", errors="replace") if e.stderr else str(e), file=sys.stderr)
            return 3
        print("导入完成。")

    return 0


def heuristic_genre(title: str) -> str:
    """Bangumi 无结果时的兜底关键字。"""
    rules: Sequence[Tuple[Tuple[str, ...], Tuple[str, ...]]] = (
        (
            ("转生", "异世界", "迷宫", "魔剑", "谭雅", "史莱姆", "无职", "魔王"),
            ("奇幻", "异世界"),
        ),
        (("恋爱", "告白", "婚纱", "女友", "婚", "大小姐", "学姐", "堀"), ("恋爱", "校园")),
        (("咒术", "鬼灭", "一拳", "进击", "死神", "海盗", "战记"), ("战斗", "热血")),
        (
            ("科幻", "赛博", "未来", "机战", "高达", "EVA"),
            ("科幻", "机战"),
        ),
        (
            ("推理", "侦探", "柯南", "替身", "夏日重现"),
            ("悬疑"),
        ),
        (
            ("日常", "龙女仆", "摇曳", "露营", "悠哉"),
            ("日常"),
        ),
        (("排球", "足球", "篮球", "赛马", "疾走"), ("运动")),
        (("间谍", "任务", "特工"), ("奇幻", "搞笑")),
        (("细胞", "工作细胞"), ("科普", "日常")),
    )
    tags: List[str] = []
    for keys, lbls in rules:
        if any(k in title for k in keys):
            tags.extend(lbls)
    if not tags and title:
        tags = ["TV动画"]
    return ",".join(dict.fromkeys(tags))[:240]


if __name__ == "__main__":
    raise SystemExit(main())
