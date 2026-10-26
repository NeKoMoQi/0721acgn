"""把已发布文章的自绘封面换成原帖/原视频封面。

用法：python backfill_covers.py [--dry-run]
只处理能拿到 B站封面的文章；拿不到的保持原样，不重画。
"""
import argparse
import asyncio
import json
import re
import sqlite3
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parents[2]
NEWSROOM = BASE / "newsroom"
sys.path.insert(0, str(NEWSROOM))

import media  # noqa: E402
from worker import load_config  # noqa: E402

BV = re.compile(r"bilibili\.com/(?:video/)?(BV[a-zA-Z0-9]{10})")


def bili_picture(url):
    match = BV.search(url or "")
    if not match:
        return ""
    try:
        from bilibili_api import video
        info = asyncio.run(video.Video(bvid=match.group(1)).get_info())
        picture = info.get("pic") or ""
        if picture.startswith("https://") or picture.startswith("http://"):
            return picture
    except Exception:
        return ""
    return ""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    cfg = load_config(str(NEWSROOM / "config.json"))
    db = sqlite3.connect(NEWSROOM / "data" / "newsroom.sqlite3")
    db.row_factory = sqlite3.Row
    rows = list(db.execute("select id, payload from articles where status='published'"))
    print(f"articles: {len(rows)}", flush=True)

    changed = 0
    for row in rows:
        payload = json.loads(row["payload"])
        sources = payload.get("sources") or []
        picture = ""
        for source in sources:
            picture = bili_picture(source.get("url", ""))
            if picture:
                break
        if not picture:
            print(f"skip {row['id']}: 没有可用的 B站封面", flush=True)
            continue
        cover = media.source_cover(cfg, str(NEWSROOM / "data"), row["id"],
                                   [{"picture": picture}])
        if not cover:
            print(f"skip {row['id']}: 封面下载失败", flush=True)
            continue
        images = payload.get("images") or []
        for index, image in enumerate(images):
            if image.get("role") == "cover":
                images[index] = {**image, "caption": cover["caption"], "credit": cover["credit"]}
                break
        else:
            images.insert(0, cover)
        payload["images"] = images
        if not args.dry_run:
            db.execute("update articles set payload=? where id=?", (json.dumps(payload, ensure_ascii=False), row["id"]))
            db.commit()
        changed += 1
        print(f"done {row['id']}: {picture[:80]}", flush=True)

    db.close()
    print(f"changed: {changed}", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()

