"""Read-only platform collectors; no posting, private messaging or stealth patches."""
import asyncio
import datetime as dt
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import time
from urllib.parse import quote, urlsplit


class CollectionError(RuntimeError):
    pass


def plain(value):
    class Text(HTMLParser):
        def __init__(self):
            super().__init__()
            self.parts = []
        def handle_data(self, data):
            self.parts.append(data)
    parser = Text()
    parser.feed(str(value or ""))
    return html.unescape(" ".join(parser.parts)).strip()


def number(value):
    if value is None or value == "":
        return None
    try:
        value = str(value).replace(",", "").strip()
        factor = 10000 if "万" in value else 100000000 if "亿" in value else 1
        return max(0, int(float(value.rstrip("万亿+")) * factor))
    except (TypeError, ValueError):
        return None


def date_string(value):
    if isinstance(value, (int, float)) or str(value or "").isdigit():
        try:
            timestamp = float(value)
            if timestamp > 10**12:
                timestamp /= 1000
            return dt.datetime.fromtimestamp(timestamp, dt.timezone.utc).isoformat()
        except (ValueError, OSError, OverflowError):
            return ""
    return str(value or "")


def field(mapping, *keys):
    for key in keys:
        if mapping.get(key) is not None:
            return mapping[key]
    return None


def safe_error(exc):
    code = getattr(exc, "code", None) or getattr(exc, "status", None)
    return type(exc).__name__ + ("(code=" + str(code) + ")" if isinstance(code, int) else "")


def bili_item(item, origin):
    bvid = item.get("bvid", "")
    if not re.fullmatch(r"BV[a-zA-Z0-9]{10}", bvid):
        return None
    stat = item.get("stat") or {}
    return {"platform": "bilibili", "platform_id": bvid,
        "url": "https://www.bilibili.com/video/" + bvid,
        "title": plain(item.get("title")), "author": (item.get("owner") or {}).get("name") or item.get("author", ""),
        "date": date_string(field(item, "pubdate", "created")),
        "summary": plain(field(item, "desc", "description"))[:2000], "content": "",
        "metrics": {"views": number(field(stat, "view") if "view" in stat else item.get("play")),
                    "comments": number(field(stat, "reply") if "reply" in stat else item.get("review")),
                    "likes": number(stat.get("like"))},
        "origin": origin, "category": item.get("tname", ""), "warnings": []}


