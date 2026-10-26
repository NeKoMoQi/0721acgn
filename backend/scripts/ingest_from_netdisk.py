"""Explicit netdisk manifest -> verified local MP4 -> OSS -> existing episodes.

Default: validate/print the manifest only. --apply downloads and publishes;
--apply --download-only downloads and inspects without accessing OSS or MySQL.
BaiduPCS-Go must already be authenticated under the account running this script.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import logging
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
import subprocess
import sys
import tempfile
from urllib.parse import quote, urlparse
import uuid

import requests

from audit_animation import database_config
from ingest_animation import episode_object_key, lock_workdir, save_state

LOG = logging.getLogger("netdisk-ingest")


def validate_jobs(config):
    jobs, seen = [], set()
    for raw in config.get("jobs", []):
        if not raw.get("enabled", True):
            continue
        job = dict(raw)
        for field in ("animation_id", "episode_no", "expected_size"):
            if type(job.get(field)) is not int or job[field] <= 0:
                raise ValueError(f"{field} must be an explicit positive integer")
        if not job.get("animation_name"):
            raise ValueError("animation_name is required for exact database matching")
        path = PurePosixPath(job.get("netdisk_path", ""))
        if not path.is_absolute() or ".." in path.parts or path.suffix.lower() not in {".mp4", ".mkv", ".mov", ".webm"}:
            raise ValueError("netdisk_path must be an absolute video file path, not a directory")
        if job.get("subtitle_language") not in {"zh-Hans", "zh-Hant"}:
            raise ValueError("Explicit Chinese subtitle language is required")
        if job.get("subtitle_kind") != "burned_in":
            raise ValueError("This pipeline requires burned-in subtitles; soft/external subtitles need a separate conversion plan")
        key = f"{job['animation_id']}-{job['episode_no']}"
        if key in seen:
            raise ValueError("Duplicate animation/episode mapping in manifest")
        seen.add(key)
        jobs.append((key, job))
    return jobs


def run_tool(args, timeout=7200, capture=False):
    # BaiduPCS-Go exits non-zero when stdout is a null device, so send output to a real file.
    sink_path = os.path.join(tempfile.gettempdir(), "netdisk-ingest-tool.log")
    with open(sink_path, "ab") as sink:
        result = subprocess.run(args, stdin=subprocess.DEVNULL,
                                stdout=subprocess.PIPE if capture else sink,
                                stderr=subprocess.DEVNULL if capture else sink,
                                timeout=timeout, check=False)
    if result.returncode:
        raise RuntimeError("External tool failed; execution stopped (authentication may need renewal)")
    return result.stdout if capture else None


def probe(path):
    data = json.loads(run_tool(["ffprobe", "-v", "error", "-show_format", "-show_streams",
                                "-of", "json", str(path)], timeout=120, capture=True))
    streams = data.get("streams", [])
    videos = [s for s in streams if s.get("codec_type") == "video" and not s.get("disposition", {}).get("attached_pic")]
    audio = [s for s in streams if s.get("codec_type") == "audio"]
    if len(videos) != 1 or len(audio) != 1:
        raise ValueError("Expected exactly one video and one audio track; select tracks explicitly for other files")
    duration = float(data.get("format", {}).get("duration", 0))
    if duration <= 0 or not videos[0].get("width") or not videos[0].get("height"):
        raise ValueError("Media dimensions/duration are invalid")
    return {"video": videos[0], "audio": audio[0], "duration": duration,
            "format": data.get("format", {}).get("format_name", "")}


def compatible(info):
    video = info["video"]
    return ("mp4" in info["format"].split(",") and video.get("codec_name") == "h264"
            and video.get("pix_fmt") == "yuv420p" and video["width"] <= 1920 and video["height"] <= 1080
            and info["audio"].get("codec_name") == "aac")


def faststart(path):
    """Inspect only top-level MP4 atoms; do not load a large video into memory."""
    with path.open("rb") as stream:
        total = path.stat().st_size
        offset = 0
        while offset + 8 <= total:
            stream.seek(offset)
            size, kind = struct.unpack(">I4s", stream.read(8))
            header = 8
            if size == 1:
                if offset + 16 > total:
                    return False
                size = struct.unpack(">Q", stream.read(8))[0]
                header = 16
            if size == 0:
                size = total - offset
            if size < header or offset + size > total:
                return False
            if kind == b"moov":
                return True
            if kind == b"mdat":
                return False
            offset += size
    return False


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            checksum.update(block)
    return checksum.hexdigest()


def download(job, config, folder):
    filename = PurePosixPath(job["netdisk_path"]).name
    destination = folder / filename
    if not destination.exists():
        folder.mkdir(parents=True, exist_ok=True)
        run_tool([config["pcs_executable"], "download", "--saveto", str(folder), "-l", "1", "-p", "2",
                  "--retry", "2", job["netdisk_path"]], timeout=int(config.get("download_timeout_seconds", 7200)))
    if not destination.resolve().is_relative_to(folder.resolve()):
        raise ValueError("Downloaded file escapes its dedicated work directory")
    # Some clients report zero exit status on failures or skip existing files.
    # Never use process success alone as proof that a download completed.
    if not destination.is_file() or destination.stat().st_size != job["expected_size"]:
        raise ValueError("Download missing/size mismatch; stop and inspect login or incomplete file (nothing was deleted)")
    return destination


def inspect(source, job, folder):
    info = probe(source)
    expected = job.get("expected_duration_seconds")
    if expected is not None and abs(info["duration"] - float(expected)) > float(job.get("duration_tolerance_seconds", 30)):
        raise ValueError("Episode duration differs from the manifest")
    run_tool(["ffmpeg", "-nostdin", "-v", "error", "-xerror", "-threads", "1", "-i", str(source),
              "-map", "0:v:0", "-map", "0:a:0", "-f", "null", "-"], timeout=3600)
    (folder / "media.json").write_text(json.dumps(info, ensure_ascii=False, indent=2), encoding="utf-8")
    for name, seconds in (("preview-start.jpg", 60), ("preview-middle.jpg", info["duration"] / 2)):
        run_tool(["ffmpeg", "-nostdin", "-v", "error", "-y", "-threads", "1", "-ss", str(min(seconds, info["duration"] / 2)),
                  "-i", str(source), "-frames:v", "1", str(folder / name)], timeout=120)
    return info


def prepare(source, info, folder):
    if compatible(info) and faststart(source):
        return source, "copy-original"
    output = folder / "prepared.mp4"
    temporary = folder / "prepared.partial.mp4"
    command = ["ffmpeg", "-nostdin", "-v", "error", "-y", "-threads", "1", "-filter_threads", "1",
               "-i", str(source), "-map", "0:v:0", "-map", "0:a:0"]
    if compatible(info):
        mode = "remux"
        command += ["-c", "copy"]
    else:
        mode = "transcode"
        command += ["-vf", "scale=w='min(1920,iw)':h='min(1080,ih)':force_original_aspect_ratio=decrease:force_divisible_by=2",
                    "-c:v", "libx264", "-threads", "1", "-preset", "fast", "-crf", "22", "-pix_fmt", "yuv420p",
                    "-c:a", "aac"]
    run_tool(command + ["-movflags", "+faststart", str(temporary)], timeout=14400)
    ready_info = probe(temporary)
    if not compatible(ready_info) or not faststart(temporary) or abs(ready_info["duration"] - info["duration"]) > 2:
        raise ValueError("Prepared file failed format/duration validation")
    temporary.replace(output)
    return output, mode


def oss_config(properties):
    values = {}
    for line in properties.read_text(encoding="utf-8-sig").splitlines():
        if "=" in line and not line.lstrip().startswith(("#", "!")):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    def get(env, prop, default=""):
        value = os.environ.get(env, values.get(prop, default))
        match = re.fullmatch(r"\$\{([^}:]+)(?::([^}]*))?\}", value)
        if match:
            value = os.environ.get(match[1], match[2])
        if value is None or "${" in value or value.startswith("YOUR_"):
            raise ValueError("Unresolved OSS configuration")
        return value
    endpoint = get("OSS_ENDPOINT", "aliyun.oss.endpoint")
    result = {"endpoint": endpoint if "://" in endpoint else "https://" + endpoint,
              "region": get("OSS_REGION", "aliyun.oss.region", "cn-hangzhou"),
              "bucket": get("OSS_BUCKET", "aliyun.oss.bucket-name"),
              "key": get("OSS_ACCESS_KEY_ID", "aliyun.oss.access-key-id"),
              "secret": get("OSS_ACCESS_KEY_SECRET", "aliyun.oss.access-key-secret"),
              "prefix": get("OSS_PREFIX", "aliyun.oss.folder-prefix", "anime-reality/")}
    if not all(result.values()) or not urlparse(result["endpoint"]).hostname:
        raise ValueError("OSS configuration is incomplete")
    return result


def verify_public(url, size):
    with requests.Session() as session:
        head = session.head(url, timeout=30)
        head.raise_for_status()
        if int(head.headers.get("Content-Length", -1)) != size or head.headers.get("Content-Type", "").split(";")[0] != "video/mp4":
            raise ValueError("Public object length/type mismatch")
        with session.get(url, headers={"Range": "bytes=0-0"}, stream=True, timeout=30) as response:
            if response.status_code != 206 or response.headers.get("Content-Range") != f"bytes 0-0/{size}":
                raise ValueError("Public playback URL failed Range verification")
            if not next(response.iter_content(chunk_size=1), b""):
                raise ValueError("Public playback URL returned no data")


def existing(db, job):
    with db.cursor() as cursor:
        cursor.execute("SELECT name FROM tb_animation WHERE id=%s", (job["animation_id"],))
        names = cursor.fetchall()
        if len(names) != 1 or names[0][0].strip() != job["animation_name"].strip():
            raise ValueError("Manifest animation ID/name does not match exactly one existing work")
        cursor.execute("SELECT video_url FROM tb_animation_episode WHERE animation_id=%s AND episode_no=%s",
                       (job["animation_id"], job["episode_no"]))
        return cursor.fetchone()


def register(db, job, url):
    db.begin()
    try:
        with db.cursor() as cursor:
            cursor.execute("SELECT video_url FROM tb_animation_episode WHERE animation_id=%s AND episode_no=%s FOR UPDATE",
                           (job["animation_id"], job["episode_no"]))
            row = cursor.fetchone()
            if row:
                if row[0] != url:
                    raise ValueError("Episode was added by another process; existing URL preserved")
            else:
                cursor.execute("INSERT INTO tb_animation_episode (animation_id,episode_no,title,video_url) VALUES (%s,%s,%s,%s)",
                               (job["animation_id"], job["episode_no"], f"第{job['episode_no']}集", url))
        db.commit()
    except Exception:
        db.rollback()
        raise


def publish(db, job, ready, config, properties, item, work, state):
    size, checksum = ready.stat().st_size, digest(ready)
    if item.get("uploaded") and (item.get("output_sha256") != checksum or item.get("output_size") != size):
        raise ValueError("Prepared file changed after upload; automatic replacement is disabled")
    if not item.get("uploaded"):
        import oss2
        cfg = oss_config(properties)
        bucket = oss2.Bucket(oss2.AuthV4(cfg["key"], cfg["secret"]), cfg["endpoint"], cfg["bucket"], region=cfg["region"])
        item.setdefault("object_key", episode_object_key(cfg["prefix"], job["animation_id"], job["episode_no"], str(uuid.uuid4())))
        save_state(work / "state.json", state)
        oss2.resumable_upload(bucket, item["object_key"], str(ready), store=oss2.ResumableStore(root=str(work / "checkpoints")),
                             headers={"Content-Type": "video/mp4", "x-oss-meta-sha256": checksum})
        if bucket.head_object(item["object_key"]).content_length != size:
            raise ValueError("OSS object size mismatch")
        item.update(uploaded=True, output_size=size, output_sha256=checksum,
                    url=config["playback_base_url"].rstrip("/") + "/" + quote(item["object_key"], safe="/"))
        save_state(work / "state.json", state)
    verify_public(item["url"], size)
    register(db, job, item["url"])
    item["status"] = "completed"
    save_state(work / "state.json", state)


def process(jobs, config, properties, work, download_only=False):
    path = work / "state.json"
    state = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    db = None
    if not download_only:
        import pymysql
        cfg = database_config(properties)
        cfg["autocommit"] = True
        db = pymysql.connect(**cfg)
    try:
        for key, job in jobs:
            if db is not None and existing(db, job):
                LOG.info("%s: existing episode preserved; skipped before download/upload", key)
                continue
            item = state.setdefault(key, {})
            # Human review is expected to change false -> true after download-only.
            identity = {k: v for k, v in job.items() if k != "subtitles_verified"}
            fingerprint = hashlib.sha256(json.dumps(identity, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
            if item.get("manifest_fingerprint", fingerprint) != fingerprint:
                raise ValueError("Manifest changed for a pending job; review the old job before restarting")
            item["manifest_fingerprint"] = fingerprint
            if shutil.disk_usage(work).free < float(config.get("min_free_gb", 8)) * 1024 ** 3 + job["expected_size"] * 3:
                raise ValueError("Insufficient free space; execution stopped")
            folder = work / key
            source = download(job, config, folder)
            info = inspect(source, job, folder)
            source_checksum = digest(source)
            if item.get("source_sha256", source_checksum) != source_checksum:
                raise ValueError("Previously inspected source content changed; review required")
            item.update(source_size=source.stat().st_size, source_sha256=source_checksum, status="inspected")
            save_state(path, state)
            if download_only:
                LOG.info("%s: downloaded/inspected; preview frames saved; no cloud/database access", key)
                continue
            if job.get("subtitles_verified") is not True:
                raise ValueError("Human subtitle review required; inspect previews and set subtitles_verified=true")
            if item.get("prepared_file"):
                ready = (work / item["prepared_file"]).resolve()
                if not ready.is_relative_to(work.resolve()) or not ready.is_file() or digest(ready) != item["prepared_sha256"]:
                    raise ValueError("Previously prepared file is missing or changed; review required")
                mode = item["media_action"]
            else:
                ready, mode = prepare(source, info, folder)
                item.update(prepared_file=str(ready.relative_to(work)), prepared_sha256=digest(ready), media_action=mode)
                save_state(path, state)
            item["media_action"] = mode
            LOG.info("%s: %s", key, mode)
            publish(db, job, ready, config, properties, item, work, state)
            if config.get("cleanup_local", False):
                for file in {source, ready}:
                    if not file.resolve().is_relative_to(work.resolve()):
                        raise ValueError("Cleanup path outside work directory")
                    file.unlink(missing_ok=True)
            LOG.info("%s: completed", key)
    finally:
        if db is not None:
            db.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--properties", type=Path, default=Path(__file__).resolve().parents[1] / "src/main/resources/application.properties")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--download-only", action="store_true")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    lock = None
    try:
        config = json.loads(args.manifest.read_text(encoding="utf-8-sig"))
        jobs = validate_jobs(config)
        for key, job in jobs:
            LOG.info("%s: %s, subtitles=%s, verified=%s", key, job["animation_name"], job["subtitle_language"], job.get("subtitles_verified", False))
        if not args.apply:
            LOG.info("Manifest preview only; no downloads, cloud or database access")
            return 0
        if not args.download_only and urlparse(config.get("playback_base_url", "")).scheme != "https":
            raise ValueError("A readable HTTPS playback_base_url is required")
        work = Path(config["work_dir"])
        if not work.is_absolute():
            work = args.manifest.resolve().parent / work
        work.mkdir(parents=True, exist_ok=True)
        lock = lock_workdir(work)
        process(jobs, config, args.properties, work, args.download_only)
        return 0
    except (ValueError, RuntimeError, FileNotFoundError) as error:
        LOG.error("%s", error)
        return 1
    except KeyboardInterrupt:
        LOG.info("Stopped; recorded state and local files retained")
        return 130
    except Exception as error:
        # Do not print HTTP URLs, cloud credentials, cookies or database exception text.
        LOG.error("Stopped: %s (credentials/configuration may need review)", type(error).__name__)
        return 1
    finally:
        if lock:
            lock.close()


if __name__ == "__main__":
    sys.exit(main())
