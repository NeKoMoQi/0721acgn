"""补拍侧栏下半段：公告、分类、圣地巡礼榜。"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1/"
OUT = Path(__file__).with_name("shots")
OUT.mkdir(exist_ok=True)


def main() -> int:
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900}).new_page()
        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(2000)

        page.evaluate(
            """() => {
              const block = Array.from(document.querySelectorAll('.meiriuser .character'))
                .find(e => e.textContent.includes('小窝公告'));
              if (block) block.scrollIntoView({block: 'center'});
            }"""
        )
        page.wait_for_timeout(900)
        page.screenshot(path=str(OUT / "verify-sidebar-notice.png"))

        page.evaluate("() => document.querySelector('.shengdi').scrollIntoView({block: 'center'})")
        page.wait_for_timeout(900)
        page.screenshot(path=str(OUT / "verify-sidebar-shengdi.png"))

        heights = page.evaluate(
            """() => ({
              behinds: Math.round(document.querySelector('.behinds').getBoundingClientRect().height),
              sidebar: Math.round(document.querySelector('.meiriuser').getBoundingClientRect().height),
              zixun: Math.round(document.querySelector('.zixun').getBoundingClientRect().height),
              overflowX: document.documentElement.scrollWidth > document.documentElement.clientWidth,
            })"""
        )
        print(heights)
        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