class BiliCollector:
    def __init__(self, cfg):
        self.cfg = cfg
        self.loop = asyncio.new_event_loop()

    def run(self, coroutine):
        return self.loop.run_until_complete(coroutine)

    def credential(self):
        from bilibili_api import Credential
        path = Path(self.cfg["bili_credential_file"])
        if not path.exists():
            return Credential()
        obj = json.loads(path.read_text(encoding="utf-8-sig"))
        return Credential(**{k: str(obj.get(k) or "") for k in
                             ("sessdata", "bili_jct", "buvid3", "dedeuserid", "ac_time_value")})

    async def _collect(self, topics):
        from bilibili_api import hot, rank, search, homepage
        items, errors = {}, []
        current_date = dt.datetime.now(dt.timezone(dt.timedelta(hours=8))).date()
        jobs = [("popular", lambda: hot.get_hot_videos(pn=1, ps=self.cfg["platform_page_size"]))]
        cred = self.credential()
        if cred.sessdata:
            jobs.append(("homepage", lambda: homepage.get_videos(credential=cred)))
        for name in self.cfg["bili_rank_categories"]:
            enum = {"douga": rank.RankType.Douga, "game": rank.RankType.Game}.get(name)
            if enum is not None:
                jobs.append(("rank:" + name, lambda enum=enum: rank.get_rank(type_=enum, day=rank.RankDayType.THREE_DAY)))
        for topic in topics:
            for order in (search.OrderVideo.PUBDATE, search.OrderVideo.CLICK):
                jobs.append(("search:" + topic, lambda topic=topic, order=order: search.search_by_type(
                    keyword=topic, search_type=search.SearchObjectType.VIDEO, page=1, order_type=order,
                    page_size=self.cfg["platform_page_size"],
                    time_start=(current_date - dt.timedelta(days=self.cfg["max_article_age_days"])).isoformat(),
                    time_end=current_date.isoformat())))
        for origin, request in jobs:
            try:
                response = await asyncio.wait_for(request(), self.cfg["platform_timeout_seconds"])
                rows = response.get("list") or response.get("result") or response.get("item") or []
                for raw in rows[:self.cfg["platform_page_size"]]:
                    item = bili_item(raw, origin)
                    if item:
                        # Global popular must match configured topics or ACGN categories.
                        text = item["title"] + " " + item["summary"] + " " + item["category"]
                        relevant = any(word in text for word in self.cfg["interest_keywords"])
                        if origin.startswith(("search:", "rank:")) or relevant:
                            items[item["url"]] = item
            except Exception as exc:
                errors.append({"operation": origin, "error": safe_error(exc)})
            await asyncio.sleep(self.cfg["platform_request_delay_seconds"])
        return list(items.values()), errors

    def collect(self, topics):
        try:
            return self.run(self._collect(topics))
        except ImportError as exc:
            raise CollectionError("install newsroom/requirements.txt for Bilibili") from exc

    async def _enrich(self, item):
        import aiohttp
        from bilibili_api import video, comment
        cred = self.credential()
        target = video.Video(bvid=item["platform_id"], credential=cred)
        info = await asyncio.wait_for(target.get_info(), self.cfg["platform_timeout_seconds"])
        details = bili_item(info, item.get("origin", "detail"))
        details["warnings"] = []
        cid = info.get("cid") or (info.get("pages") or [{}])[0].get("cid")
        details["subtitle_state"] = "unavailable"
        if cid:
            try:
                response = await asyncio.wait_for(target.get_subtitle(cid=cid), self.cfg["platform_timeout_seconds"])
                tracks = response.get("subtitles") or []
                tracks.sort(key=lambda s: s.get("lan") not in ("zh-CN", "zh", "ai-zh"))
                if tracks:
                    url = tracks[0].get("subtitle_url", "")
                    url = "https:" + url if url.startswith("//") else url
                    host = urlsplit(url).hostname or ""
                    if not (url.startswith("https://") and (host.endswith(".hdslb.com") or host.endswith(".bilibili.com"))):
                        raise ValueError("unexpected subtitle host")
                    async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.cfg["platform_timeout_seconds"])) as session:
                        async with session.get(url) as response:
                            response.raise_for_status()
                            data = await response.content.read(2_000_001)
                            if len(data) > 2_000_000:
                                raise ValueError("subtitle too large")
                            segments = json.loads(data).get("body") or []
                    details["content"] = "\n".join(str(s.get("content", "")) for s in segments)[:30000]
                    details["subtitle_state"] = "read"
                else:
                    details["subtitle_state"] = "no_subtitle"
            except Exception as exc:
                details["warnings"].append("subtitle:" + type(exc).__name__)
        try:
            data = await asyncio.wait_for(comment.get_comments(oid=info["aid"],
                type_=comment.CommentResourceType.VIDEO, page_index=1,
                order=comment.OrderType.LIKE, credential=cred), self.cfg["platform_timeout_seconds"])
            details["comments"] = [{"text": str((r.get("content") or {}).get("message", ""))[:800],
                                    "likes": number(r.get("like"))} for r in
                                   (data.get("replies") or [])[:self.cfg["max_comments"]]]
            details["comments_state"] = "read"
        except Exception as exc:
            details["comments_state"] = "unavailable"
            details["warnings"].append("comments:" + type(exc).__name__)
        # Description is factual video metadata, never pretend it is a transcript.
        if not details["content"]:
            details["content"] = details["summary"]
            details["content_type"] = "description_only"
        else:
            details["content_type"] = "subtitle"
        return details

    def enrich(self, item):
        return self.run(self._enrich(item))

    async def _login(self, destination):
        from bilibili_api.login_v2 import QrCodeLogin, QrCodeLoginEvents
        login = QrCodeLogin()
        await login.generate_qrcode()
        qr = Path(destination).with_name("bili-login.png")
        qr.write_bytes(login.get_qrcode_picture().content)
        print("Scan with Bilibili app: " + str(qr), flush=True)
        print(login.get_qrcode_terminal(), flush=True)
        for _ in range(90):
            state = await login.check_state()
            if state == QrCodeLoginEvents.DONE:
                cred = login.get_credential()
                obj = {k: getattr(cred, k, "") or "" for k in
                       ("sessdata", "bili_jct", "buvid3", "dedeuserid", "ac_time_value")}
                path = Path(destination)
                temp = path.with_suffix(".tmp")
                temp.write_text(json.dumps(obj), encoding="utf-8")
                temp.replace(path)
                qr.unlink(missing_ok=True)
                print("Bilibili session saved locally.")
                return
            if state == QrCodeLoginEvents.TIMEOUT:
                break
            await asyncio.sleep(2)
        qr.unlink(missing_ok=True)
        raise CollectionError("Bilibili login expired; run login again")

    def login(self):
        Path(self.cfg["bili_credential_file"]).parent.mkdir(parents=True, exist_ok=True)
        self.run(self._login(self.cfg["bili_credential_file"]))

    def close(self):
        # bilibili-api stores HTTP sessions per event loop; close before the loop.
        async def cleanup():
            from bilibili_api import get_client
            await get_client().close()
        try:
            self.run(cleanup())
        except Exception:
            pass
        self.loop.close()


