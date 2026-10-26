"""手机宽度（390×844）下检查首页两栏是否正常堆叠。"""
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1/"
OUT = Path(__file__).with_name("shots")
OUT.mkdir(exist_ok=True)


def main() -> int:
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_context(
            ignore_https_errors=True, viewport={"width": 390, "height": 844}, is_mobile=True, has_touch=True
        ).new_page()
        errors = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(2000)
        page.evaluate("() => document.querySelector('.behinds').scrollIntoView({block: 'start'})")
        page.wait_for_timeout(900)
        page.screenshot(path=str(OUT / "verify-mobile-feed.png"))
        page.evaluate("() => document.querySelector('.shengdi').scrollIntoView({block: 'center'})")
        page.wait_for_timeout(900)
        page.screenshot(path=str(OUT / "verify-mobile-sidebar.png"))
        info = page.evaluate(
            """() => ({
              zixunHeight: Math.round(document.querySelector('.zixun').getBoundingClientRect().height),
              zixunOverflow: getComputedStyle(document.querySelector('.zixun')).overflowY,
              sidebarWidth: Math.round(document.querySelector('.meiriuser').getBoundingClientRect().width),
              viewport: window.innerWidth,
              overflowX: document.documentElement.scrollWidth > document.documentElement.clientWidth,
              scrollWidth: document.documentElement.scrollWidth,
            })"""
        )
        info["pageErrors"] = errors[:5]
        print(json.dumps(info, ensure_ascii=False))
        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
