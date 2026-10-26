"""看 Live2D 初始化过程中请求了哪些资源、哪些失败。"""
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1/"
OUT = Path(__file__).with_name("shots")


def main() -> int:
    reqs, fails, errs = [], [], []
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900}).new_page()
        page.on("response", lambda r: reqs.append((r.status, r.url)) if ("models/" in r.url or "live2d" in r.url or "cubism" in r.url) else None)
        page.on("requestfailed", lambda r: fails.append((r.failure or "?", r.url)))
        page.on("console", lambda m: errs.append(f"{m.type}: {m.text[:200]}"))
        page.on("pageerror", lambda e: errs.append(f"pageerror: {str(e)[:200]}"))
        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(8000)
        browser.close()
    print(json.dumps({
        "responses": reqs[-25:],
        "failed": fails[-10:],
        "console": errs[-12:],
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
