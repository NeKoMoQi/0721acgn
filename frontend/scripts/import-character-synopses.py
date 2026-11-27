"""Refresh attributable Bangumi wiki introductions for the characters already in 0721.

Usage: python frontend/scripts/import-character-synopses.py ARCHIVE.zip [--report REPORT.json]

Bangumi's editorial material is CC BY-SA 3.0. The source, license and edits are
credited in the site UI. Keep this output separate from editorial biographies.
"""
import argparse
from collections import Counter
from html import unescape
import json
from pathlib import Path
import re
import zipfile

DATA = Path(__file__).resolve().parents[1] / "public" / "data" / "characters"
BUCKETS = 32
# https://github.com/bangumi/common/blob/master/person_relations.yml
RELATIONS = {
    2001: "形态", 2002: "朋友", 2003: "恋人", 2004: "单恋", 2005: "对手",
    2006: "亲属", 2007: "成员", 2008: "创始人", 2009: "主人", 2010: "宠物",
    2011: "学生", 2012: "老师", 2015: "驾驶员", 2016: "侍从", 2017: "配偶",
    2018: "下属", 2019: "上司", 2020: "改编版本", 2021: "原型", 2022: "前任",
    2023: "同事", 2024: "亲戚", 2025: "同学", 2026: "同门", 2027: "进化",
    2028: "前身", 2029: "饲主", 2099: "相关角色",
}
SOURCE_FIELD = re.compile(r"^[ \t]*\|[ \t]*(?:引用来源|資料來源|资料来源|出典|引用元)[ \t]*=[ \t]*([^\r\n]*)", re.M)
SOURCE_NOTE = re.compile(
    r"(?im)^\s*[（(【\[]?\s*(?:(?:简介|介紹|介绍|内容|內容|本文|訳文)\s*)?"
    r"(?:转载自|轉載自|轉自|转自|摘自|引用自|译自|譯自|"
    r"(?:转载|轉載|資料來源|资料来源|転載|引用元|出典|翻[译譯]|来源|來源)\s*(?:[:：]|自|于|於|https?://))"
)
IMAGE_BLOCK = re.compile(r"(?is)\[(img|spoiler)(?:=[^\]]*)?\].*?\[/\1\]")
LAYOUT = re.compile(r"(?i)\[/?(?:b|i|u|s|color|size|font|quote|url|align|center|left|right|list|code)(?:=[^\]]*)?\]")


def synopsis_for(row):
    raw = unescape(row.get("summary") or "").strip()
    if not raw:
        return None, "no_text"
    sources = SOURCE_FIELD.findall(row.get("infobox") or "")
    if any(source.strip() not in {"", "-", "—", "无", "無", "暂无", "未填写"} for source in sources):
        return None, "external_attribution"
    if SOURCE_NOTE.search(raw) or re.search(r"(?i)https?://|\[url\s*=", raw):
        return None, "external_attribution"
    plain = re.sub(r"<br\s*/?>", "\n", raw, flags=re.I)
    plain = LAYOUT.sub("", IMAGE_BLOCK.sub("", plain))
    plain = re.sub(r"(?im)^\s*(?:[\-—=*_•·]|\[\*\]){1,}\s*$", "", plain)
    plain = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f\u202a-\u202e\u2066-\u2069]", "", plain)
    paragraphs = [line.strip() for line in plain.splitlines() if line.strip()]
    if sum(ch.isalnum() for ch in "".join(paragraphs)) < 8:
        return None, "too_short"
    return paragraphs, "imported"


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("archive", type=Path)
    cli.add_argument("--report", type=Path)
    args = cli.parse_args()
    characters = {
        int(key): profile["person"]
        for path in DATA.glob("profile-*.json")
        for key, profile in json.loads(path.read_text(encoding="utf-8")).items()
    }
    if not characters:
        cli.error("generate the character archive first")
    buckets = [{} for _ in range(BUCKETS)]
    status = {}
    with zipfile.ZipFile(args.archive) as archive:
        with archive.open("character.jsonlines") as rows:
            for line in rows:
                row = json.loads(line)
                id = row["id"]
                if id not in characters:
                    continue
                paragraphs, status[id] = synopsis_for(row)
                if paragraphs:
                    buckets[id % BUCKETS][str(id)] = {"paragraphs": paragraphs}
        seen = set()
        with archive.open("person-relations.jsonlines") as rows:
            for line in rows:
                row = json.loads(line)
                id, target, kind = row["person_id"], row["related_person_id"], row["relation_type"]
                key = (id, target, kind)
                if (row["person_type"] != "crt" or row["spoiler"] or id == target or id not in characters
                        or target not in characters or kind not in RELATIONS or key in seen):
                    continue
                seen.add(key)
                profile = buckets[id % BUCKETS].setdefault(str(id), {})
                person = {field: characters[target][field] for field in ("id", "name", "siteId", "game")
                          if field in characters[target]}
                profile.setdefault("relations", []).append([RELATIONS[kind], person, bool(row["ended"])])
    counts = Counter(status.get(id, "not_in_snapshot") for id in characters)
    if not counts["imported"]:
        cli.error("the archive had no usable introductions for these characters")
    for i, bucket in enumerate(buckets):
        target = DATA / f"synopsis-{i}.json"
        target.write_text(json.dumps(bucket, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    if args.report:
        misses = [
            {"id": id, "name": person["name"], "reason": status.get(id, "not_in_snapshot")}
            for id, person in sorted(characters.items())
            if status.get(id) != "imported"
        ]
        args.report.write_text(json.dumps({"counts": counts, "misses": misses}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Audited", len(characters), "characters:", dict(counts))
    print("Non-spoiler relationships", len(seen), "across", len({row[0] for row in seen}), "characters")


if __name__ == "__main__":
    main()
