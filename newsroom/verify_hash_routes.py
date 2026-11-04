"""用 hash 路由的真实地址开三个页面，确认各自渲染正确、无报错。"""
import json
import sys

from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1"
CASES = {
    "shengdi": "/#/shengdi",
    "galgame": "/#/galagame",
    "news": "/#/news",
}


def main() -> int:
    out = {}
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for name, path in CASES.items():
            page = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900}).new_page()
            errors = []
            page.on("console", lambda m: errors.append(f"{m.type}: {m.text[:140]}") if m.type == "error" else None)
            page.on("pageerror", lambda e: errors.append(f"pageerror: {str(e)[:140]}"))
            page.goto(BASE + path, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(5000)
            out[name] = {
                "url": page.url,
                "hero": page.locator(".mainfont1").count(),
                "bodyHead": page.inner_text("body")[:100].replace("\n", " "),
                "errors": errors[:5],
            }
            page.screenshot(path=f"shots/smoke-hash-{name}.png")
            page.context.close()
        browser.close()
    print(json.dumps(out, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