def xhh_items(payload):
    """Only recognized post objects are retained; never persist full browser responses."""
    found = {}
    def walk(node):
        if isinstance(node, dict):
            ident = str(node.get("link_id") or node.get("linkid") or "")
            title = plain(node.get("title"))
            if ident and re.fullmatch(r"[A-Za-z0-9_-]{1,80}", ident) and title:
                stats = node.get("stat") or node
                full_text = plain(field(node, "text", "content"))
                content = full_text or plain(node.get("description"))
                found[ident] = {"platform": "xiaoheihe", "platform_id": ident,
                    "url": "https://www.xiaoheihe.cn/app/bbs/link/" + ident,
                    "title": title, "summary": content[:1500], "content": content[:30000],
                    "date": date_string(field(node, "create_at", "create_time", "timestamp")),
                    "content_type": "post_full" if full_text else "feed_excerpt", "origin": "site_feed", "warnings": [],
                    "metrics": {"views": number(field(stats, "click", "click_count", "view_num", "view_count")),
                                "comments": number(field(stats, "comment_num", "comment_count", "reply_num")),
                                "likes": number(field(stats, "support_num", "like_num", "support_count"))}}
            for key, value in node.items():
                if key not in ("user", "user_info", "author", "account", "token", "cookies"):
                    walk(value)
        elif isinstance(node, list):
            for entry in node:
                walk(entry)
    walk(payload)
    return list(found.values())


def xhh_comments(payload, limit):
    rows = []
    def walk(node):
        if len(rows) >= limit:
            return
        if isinstance(node, dict):
            if ("comment_id" in node or "commentid" in node) and isinstance(node.get("text"), str):
                rows.append({"text": plain(node["text"])[:800], "likes": number(node.get("support_num"))})
            for key, child in node.items():
                if key not in ("user", "user_info", "author"):
                    walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)
    walk(payload)
    return rows


