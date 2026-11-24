"""把今天的文章按新规则（900-1200 字 + 小标题）重写；无限暖暖那篇用已生成的专稿。"""
import json
import sqlite3
import sys

sys.path.insert(0, ".")
sys.stdout.reconfigure(encoding="utf-8")
from rewrite_articles import build_source  # noqa: E402
from worker import Newsroom, acgn_score, load_config, stamp  # noqa: E402

FIELDS = ("title", "summary", "paragraphs", "kind", "needs_review", "review_reason", "event_key", "event_date")
TODAY = "2026-11-23"
INFINITY_ID = "8b0fd6601cfad3d0f670c8c6"

cfg = load_config("config.json")
cfg["max_article_age_days"] = 365

# 无限暖暖：把已有专稿转成带小标题的段落
draft = open("data/infinity_draft.txt", encoding="utf-8").read().strip().splitlines()
title = draft[0].lstrip("# ").strip()
body, current = [], []
for line in draft[1:]:
    line = line.strip()
    if not line:
        continue
    if line.startswith("## "):
        body.append("## " + line[3:].strip())
    else:
        body.append(line)

db = sqlite3.connect("data/newsroom.sqlite3")
db.row_factory = sqlite3.Row
db.execute("update budget set calls=0, drafts=0 where day=?", (TODAY,))
db.commit()
print("额度重置:", db.execute("select day,calls,drafts from budget where day=?", (TODAY,)).fetchone())

rows = list(db.execute("select * from articles where status='published' and published_at like ?", (TODAY + "%",)))
print("今日文章:", len(rows))

for row in rows:
    payload = json.loads(row["payload"])
    if row["id"] == INFINITY_ID:
        payload.update({"title": title, "paragraphs": body, "rewritten_at": stamp() + "-v4"})
        db.execute("update articles set title=?, payload=? where id=?",
                   (title, json.dumps(payload, ensure_ascii=False), row["id"]))
        db.commit()
        print("无限暖暖：用专稿 ->", len("".join(body)), "字")
        continue

    candidate = db.execute("select * from candidates where id=?", (row["candidate_id"],)).fetchone()
    if candidate is None:
        print("跳过（无候选）:", payload.get("title")[:36])
        continue
    raw = json.loads(candidate["payload"])
    source = build_source(raw)
    if len(source["content"]) < 80:
        print("跳过（素材太少）:", payload.get("title")[:36])
        continue

    room = Newsroom(cfg)
    try:
        extra = room.background_sources(raw, {source["url"]})
        sources = [source] + extra
        article = None
        for attempt in (1, 2):
            try:
                article = room.validate(room.api.write(sources), sources)
                break
            except Exception as exc:  # noqa: BLE001
                print(f"  第{attempt}次失败 {type(exc).__name__}: {str(exc)[:60]}")
        if article is None:
            print("跳过（写不出）:", payload.get("title")[:36])
            continue
        merged = {**payload, **{k: article[k] for k in FIELDS if k in article}}
        score = acgn_score(f"{article['title']} {article['summary']}", cfg, room.catalog)
        merged["acgn_score"] = score
        merged["featured"] = score >= cfg.get("featured_min_score", 2)
        merged["rewritten_at"] = stamp() + "-v4"
        db.execute("update articles set title=?, payload=? where id=?",
                   (article["title"], json.dumps(merged, ensure_ascii=False), row["id"]))
        db.commit()
        print(f"重写 {len(''.join(article['paragraphs']))} 字 | {article['title'][:36]}")
    finally:
        room.close()

db.close()
print("done")
