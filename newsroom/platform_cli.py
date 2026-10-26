"""Login and read-only live diagnostics, no DeepSeek API calls."""
import argparse
import json
from pathlib import Path
import sys
from worker import Newsroom, exclusive_lock, load_config
from platforms import safe_error, CollectionError


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("login", "probe"))
    parser.add_argument("platform", choices=("bilibili", "xiaoheihe"))
    parser.add_argument("--config", default=str(Path(__file__).with_name("config.json")))
    parser.add_argument("--detail", action="store_true", help="also read the highest-ranked returned item")
    args = parser.parse_args()
    cfg = load_config(args.config)
    room = Newsroom(cfg)
    try:
        with exclusive_lock(room.root):
            target = room.collector(args.platform)
            if args.command == "login":
                target.login()
                return
            items, errors = target.collect(cfg["topics"])
            report = {"platform": args.platform, "items": len(items), "errors": errors,
                      "examples": [{k: item.get(k) for k in ("url", "title", "date", "metrics")} for item in items[:3]]}
            if args.detail and items:
                try:
                    item = target.enrich(items[0])
                    report["detail"] = {"url": item["url"], "content_characters": len(item.get("content", "")),
                        "content_type": item.get("content_type"), "subtitle_state": item.get("subtitle_state"),
                        "comments": len(item.get("comments", [])), "warnings": item.get("warnings", [])}
                except Exception as exc:
                    report["detail"] = {"error": str(exc) if isinstance(exc, CollectionError) else safe_error(exc)}
            print(json.dumps(report, ensure_ascii=False, indent=2))
            return 2 if "error" in report.get("detail", {}) else 0 if items else 1
    finally:
        room.close()


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        # Exception messages from external clients may contain request URLs/tokens.
        print("Platform operation failed: " + type(exc).__name__ + ". Check login/network/dependencies.", file=sys.stderr)
        sys.exit(1)
