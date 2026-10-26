"""处理还没按新规则重写的旧稿：能重写就重写，重写不了就隐藏。"""
import json
import sys

sys.path.insert(0, ".")
sys.stdout.reconfigure(encoding="utf-8")
from rewrite_articles import build_source  # noqa: E402
from worker import Newsroom, acgn_score, load_config, stamp  # noqa: E402

FIELDS = ("title", "summary", "paragraphs", "kind", "needs_review", "review_reason", "event_key", "event_date")

cfg = load_config("config.json")
cfg["max_article_age_days"] = 365
room = Newsroom(cfg)
rewritten = hidden = native = 0
try:
    rows = list(room.db.execute("select * from articles where status='published' order by published_at"))
    for row in rows:
        payload = json.loads(row["payload"])
        if payload.get("rewritten_at"):
            continue
        # 10-26 之后发布的本来就是新规则写的，只打个标记
        if (row["published_at"] or "") >= "2026-10-26":
            payload["rewritten_at"] = "v3-native"
            room.db.execute("update articles set payload=? where id=?", (json.dumps(payload, ensure_ascii=False), row["id"]))
            room.db.commit()
            native += 1
            continue

        candidate = room.db.execute("select * from candidates where id=?", (row["candidate_id"],)).fetchone()
        if candidate is None:
            room.db.execute("update articles set status='hidden' where id=?", (row["id"],))
            room.db.commit()
            hidden += 1
            print(f"隐藏（无候选）: {payload.get('title')[:40]}", flush=True)
            continue

        raw = json.loads(candidate["payload"])
        try:
            if raw.get("platform") in ("bilibili", "xiaoheihe"):
                raw = {**raw, **room.collector(raw["platform"]).enrich(raw)}
                room.db.execute("update candidates set payload=? where id=?",
                                (json.dumps(raw, ensure_ascii=False), candidate["id"]))
                room.db.commit()
        except Exception as exc:  # noqa: BLE001
            print(f"补充素材失败 {type(exc).__name__}: {raw.get('title','')[:30]}", flush=True)

        source = build_source(raw)
        if len(source["content"]) < 80:
            room.db.execute("update articles set status='hidden' where id=?", (row["id"],))
            room.db.commit()
            hidden += 1
            print(f"隐藏（素材太少 {len(source['content'])} 字）: {payload.get('title')[:38]}", flush=True)
            continue

        extra = room.background_sources(raw, {source["url"]})
        sources = [source] + extra
        article = None
        for attempt in (1, 2):
            try:
                article = room.validate(room.api.write(sources), sources)
                break
            except Exception as exc:  # noqa: BLE001
                print(f"  第{attempt}次失败 {type(exc).__name__}: {str(exc)[:60]}", flush=True)
        if article is None:
            room.db.execute("update articles set status='hidden' where id=?", (row["id"],))
            room.db.commit()
            hidden += 1
            print(f"隐藏（模型判定写不出）: {payload.get('title')[:38]}", flush=True)
            continue

        merged = {**payload, **{key: article[key] for key in FIELDS if key in article}}
        score = acgn_score(f"{article['title']} {article['summary']}", cfg, room.catalog)
        merged["acgn_score"] = score
        merged["featured"] = score >= cfg.get("featured_min_score", 2)
        merged["rewritten_at"] = stamp()
        room.db.execute("update articles set title=?, payload=? where id=?",
                        (article["title"], json.dumps(merged, ensure_ascii=False), row["id"]))
        room.db.commit()
        rewritten += 1
        print(f"重写 {len(''.join(article['paragraphs']))} 字 | {article['title'][:40]}", flush=True)

    room.export()
    print(f"完成：重写 {rewritten}，隐藏 {hidden}，新规则原生 {native}", flush=True)
finally:
    room.close()
