"""检查 Live2D 看板娘：控制台是否还有 SDK 报错、canvas 是否真的画出来了。"""
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = "https://127.0.0.1/"
OUT = Path(__file__).with_name("shots")
OUT.mkdir(exist_ok=True)


def main() -> int:
    logs = []
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_context(ignore_https_errors=True, viewport={"width": 1440, "height": 900}).new_page()
        page.on("console", lambda m: logs.append(f"{m.type}: {m.text}"))
        page.on("pageerror", lambda e: logs.append(f"pageerror: {e}"))
        page.goto(BASE, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(6000)

        info = page.evaluate(
            """() => {
              const c = document.getElementById('live2d');
              let painted = false;
              if (c && c.width && c.height) {
                try {
                  const ctx = c.getContext('2d');
                  const data = ctx ? ctx.getImageData(0, 0, c.width, c.height).data : null;
                  if (data) {
                    for (let i = 3; i < data.length; i += 4) { if (data[i] > 0) { painted = true; break; } }
                  }
                } catch (e) { painted = 'read-failed'; }
              }
              return {
                canvas: !!c,
                size: c ? [c.width, c.height] : null,
                sdkLoaded: !!window.Live2dRender,
                painted,
              };
            }"""
        )
        page.screenshot(path=str(OUT / "verify-live2d.png"))
        info["console"] = [l for l in logs if "Live2D" in l or "live2d" in l or l.startswith("pageerror")][:8]
        info["consoleCount"] = len(logs)
        print(json.dumps(info, ensure_ascii=False, indent=2))
        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
