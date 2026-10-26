"""用新提示词重写已发布文章（只重写正文，不动 ID、发布时间和封面）。

用法：python rewrite_articles.py [--dry-run] [--limit N]
每篇 1-3 次模型调用；调用次数不足时自动停。
"""
import argparse
import json
import sys

sys.path.insert(0, ".")
sys.stdout.reconfigure(encoding="utf-8")
from worker import Newsroom, acgn_score, load_config, stamp  # noqa: E402

FIELDS = ("title", "summary", "paragraphs", "kind", "needs_review", "review_reason", "event_key", "event_date")


def build_source(payload):
    source = {
        "id": 1,
        "url": payload.get("url", ""),
        "title": (payload.get("title") or "")[:200],
        "date": payload.get("date") or "",
        "content": (payload.get("content") or payload.get("summary") or "")[:8000],
        "summary": (payload.get("summary") or "")[:1500],
        "is_official": False,
        "metrics": payload.get("metrics") or {},
    }
    if payload.get("comments"):
        source["comments"] = payload["comments"][:12]
    if payload.get("picture"):
        source["picture"] = payload["picture"]
    return source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--min-calls-left", type=int, default=8)
    parser.add_argument("--max-age", type=int, default=365, help="重写时放宽事件日期的新鲜度")
    parser.add_argument("--force", action="store_true", help="连已经重写过的也再来一遍")
    args = parser.parse_args()

    cfg = load_config("config.json")
    cfg["max_article_age_days"] = args.max_age
    room = Newsroom(cfg)
    rewritten = skipped = failed = 0
    try:
        rows = list(room.db.execute("select * from articles where status='published' order by published_at"))
        if args.limit:
            rows = rows[:args.limit]
        print(f"待重写: {len(rows)}", flush=True)
        for index, row in enumerate(rows, 1):
            used = room.db.execute("select calls from budget where day=?", (__import__("worker").today(),)).fetchone()
            used = used[0] if used else 0
            if cfg["max_api_calls_per_day"] - used < args.min_calls_left:
                print("预算不足，停止", flush=True)
                break
            candidate = room.db.execute("select * from candidates where id=?", (row["candidate_id"],)).fetchone()
            if candidate is None:
                skipped += 1
                print(f"[{index}] 跳过（没有原始候选）", flush=True)
                continue
            payload = json.loads(candidate["payload"])
            if len((payload.get("content") or "")) < 100:
                skipped += 1
                print(f"[{index}] 跳过（素材太少）", flush=True)
                continue
            source = build_source(payload)
            # 新规则要求解释背景：先自己补一次背景来源，再交给模型
            try:
                extra = room.background_sources(payload, {source["url"]})
            except Exception as exc:  # noqa: BLE001
                print(f"[{index}] 背景检索失败 {type(exc).__name__}", flush=True)
                extra = []
            sources_for_write = [source] + extra
            article = None
            last_error = None
            for attempt in (1, 2):
                try:
                    draft = room.api.write(sources_for_write)
                    article = room.validate(draft, sources_for_write)
                    break
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[{index}] 第{attempt}次失败 {type(exc).__name__}: {str(exc)[:80]}", flush=True)
            if article is None and last_error is not None:
                failed += 1
                continue
            if article is None:
                skipped += 1
                print(f"[{index}] 模型判断素材不足，跳过", flush=True)
                continue
            old = json.loads(row["payload"])
            if old.get("rewritten_at") and not args.force:
                skipped += 1
                print(f"[{index}] 已重写过，跳过", flush=True)
                continue
            merged = {**old, **{key: article[key] for key in FIELDS if key in article}}
            score = acgn_score(f"{article['title']} {article['summary']}", cfg, room.catalog)
            merged["acgn_score"] = score
            merged["featured"] = score >= cfg.get("featured_min_score", 2)
            merged["rewritten_at"] = stamp()
            if args.dry_run:
                print(f"[{index}] DRY {article['title'][:40]} ({len(''.join(article['paragraphs']))} 字)", flush=True)
                rewritten += 1
                continue
            try:
                room.db.execute("update articles set title=?, payload=? where id=?",
                                (article["title"], json.dumps(merged, ensure_ascii=False), row["id"]))
                room.db.commit()
            except Exception as exc:  # noqa: BLE001
                room.db.rollback()
                failed += 1
                print(f"[{index}] 写库失败 {type(exc).__name__}: {str(exc)[:80]}", flush=True)
                continue
            rewritten += 1
            print(f"[{index}] 改写 {article['title'][:38]} | {len(''.join(article['paragraphs']))} 字", flush=True)
        if not args.dry_run:
            room.export()
            print("快照已重新导出", flush=True)
        print(f"完成：改写 {rewritten}，跳过 {skipped}，失败 {failed}", flush=True)
    finally:
        room.close()


if __name__ == "__main__":
    main()




