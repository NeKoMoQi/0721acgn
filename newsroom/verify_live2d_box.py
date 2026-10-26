"""对比隐藏 #live2d 画布前后的同一区域，确认右下角角色是 Live2D 画出来的还是静态图。"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageChops
from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1/"
OUT = Path(__file__).with_name("shots")
OUT.mkdir(exist_ok=True)
BOX = (1150, 560, 1440, 900)  # 右下角区域


def main() -> int:
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900}).new_page()
        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(6000)
        page.screenshot(path=str(OUT / "live2d-on.png"))
        rects = page.evaluate(
            """() => {
              const r = (sel) => { const e = document.querySelector(sel); if (!e) return null;
                const b = e.getBoundingClientRect(); return [Math.round(b.x), Math.round(b.y), Math.round(b.width), Math.round(b.height)]; };
              return { canvas: r('#live2d'), tsg: r('.tsg'), toolbox: r('#live2d-toolbox') };
            }"""
        )
        page.evaluate("() => { const c = document.getElementById('live2d'); if (c) c.style.visibility = 'hidden'; }")
        page.wait_for_timeout(500)
        page.screenshot(path=str(OUT / "live2d-off.png"))
        browser.close()

    on = Image.open(OUT / "live2d-on.png").convert("RGB").crop(BOX)
    off = Image.open(OUT / "live2d-off.png").convert("RGB").crop(BOX)
    diff = ImageChops.difference(on, off)
    changed = sum(1 for px in diff.getdata() if px != (0, 0, 0))
    total = on.size[0] * on.size[1]
    print(json.dumps({**rects, "changedPixels": changed, "regionPixels": total,
                      "changedRatio": round(changed / total, 3)}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
