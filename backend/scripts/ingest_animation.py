"""Configured source pages/RSS -> local qBittorrent -> MP4 -> OSS -> episodes.

Run without --apply to inspect matches without downloading or writing remote data.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
from html import unescape
from html.parser import HTMLParser
import json
import logging
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid
from urllib.parse import parse_qs, quote, urljoin, urlparse
import xml.etree.ElementTree as ET

import requests

LOG = logging.getLogger("animation-ingest")
VIDEO_EXTENSIONS = {".mp4", ".mkv", ".webm", ".mov", ".avi", ".m4v"}


class Anchors(HTMLParser):
    def __init__(self):
        super().__init__()
        self.items = []
        self.anchor = None
        self.text = []

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self.anchor = dict(attrs)
            self.text = []

    def handle_data(self, data):
        if self.anchor is not None:
            self.text.append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self.anchor is not None:
            if self.anchor.get("href"):
                self.items.append((self.anchor.get("title") or " ".join(self.text).strip(), self.anchor["href"]))
            self.anchor = None


def fetch(session, url):
    if urlparse(url).scheme not in {"http", "https"}:
        raise ValueError("Source pages must use HTTP(S)")
    response = session.get(url, timeout=30)
    response.raise_for_status()
    return response


def links(session, url):
    response = fetch(session, url)
    if "xml" in response.headers.get("Content-Type", "") or response.content.lstrip().startswith(b"<?xml"):
        root = ET.fromstring(response.content)
        for item in root.findall(".//item"):
            title = item.findtext("title", "")
            enclosure = item.find("enclosure")
            target = enclosure.get("url") if enclosure is not None else item.findtext("link")
            if target:
                yield title, urljoin(response.url, target)
    else:
        parser = Anchors()
        parser.feed(response.content.decode(response.encoding or "utf-8", errors="replace")
                    if response.encoding and response.encoding.lower() != "iso-8859-1"
                    else response.content.decode("utf-8", errors="replace"))
        for title, href in parser.items:
            yield title, urljoin(response.url, href)


def is_seed(url):
    return url.startswith("magnet:?") or urlparse(url).path.lower().endswith(".torrent")


def resolve_seed(session, url):
    if is_seed(url):
        return url
    response = fetch(session, url)
    body = response.content.decode("utf-8", errors="replace")
    magnets = {}
    for raw in re.findall(r'''magnet:\?[^\s"'<>]+''', body):
        magnet = unescape(raw)
        for xt in parse_qs(urlparse(magnet).query).get("xt", []):
            if not xt.lower().startswith("urn:btih:"):
                continue
            infohash = xt[9:]
            if re.fullmatch(r"[A-Za-z2-7]{32}", infohash):
                infohash = base64.b32decode(infohash.upper()).hex()
            if re.fullmatch(r"[0-9a-fA-F]{40}", infohash):
                magnets.setdefault(infohash.lower(), magnet)
    parser = Anchors()
    parser.feed(body)
    torrents = [urljoin(response.url, href) for _, href in parser.items
                if urlparse(href).path.lower().endswith(".torrent")]
    candidates = list(magnets.values()) or list(dict.fromkeys(torrents))
    if len(candidates) != 1:
        raise ValueError("Detail page must expose exactly one magnet or .torrent link; configure a direct seed URL otherwise")
    return candidates[0]


def discover(config, session):
    jobs = {}
    for rule in config["subscriptions"]:
        if not rule.get("enabled", True):
            continue
        animation_id = int(rule["animation_id"])
        if animation_id <= 0:
            raise ValueError("animation_id must be positive")
        if "seed_url" in rule:
            matches = [(int(rule["episode_no"]), rule.get("title", ""), rule["seed_url"])]
        elif "page_url" in rule:
            matches = [(int(rule["episode_no"]), rule["title"], rule["page_url"])]
        else:
            pattern = re.compile(rule["title_regex"], re.I)
            if "episode" not in pattern.groupindex:
                raise ValueError("title_regex needs a named (?P<episode>...) group")
            matches = []
            for title, url in links(session, rule["source_url"]):
                match = pattern.search(title)
                if match:
                    matches.append((int(match["episode"]), title, url))
        for number, title, url in matches:
            if number <= 0:
                raise ValueError("Episode number must be positive")
            key = f"{animation_id}-{number}"
            # Prefer the first release on a newest-first feed; never silently replace an episode.
            jobs.setdefault(key, {"animation_id": animation_id, "episode_no": number,
                                  "title": f"第{number}集", "release": title, "source": url,
                                  "direct_seed": "seed_url" in rule})
    return jobs


def settings():
    properties = Path(__file__).resolve().parents[1] / "src/main/resources/application.properties"
    values = {}
    if properties.exists():
        for line in properties.read_text(encoding="utf-8-sig").splitlines():
            if "=" in line and not line.lstrip().startswith(("#", "!")):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()

    def get(env, prop, default=""):
        return os.environ.get(env, values.get(prop, default))

    endpoint = get("OSS_ENDPOINT", "aliyun.oss.endpoint")
    endpoint = endpoint if "://" in endpoint else "https://" + endpoint
    jdbc = get("MYSQL_URL", "spring.datasource.mysql.jdbc-url", "jdbc:mysql://localhost:3306/0721acgn")
    mysql = urlparse(jdbc.removeprefix("jdbc:"))
    return {"endpoint": endpoint, "region": get("OSS_REGION", "", "cn-hangzhou"),
            "key": get("OSS_ACCESS_KEY_ID", "aliyun.oss.access-key-id"),
            "secret": get("OSS_ACCESS_KEY_SECRET", "aliyun.oss.access-key-secret"),
            "bucket": get("OSS_BUCKET", "aliyun.oss.bucket-name"),
            "prefix": get("OSS_PREFIX", "aliyun.oss.folder-prefix", "anime-reality/").strip("/"),
            "db": {"host": mysql.hostname, "port": mysql.port or 3306,
                   "database": mysql.path.lstrip("/"),
                   "user": get("MYSQL_USER", "spring.datasource.mysql.username"),
                   "password": get("MYSQL_PASSWORD", "spring.datasource.mysql.password"),
                   "charset": "utf8mb4", "connect_timeout": 15, "autocommit": True}}


class Downloader:
    def __init__(self, url):
        self.base = url.rstrip("/") + "/api/v2/"
        self.session = requests.Session()
        self.session.headers["Referer"] = url.rstrip("/") + "/"
        answer = self.call("auth/login", {"username": os.environ.get("QBIT_USERNAME", "admin"),
                                          "password": os.environ["QBIT_PASSWORD"]})
        if not (answer.status_code == 204 or (answer.status_code == 200 and answer.text.strip() == "Ok.")):
            raise RuntimeError("qBittorrent login failed")
        # Verify the authenticated session rather than trusting an empty login response.
        version = self.call("app/version")
        if not version.text.strip().startswith("v"):
            raise RuntimeError("qBittorrent authenticated session verification failed")

    def call(self, path, data=None, **kwargs):
        response = self.session.request("POST" if data is not None else "GET", self.base + path,
                                        data=data, timeout=30, **kwargs)
        response.raise_for_status()
        return response

    def completed_file(self, key, seed, work):
        folder = (work / "downloads" / key).resolve()
        torrents = self.call("torrents/info").json()
        matching = [t for t in torrents if Path(t["save_path"]).resolve() == folder]
        if not matching:
            response = self.call("torrents/add", {"urls": seed, "savepath": str(folder), "autoTMM": "false"})
            if not add_accepted(response):
                raise RuntimeError("qBittorrent rejected the seed")
            LOG.info("%s: download submitted", key)
            return None
        if len(matching) != 1:
            raise RuntimeError("Multiple torrents in an episode folder; separate the downloads")
        torrent = matching[0]
        if torrent["state"] in {"error", "missingFiles", "unknown"}:
            raise RuntimeError("qBittorrent reports a download error")
        if torrent["progress"] < 1 or torrent["state"].startswith(("checking", "moving")):
            LOG.info("%s: download %.1f%%", key, torrent["progress"] * 100)
            return None
        files = self.call("torrents/files", params={"hash": torrent["hash"]}).json()
        media = [f for f in files if Path(f["name"]).suffix.lower() in VIDEO_EXTENSIONS]
        if len(media) != 1:
            raise ValueError("Only single-episode torrents are supported; packs need an explicit file/episode mapping")
        if media[0]["progress"] < 1:
            return None
        path = (folder / media[0]["name"]).resolve()
        if not path.is_relative_to(folder) or not path.is_file() or path.stat().st_size != media[0]["size"]:
            raise ValueError("Completed video path/size mismatch; script and qBittorrent must share the same filesystem")
        return path


def add_accepted(response):
    if response.status_code == 200 and response.text.strip() == "Ok.":
        return True
    if response.status_code == 204:
        return True  # Actual torrent presence is checked in the next pass.
    if response.status_code not in (200, 202):
        return False
    try:
        result = response.json()
        return (isinstance(result, dict) and int(result.get("failure_count", 0)) == 0
                and int(result.get("success_count", 0)) + int(result.get("pending_count", 0)) == 1)
    except (ValueError, TypeError):
        return False


def make_mp4(source, target):
    if target.is_file():
        return target
    if not shutil.which("ffmpeg"):
        raise RuntimeError("Install ffmpeg and add it to PATH")
    temporary = target.with_suffix(".partial.mp4")
    subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-threads", "1", "-filter_threads", "1",
                    "-i", str(source), "-map", "0:v:0", "-map", "0:a:0?",
                    "-c:v", "libx264", "-threads", "1", "-crf", "22", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-movflags", "+faststart", str(temporary)], check=True)
    temporary.replace(target)
    return target


def save_state(path, state):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def episode_object_key(prefix, animation_id, episode_no, token):
    return "/".join(filter(None, [prefix.strip("/"), "animation-video", str(animation_id),
                                  f"{episode_no:03d}-{token}.mp4"]))


def download_jobs(jobs, config, session, work, state):
    """Download staging only: deliberately never opens OSS or database clients."""
    downloader = Downloader(config.get("qbit_url", "http://127.0.0.1:8080"))
    for key, job in jobs.items():
        item = state.setdefault(key, {})
        if item.get("url"):
            continue
        if shutil.disk_usage(work).free < float(config.get("min_free_gb", 8)) * 1024 ** 3:
            raise RuntimeError("Insufficient free disk space")
        if "seed" not in item:
            item["seed"] = job["source"] if job.get("direct_seed") else resolve_seed(session, job["source"])
            save_state(work / "state.json", state)
        source = downloader.completed_file(key, item["seed"], work)
        if source:
            LOG.info("%s: download complete; download-only mode leaves the file for review", key)
        break  # Stage one job only; no transcode, upload or SQL.
    return 0


def apply_jobs(jobs, config, session, work, state):
    import oss2
    import pymysql

    cfg = settings()
    if not all((cfg["key"], cfg["secret"], cfg["bucket"])):
        raise ValueError("OSS credentials and bucket are required")
    bucket = oss2.Bucket(oss2.AuthV4(cfg["key"], cfg["secret"]), cfg["endpoint"], cfg["bucket"], region=cfg["region"])
    public_base = config.get("playback_base_url")
    if not public_base or "YOUR_" in public_base or urlparse(public_base).scheme != "https":
        raise ValueError("Set playback_base_url to a readable OSS/CDN URL; the script does not change bucket ACLs")
    if not shutil.which("ffmpeg"):
        raise RuntimeError("Install ffmpeg and add it to PATH")
    downloader = Downloader(config.get("qbit_url", "http://127.0.0.1:8080"))
    failures = 0
    with pymysql.connect(**cfg["db"]) as db:
        for key, job in jobs.items():
            try:
                with db.cursor() as cursor:
                    cursor.execute("SELECT id FROM tb_animation WHERE id=%s", (job["animation_id"],))
                    if not cursor.fetchone():
                        raise ValueError("animation_id does not exist; import the work metadata first")
                    cursor.execute("SELECT video_url FROM tb_animation_episode WHERE animation_id=%s AND episode_no=%s",
                                   (job["animation_id"], job["episode_no"]))
                    if cursor.fetchone():
                        LOG.info("%s: already in database, skipped", key)
                        continue
                item = state.setdefault(key, {})
                if "url" not in item:
                    if config.get("require_burned_in_subtitles") and not any(
                            label in job["release"] for label in ("简体内嵌", "简中内嵌", "简日内嵌", "简日双语内嵌")):
                        raise ValueError("Trial requires a release explicitly labelled with burned-in Simplified Chinese subtitles")
                    if shutil.disk_usage(work).free < float(config.get("min_free_gb", 8)) * 1024 ** 3:
                        raise RuntimeError("Insufficient free disk space; waiting for manual review")
                    if "seed" not in item:
                        item["seed"] = job["source"] if job.get("direct_seed") else resolve_seed(session, job["source"])
                        save_state(work / "state.json", state)
                    source = downloader.completed_file(key, item["seed"], work)
                    if source is None:
                        # One pending download at a time on the 4 GB server.
                        break
                    video = make_mp4(source, work / f"{key}.mp4")
                    digest = hashlib.sha256()
                    with video.open("rb") as stream:
                        for chunk in iter(lambda: stream.read(8 * 1024 * 1024), b""):
                            digest.update(chunk)
                    item.setdefault("object_key", episode_object_key(cfg["prefix"], job["animation_id"],
                                                                    job["episode_no"], str(uuid.uuid4())))
                    item["sha256"] = digest.hexdigest()
                    save_state(work / "state.json", state)
                    object_key = item["object_key"]
                    oss2.resumable_upload(bucket, object_key, str(video),
                                         store=oss2.ResumableStore(root=str(work / "oss-checkpoints")),
                                         headers={"Content-Type": "video/mp4"})
                    if bucket.head_object(object_key).content_length != video.stat().st_size:
                        raise RuntimeError("Uploaded object size does not match local file")
                    item["url"] = public_base.rstrip("/") + "/" + quote(object_key, safe="/")
                    save_state(work / "state.json", state)
                with db.cursor() as cursor:
                    cursor.execute("INSERT INTO tb_animation_episode (animation_id,episode_no,title,video_url) "
                                   "VALUES (%s,%s,%s,%s)", (job["animation_id"], job["episode_no"], job["title"], item["url"]))
                LOG.info("%s: uploaded and registered", key)
            except (ValueError, RuntimeError) as error:
                LOG.error("%s: %s", key, error)
                failures += 1
                break
            except Exception as error:
                # Do not include request URLs or credentials in log output.
                LOG.error("%s: failed (%s); check configuration/service status; retry on next pass", key, type(error).__name__)
                failures += 1
                break
    return failures


def lock_workdir(work):
    handle = (work / "run.lock").open("a+b")
    if handle.seek(0, 2) == 0:
        handle.write(b"0")
        handle.flush()
    handle.seek(0)
    try:
        if os.name == "nt":
            import msvcrt
            msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        handle.close()
        raise RuntimeError("Another ingest process is using this work directory") from None
    return handle


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--download-only", action="store_true", help="With --apply: only submit/check downloads, never transcode or access OSS/database")
    parser.add_argument("--watch", type=int, default=0, help="Repeat interval in seconds (minimum 60)")
    args = parser.parse_args()
    if args.download_only and not args.apply:
        parser.error("--download-only requires --apply")
    if args.watch and args.watch < 60:
        parser.error("--watch must be at least 60 seconds")
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    config = json.loads(args.config.read_text(encoding="utf-8-sig"))
    work = Path(config.get("work_dir", "../uploads/animation-ingest"))
    if not work.is_absolute():
        work = args.config.resolve().parent / work
    work = work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    lock = lock_workdir(work)
    try:
        path = work / "state.json"
        state = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        session = requests.Session()
        session.headers["User-Agent"] = "0721ACGN-AnimationIngest/1.0"
        while True:
            try:
                discovery_failed = False
                try:
                    jobs = discover(config, session)
                except Exception as error:
                    LOG.error("Discovery failed (%s); continuing previously queued jobs", type(error).__name__)
                    jobs = {}
                    discovery_failed = True
                # Persist discovered jobs so a release disappearing from the feed can still finish.
                if args.apply:
                    for key, job in jobs.items():
                        state.setdefault(key, {}).setdefault("job", job)
                    save_state(path, state)
                    jobs = {key: item["job"] for key, item in state.items() if "job" in item}
                for key, job in jobs.items():
                    LOG.info("Matched %s: %s", key, job["release"])
                if not jobs:
                    LOG.warning("No matches; check source_url and title_regex")
                if args.apply and jobs:
                    handler = download_jobs if args.download_only else apply_jobs
                    failures = handler(jobs, config, session, work, state)
                else:
                    failures = 0
                failures += int(discovery_failed)
            except (ValueError, RuntimeError) as error:
                LOG.error("%s", error)
                failures = 1
            except Exception as error:
                LOG.error("Pass failed (%s); inspect configuration and service availability", type(error).__name__)
                failures = 1
            if not args.watch:
                return 1 if failures else 0
            time.sleep(args.watch)
    finally:
        lock.close()


if __name__ == "__main__":
    raise SystemExit(main())