class XhhCollector:
    def __init__(self, cfg):
        self.cfg = cfg

    def _state_file(self):
        configured = self.cfg.get("xhh_storage_file") or ""
        if configured:
            return Path(configured)
        return Path(self.cfg["xhh_profile_dir"]).parent / "xhh-storage.json"

    def save_state(self, browser):
        """把 cookie（含会话 cookie）另存一份，浏览器关闭后下次还能恢复，避免一关就掉登录。"""
        try:
            path = self._state_file()
            path.parent.mkdir(parents=True, exist_ok=True)
            browser.storage_state(path=str(path))
            return True
        except Exception:
            return False

    def open(self, headless=True):
        from playwright.sync_api import sync_playwright
        driver = sync_playwright().start()
        try:
            browser = driver.chromium.launch_persistent_context(self.cfg["xhh_profile_dir"],
                headless=headless, locale="zh-CN", viewport={"width": 1360, "height": 900})
            stored = self._state_file()
            if stored.is_file():
                try:
                    cookies = json.loads(stored.read_text(encoding="utf-8")).get("cookies") or []
                    if cookies:
                        browser.add_cookies(cookies)
                except Exception:
                    pass
            return driver, browser
        except Exception:
            driver.stop()
            raise

    def _visit(self, page, url, posts, comments, errors):
        def receive(response):
            parsed = urlsplit(response.url)
            if not ((parsed.hostname or "").endswith(".xiaoheihe.cn") and
                    any(p in parsed.path for p in ("/bbs/app/feeds", "/bbs/app/link/", "/bbs/web/", "/bbs/app/search"))):
                return
            if response.status in (401, 403, 412, 429):
                errors.append({"operation": "site_response", "error": "HTTP_" + str(response.status)})
                return
            try:
                if "json" not in response.headers.get("content-type", ""):
                    return
                data = response.json()
                if "status" in data and data["status"] != "ok":
                    errors.append({"operation": "site_response", "error": "platform_rejected"})
                for item in xhh_items(data):
                    previous = posts.get(item["url"])
                    if not previous or len(item["content"]) >= len(previous["content"]):
                        posts[item["url"]] = item
                comments.extend(xhh_comments(data, self.cfg["max_comments"]))
            except Exception:
                errors.append({"operation": "site_response", "error": "unrecognized_response"})
        page.on("response", receive)
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=self.cfg["platform_timeout_seconds"] * 1000)
            for _ in range(self.cfg["xhh_scroll_pages"]):
                page.wait_for_timeout(self.cfg["browser_settle_ms"])
                if page.get_by_text(re.compile("安全验证|访问过于频繁|请完成验证"), exact=False).count():
                    raise CollectionError("xiaoheihe requires user verification; no bypass attempted")
                page.mouse.wheel(0, 900)
            page.wait_for_timeout(self.cfg["browser_settle_ms"])
            # DOM fallback still reads actual rendered post text, not search snippets.
            links = page.locator('a[href*="/app/bbs/link/"], a[href*="/app/topic/link/"]').evaluate_all(
                "els => els.slice(0,100).map(a => ({url:a.href,title:(a.querySelector('.bbs-content__title')?.innerText || a.innerText || '').trim()}))")
            for link in links:
                parsed = urlsplit(link["url"])
                match = re.search(r"/app/(?:bbs|topic)/link/([A-Za-z0-9_-]+)", parsed.path)
                if parsed.hostname == "www.xiaoheihe.cn" and match and link["title"]:
                    canonical = "https://www.xiaoheihe.cn/app/bbs/link/" + match[1]
                    posts.setdefault(canonical, {"platform": "xiaoheihe", "platform_id": match[1],
                        "url": canonical, "title": link["title"][:200], "content": "", "summary": "",
                        "date": "", "metrics": {}, "origin": "rendered_feed", "warnings": []})
        finally:
            page.remove_listener("response", receive)

    def collect(self, topics):
        posts, errors = {}, []
        try:
            driver, browser = self.open()
        except ImportError as exc:
            raise CollectionError("install requirements and playwright chromium for Xiaoheihe") from exc
        try:
            page = browser.new_page()
            urls = ["https://www.xiaoheihe.cn/app/bbs"] + self.cfg["xhh_feed_urls"]
            for url in list(dict.fromkeys(urls)):
                try:
                    self._visit(page, url, posts, [], errors)
                except Exception as exc:
                    errors.append({"operation": "feed", "error": str(exc) if isinstance(exc, CollectionError) else type(exc).__name__})
            if not posts:
                errors.append({"operation": "feed", "error": "no_posts; login, site changes or network failure"})
            return list(posts.values()), errors
        finally:
            self.save_state(browser)
            browser.close()
            driver.stop()

    def enrich(self, item):
        posts, comments, errors = {}, [], []
        driver, browser = self.open()
        try:
            page = browser.new_page()
            self._visit(page, item["url"], posts, comments, errors)
            result = {**item, **posts.get(item["url"], {})}
            # Read only the article content area. Never use full body (nav/account menus).
            content = page.locator('.bbs-content__content, .article-content, .link-content')
            if content.count():
                text = content.first.inner_text()[:30000]
                if len(text) > len(result.get("content", "")):
                    result["content"] = text
                    result["content_type"] = "post_full"
            title = page.locator('.bbs-content__title, h1')
            if title.count():
                result["title"] = title.first.inner_text()[:200]
            result["comments"] = comments[:self.cfg["max_comments"]]
            result["comments_state"] = "read" if comments else "unavailable_or_empty"
            result["warnings"] = result.get("warnings", []) + [e["error"] for e in errors]
            if result.get("content_type") != "post_full":
                raise CollectionError("xiaoheihe full post unavailable; login via platform_cli.py, then probe again")
            return result
        finally:
            self.save_state(browser)
            browser.close()
            driver.stop()

    def login(self):
        driver, browser = self.open(headless=False)
        try:
            page = browser.new_page()
            page.goto("https://www.xiaoheihe.cn/app/bbs")
            print("Complete login yourself in the opened browser. No password is read by this program.", flush=True)
            input("Press Enter after completing login (Ctrl+C cancels): ")
            print("Browser session saved locally; use probe to verify actual feed access.")
        finally:
            self.save_state(browser)
            browser.close()
            driver.stop()

    def close(self):
        pass
