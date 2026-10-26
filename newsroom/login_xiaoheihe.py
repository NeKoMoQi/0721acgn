"""小黑盒登录助手：打开浏览器让你手动登录，检测到登录态后保存 cookie（含会话 cookie）。

用法：.venv\\Scripts\\python.exe login_xiaoheihe.py [--headless]
登录窗口会在当前 Windows 会话里弹出；登录完成后不用按回车，脚本自己检测。
"""
import argparse
import sys
import time

from platforms import XhhCollector
from worker import load_config

LOGIN_COOKIES = ("user_heybox_id", "heybox_id", "user_heybox_token", "heybox_token", "x_xhh_token")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="config.json")
    parser.add_argument("--timeout", type=int, default=900)
    args = parser.parse_args()

    cfg = load_config(args.config)
    collector = XhhCollector(cfg)
    driver, browser = collector.open(headless=False)
    try:
        page = browser.pages[0] if browser.pages else browser.new_page()
        page.goto("https://www.xiaoheihe.cn/app/bbs", wait_until="domcontentloaded", timeout=60000)
        print("browser opened, waiting for manual login", flush=True)
        deadline = time.monotonic() + args.timeout
        while time.monotonic() < deadline:
            for cookie in browser.cookies():
                if cookie["name"].lower() in LOGIN_COOKIES and str(cookie.get("value", "")).strip():
                    print("login detected:", cookie["name"], flush=True)
                    time.sleep(4)
                    saved = collector.save_state(browser)
                    print("state saved:", saved, "->", collector._state_file(), flush=True)
                    return 0
            time.sleep(2)
        print("TIMEOUT: 没有检测到登录态", flush=True)
        return 2
    finally:
        try:
            collector.save_state(browser)
        except Exception:
            pass
        browser.close()
        driver.stop()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
