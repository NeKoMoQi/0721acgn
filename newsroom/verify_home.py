"""一次性验证脚本：打开线上首页，截首页/侧栏图，并把关键文案抓出来。用完可删。"""
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
        context = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900})
        page = context.new_page()
        errors = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.on("console", lambda m: errors.append(f"console.{m.type}: {m.text}") if m.type == "error" else None)

        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(2500)
        page.screenshot(path=str(OUT / "verify-home-top.png"))

        # 侧栏整体上卷，拍第二屏
        page.evaluate("window.scrollTo(0, document.querySelector('.behinds').offsetTop - 80)")
        page.wait_for_timeout(1200)
        page.screenshot(path=str(OUT / "verify-home-sidebar.png"))

        info = page.evaluate(
            """() => {
              const txt = (sel) => Array.from(document.querySelectorAll(sel)).map(e => e.textContent.trim());
              const cards = Array.from(document.querySelectorAll('.zixun .zi1')).slice(0, 6).map(e => ({
                title: e.querySelector('.headfont')?.textContent.trim() || '',
                meta: e.querySelector('.newsmeta')?.textContent.trim() || '',
                summary: (e.querySelector('.desfont')?.textContent.trim() || '').slice(0, 60),
              }));
              return {
                sidebarTitles: txt('.meiriuser .hottitle'),
                randomButton: document.querySelector('.random-entry')?.textContent.trim() || null,
                rankingRule: document.querySelector('.ranking-rule')?.textContent.trim() || null,
                rankingHeaders: txt('.shengdi .littletitle'),
                rankingRows: Array.from(document.querySelectorAll('.shengdi .hot-item')).slice(0, 5).map(e => e.textContent.replace(/\\s+/g, ' ').trim()),
                cards,
                navItems: txt('.heads .acgn'),
                hasUnknown: document.body.textContent.includes('未知功能'),
                hasRandomArticle: document.body.textContent.includes('随机文章'),
                newsMetaAll: txt('.zixun .newsmeta').slice(0, 6),
              };
            }"""
        )
        info["pageErrors"] = errors[:10]
        print(json.dumps(info, ensure_ascii=False, indent=2))
        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
