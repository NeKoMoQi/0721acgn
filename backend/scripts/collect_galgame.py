# -*- coding: utf-8 -*-
"""
Galgame 数据采集与入库脚本（首批 20 部）。

流程：
  1. 读取 galgame_catalog.json（作品名单 + 人工整理的中文简介）。
  2. 从 VNDB 公共 API 拉取原名、发行日期、开发商、平台、标签、评分与评分人数。
  3. 从萌娘百科 MediaWiki API 拉取中文条目摘要与页面地址。
  4. 可选核对 Steam 商店页（官方发行信息）。
  5. 下载 VNDB 封面缩略图到 frontend/public/galgame/，仅接受 sexual=0、violence=0 的图片。
  6. 生成 backend/data/galgame_seed.json、backend/sql/galgame_seed.sql，
     并可用 --apply 写入 MySQL 的 tb_galgame。

访问规则：
  - 只使用公开 API，不登录、不绕过验证码或访问限制。
  - 每次请求间隔 0.8 秒，20 秒超时，最多 2 次重试；响应缓存 7 天。
  - Bangumi 在当前网络下连接超时，脚本会记录失败并继续，不伪造其结果。

示例：
  python collect_galgame.py --refresh
  python collect_galgame.py --apply --refresh
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any

import requests

ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = ROOT.parent
CATALOG_PATH = ROOT / "scripts" / "galgame_catalog.json"
CACHE_DIR = ROOT / ".cache" / "galgame"
DATA_DIR = ROOT / "data"
SQL_DIR = ROOT / "sql"
COVER_DIR = PROJECT_ROOT / "frontend" / "public" / "galgame"
REPORT_PATH = DATA_DIR / "galgame_collection_report.json"
SEED_JSON_PATH = DATA_DIR / "galgame_seed.json"
SEED_SQL_PATH = SQL_DIR / "galgame_seed.sql"
SCHEMA_SQL_PATH = SQL_DIR / "galgame.sql"

UA = "ACGN-BISHE-Galgame/1.0 (local school project; public API only)"
TIMEOUT = 20
DELAY = 0.8
CACHE_TTL_SECONDS = 7 * 24 * 3600

DDL = """
CREATE TABLE IF NOT EXISTS `tb_galgame` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(64) NOT NULL COMMENT '稳定标识，用于去重与封面文件名',
  `name_zh` VARCHAR(255) NOT NULL COMMENT '中文名',
  `name_original` VARCHAR(255) DEFAULT NULL COMMENT '原名',
  `aliases` VARCHAR(512) DEFAULT NULL COMMENT '别名，/ 分隔',
  `short_intro` VARCHAR(500) DEFAULT NULL COMMENT '列表用简短介绍',
  `intro` TEXT COMMENT '详情用无剧透简介',
  `cover` VARCHAR(512) DEFAULT NULL COMMENT '封面地址',
  `developer` VARCHAR(255) DEFAULT NULL COMMENT '开发商',
  `release_date` VARCHAR(32) DEFAULT NULL COMMENT '首发日期',
  `release_year` INT DEFAULT NULL COMMENT '发行年份，用于年代筛选',
  `release_note` VARCHAR(255) DEFAULT NULL COMMENT '版本/平台发行说明',
  `platforms` VARCHAR(255) DEFAULT NULL COMMENT '支持平台',
  `genres` VARCHAR(512) DEFAULT NULL COMMENT '题材/标签，逗号分隔',
  `rating` DECIMAL(3,1) DEFAULT NULL COMMENT '评分（十分制）',
  `rating_count` INT DEFAULT NULL COMMENT '评分人数',
  `rating_source` VARCHAR(64) DEFAULT NULL COMMENT '评分来源',
  `rating_source_url` VARCHAR(512) DEFAULT NULL COMMENT '评分来源链接',
  `moegirl_url` VARCHAR(512) DEFAULT NULL,
  `bangumi_url` VARCHAR(512) DEFAULT NULL,
  `vndb_url` VARCHAR(512) DEFAULT NULL,
  `official_url` VARCHAR(512) DEFAULT NULL,
  `steam_url` VARCHAR(512) DEFAULT NULL,
  `sources_json` TEXT COMMENT '来源链接、采集时间与字段说明',
  `source_note` VARCHAR(500) DEFAULT NULL COMMENT '来源限制/版本说明',
  `featured` TINYINT NOT NULL DEFAULT 0 COMMENT '首页轮播推荐',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '默认排序',
  `fetched_at` DATETIME DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_galgame_slug` (`slug`),
  KEY `idx_galgame_year` (`release_year`),
  KEY `idx_galgame_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Galgame 作品资料（VNDB/萌娘百科公开资料整理）'
""".strip()

PLATFORM_MAP = {
    "win": "PC (Windows)",
    "lin": "Linux",
    "mac": "macOS",
    "ios": "iOS",
    "and": "Android",
    "psp": "PSP",
    "ps2": "PS2",
    "ps3": "PS3",
    "ps4": "PS4",
    "ps5": "PS5",
    "psv": "PS Vita",
    "swi": "Nintendo Switch",
    "sw2": "Nintendo Switch 2",
    "nds": "Nintendo DS",
    "n3d": "Nintendo 3DS",
    "gba": "Game Boy Advance",
    "gbc": "Game Boy Color",
    "wii": "Wii",
    "xb3": "Xbox 360",
    "xbo": "Xbox One",
    "xxs": "Xbox Series X/S",
    "web": "网页",
    "mob": "手机",
}

FEATURED_SLUGS = {
    "clannad",
    "steins-gate",
    "white-album-2",
    "summer-pockets",
    "fata-morgana",
    "atri",
}


def now_iso() -> str:
    return dt.datetime.now().astimezone().isoformat(timespec="seconds")


def log(msg: str) -> None:
    print(msg, flush=True)


def ensure_dirs() -> None:
    for p in (CACHE_DIR, DATA_DIR, SQL_DIR, COVER_DIR):
        p.mkdir(parents=True, exist_ok=True)


def cache_path(source: str, key: str) -> Path:
    digest = hashlib.sha1(key.encode("utf-8")).hexdigest()[:16]
    safe = re.sub(r"[^0-9A-Za-z_.-]+", "_", key)[:40]
    return CACHE_DIR / f"{source}_{safe}_{digest}.json"


def read_cache(source: str, key: str, refresh: bool) -> Any | None:
    p = cache_path(source, key)
    if refresh or not p.exists():
        return None
    try:
        payload = json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    if time.time() - float(payload.get("_cached_at", 0)) > CACHE_TTL_SECONDS:
        return None
    return payload.get("data")


def write_cache(source: str, key: str, data: Any) -> None:
    p = cache_path(source, key)
    p.write_text(
        json.dumps({"_cached_at": time.time(), "data": data}, ensure_ascii=False),
        encoding="utf-8",
    )


def get_json(
    session: requests.Session,
    source: str,
    key: str,
    url: str,
    *,
    method: str = "GET",
    params: dict | None = None,
    body: dict | None = None,
    refresh: bool = False,
    timeout: int = TIMEOUT,
) -> Any:
    cached = read_cache(source, key, refresh)
    if cached is not None:
        return cached
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            if method == "POST":
                resp = session.post(url, params=params, json=body, timeout=timeout)
            else:
                resp = session.get(url, params=params, timeout=timeout)
            resp.raise_for_status()
            data = resp.json()
            write_cache(source, key, data)
            time.sleep(DELAY)
            return data
        except Exception as e:  # noqa: BLE001
            last_err = e
            if attempt == 0:
                time.sleep(1.2)
    raise RuntimeError(f"{source} 请求失败：{type(last_err).__name__}: {last_err}")


def fetch_vndb(session: requests.Session, vid: str, refresh: bool) -> dict:
    data = get_json(
        session,
        "vndb",
        vid,
        "https://api.vndb.org/kana/vn",
        method="POST",
        body={
            "filters": ["id", "=", vid],
            "fields": (
                "id,title,alttitle,titles{lang,title,latin,official,main},aliases,released,"
                "developers{name},rating,votecount,platforms,image{url,sexual,violence},length"
            ),
        },
        refresh=refresh,
    )
    results = data.get("results") or []
    if not results:
        raise RuntimeError(f"VNDB 未找到条目 {vid}")
    return results[0]


def fetch_moegirl(session: requests.Session, title: str, refresh: bool) -> dict:
    data = get_json(
        session,
        "moegirl",
        title,
        "https://zh.moegirl.org.cn/api.php",
        params={
            "action": "query",
            "format": "json",
            "formatversion": "2",
            "prop": "extracts|pageimages|info",
            "exintro": "1",
            "explaintext": "1",
            "piprop": "original",
            "inprop": "url",
            "redirects": "1",
            "titles": title,
        },
        refresh=refresh,
    )
    pages = data.get("query", {}).get("pages", [])
    if not pages:
        raise RuntimeError("萌娘百科未返回页面")
    page = pages[0]
    if page.get("missing"):
        raise RuntimeError(f"萌娘百科页面不存在：{title}")
    return {
        "title": page.get("title") or title,
        "pageid": page.get("pageid"),
        "url": page.get("fullurl") or f"https://zh.moegirl.org.cn/{title}",
        "extract": (page.get("extract") or "").strip(),
    }


def fetch_steam(session: requests.Session, appid: int, refresh: bool) -> dict | None:
    try:
        data = get_json(
            session,
            "steam",
            str(appid),
            "https://store.steampowered.com/api/appdetails",
            params={"appids": appid, "l": "schinese", "cc": "CN"},
            refresh=refresh,
        )
        block = data.get(str(appid)) or {}
        if not block.get("success"):
            return None
        d = block.get("data") or {}
        return {
            "name": d.get("name"),
            "release_date": (d.get("release_date") or {}).get("date"),
            "developers": d.get("developers") or [],
            "platforms": d.get("platforms") or {},
            "url": f"https://store.steampowered.com/app/{appid}/",
        }
    except Exception:  # noqa: BLE001
        return None


def probe_bangumi(session: requests.Session) -> dict:
    """只探测一次，失败即记录原因；不重试、不伪造数据。"""
    url = "https://api.bgm.tv/v0/search/subjects"
    try:
        resp = session.post(
            url,
            params={"limit": 1},
            json={"keyword": "CLANNAD", "filter": {"type": [4]}},
            timeout=10,
        )
        if resp.status_code == 200:
            return {"ok": True, "url": url, "note": "Bangumi API 可用"}
        return {"ok": False, "url": url, "error": f"HTTP {resp.status_code}"}
    except Exception as e:  # noqa: BLE001
        return {"ok": False, "url": url, "error": f"{type(e).__name__}: {e}"}


def safe_platforms(codes: list[str]) -> str:
    names: list[str] = []
    for code in codes or []:
        name = PLATFORM_MAP.get(str(code))
        if name and name not in names:
            names.append(name)
    return " / ".join(names)


def parse_year(date: str | None) -> int | None:
    if not date:
        return None
    m = re.match(r"(\d{4})", str(date))
    return int(m.group(1)) if m else None


def download_cover(session: requests.Session, url: str, slug: str, refresh: bool) -> tuple[str, str]:
    """返回 (封面地址, 错误说明)。下载失败时退回远程地址。"""
    dest = COVER_DIR / f"{slug}.jpg"
    public_path = f"/galgame/{slug}.jpg"
    if dest.exists() and dest.stat().st_size > 1000 and not refresh:
        return public_path, ""
    try:
        resp = session.get(url, timeout=TIMEOUT, stream=True)
        resp.raise_for_status()
        ctype = (resp.headers.get("Content-Type") or "").lower()
        if "image" not in ctype:
            raise RuntimeError(f"非图片响应：{ctype}")
        size = 0
        tmp = dest.with_suffix(".jpg.part")
        with tmp.open("wb") as fh:
            for chunk in resp.iter_content(64 * 1024):
                if not chunk:
                    continue
                size += len(chunk)
                if size > 8 * 1024 * 1024:
                    raise RuntimeError("图片超过 8MB")
                fh.write(chunk)
        if size < 1000:
            tmp.unlink(missing_ok=True)
            raise RuntimeError("图片过小")
        tmp.replace(dest)
        time.sleep(DELAY)
        return public_path, ""
    except Exception as e:  # noqa: BLE001
        return url, f"封面下载失败，暂用远程地址：{type(e).__name__}"


def build_rows(catalog: dict, session: requests.Session, refresh: bool, cover: bool) -> tuple[list[dict], list[dict]]:
    rows: list[dict] = []
    errors: list[dict] = []
    total = len(catalog["items"])
    for index, item in enumerate(catalog["items"], start=1):
        slug = item["slug"]
        log(f"[{index}/{total}] {item['name_zh']} …")
        try:
            vndb = fetch_vndb(session, item["vndb_id"], refresh)
        except Exception as e:  # noqa: BLE001
            errors.append({"slug": slug, "source": "VNDB", "error": str(e)})
            continue
        try:
            moegirl = fetch_moegirl(session, item["moegirl_title"], refresh)
        except Exception as e:  # noqa: BLE001
            moegirl = {"title": item["moegirl_title"], "url": "", "extract": ""}
            errors.append({"slug": slug, "source": "萌娘百科", "error": str(e)})

        steam = fetch_steam(session, item["steam_appid"], refresh) if item.get("steam_appid") else None

        image = vndb.get("image") or {}
        cover_url = ""
        cover_err = ""
        safe_image = image.get("url") and int(image.get("sexual") or 0) == 0 and int(image.get("violence") or 0) == 0
        if safe_image and cover:
            cover_url, cover_err = download_cover(session, image["url"], slug, refresh)
        elif safe_image:
            cover_url = image["url"]
        if cover_err:
            errors.append({"slug": slug, "source": "VNDB 封面", "error": cover_err})

        release_date = item.get("release_date") or vndb.get("released")
        rating = vndb.get("rating")
        rating_10 = round(float(rating) / 10, 1) if rating is not None else None
        sources = [
            {
                "name": "VNDB",
                "url": f"https://vndb.org/{vndb.get('id')}",
                "retrieved_at": now_iso(),
                "fields": ["原名", "发行日期", "开发商", "支持平台", "评分", "评分人数"],
            }
        ]
        if moegirl.get("url"):
            sources.append(
                {
                    "name": "萌娘百科",
                    "url": moegirl["url"],
                    "retrieved_at": now_iso(),
                    "fields": ["中文名", "别名", "背景介绍"],
                    "license": "萌娘百科内容适用 CC BY-NC-SA 3.0 CN",
                }
            )
        if steam:
            sources.append(
                {
                    "name": "Steam 商店页",
                    "url": steam["url"],
                    "retrieved_at": now_iso(),
                    "fields": ["发行信息核对", "开发商核对", "平台核对"],
                }
            )
        source_note = "结构化资料取自 VNDB；中文名、别名与背景参考萌娘百科；题材标签根据 VNDB 标签与萌娘百科介绍人工整理。"
        if item.get("release_note"):
            source_note += f" 版本说明：{item['release_note']}。"
        if cover_err:
            source_note += " " + cover_err + "。"
        if not moegirl.get("extract"):
            source_note += " 萌娘百科页面摘要为空，简介依据 VNDB 与公开商店页整理。"

        rows.append(
            {
                "slug": slug,
                "name_zh": item["name_zh"],
                "name_original": vndb.get("alttitle") or vndb.get("title") or item.get("name_zh"),
                "aliases": " / ".join(item.get("aliases") or []),
                "short_intro": item["short_intro"],
                "intro": item["intro"],
                "cover": cover_url,
                "developer": item.get("developer") or ", ".join(d["name"] for d in vndb.get("developers") or []),
                "release_date": release_date or "",
                "release_year": parse_year(release_date),
                "release_note": item.get("release_note") or "",
                "platforms": safe_platforms(vndb.get("platforms") or []),
                "genres": ",".join(item.get("genres") or []),
                "rating": rating_10,
                "rating_count": vndb.get("votecount"),
                "rating_source": "VNDB" if rating_10 is not None else "",
                "rating_source_url": f"https://vndb.org/{vndb.get('id')}",
                "moegirl_url": moegirl.get("url") or "",
                "bangumi_url": "",
                "vndb_url": f"https://vndb.org/{vndb.get('id')}",
                "official_url": item.get("official_url") or "",
                "steam_url": steam["url"] if steam else "",
                "sources_json": json.dumps(sources, ensure_ascii=False),
                "source_note": source_note,
                "featured": 1 if slug in FEATURED_SLUGS else 0,
                "sort_order": index,
                "fetched_at": now_iso(),
            }
        )
    return rows, errors


def sql_escape(value: Any) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    s = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


def write_sql(rows: list[dict]) -> None:
    SCHEMA_SQL_PATH.write_text(DDL + ";\n", encoding="utf-8")
    cols = [
        "slug", "name_zh", "name_original", "aliases", "short_intro", "intro",
        "cover", "developer", "release_date", "release_year", "release_note",
        "platforms", "genres", "rating", "rating_count", "rating_source",
        "rating_source_url", "moegirl_url", "bangumi_url", "vndb_url",
        "official_url", "steam_url", "sources_json", "source_note",
        "featured", "sort_order", "fetched_at",
    ]
    lines = ["SET NAMES utf8mb4;", "USE `0721acgn`;", "", DDL + ";", ""]
    for row in rows:
        values = ", ".join(sql_escape(row.get(c)) for c in cols)
        updates = ", ".join(f"`{c}` = VALUES(`{c}`)" for c in cols if c != "slug")
        lines.append(
            f"INSERT INTO `tb_galgame` ({', '.join(f'`{c}`' for c in cols)}) VALUES ({values})\n"
            f"ON DUPLICATE KEY UPDATE {updates};"
        )
    SEED_SQL_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def load_db_defaults() -> dict:
    props = ROOT / "src" / "main" / "resources" / "application.properties"
    out = {"user": "root", "password": "", "database": "0721acgn", "host": "127.0.0.1", "port": 3306}
    if props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line.startswith("spring.datasource.mysql.username="):
                out["user"] = line.split("=", 1)[1].strip()
            elif line.startswith("spring.datasource.mysql.password="):
                out["password"] = line.split("=", 1)[1].strip()
            elif line.startswith("spring.datasource.mysql.jdbc-url="):
                m = re.search(r"//([^:/]+):(\d+)/([^?]+)", line)
                if m:
                    out["host"], out["port"], out["database"] = m.group(1), int(m.group(2)), m.group(3)
    out["user"] = os.environ.get("BISHE_MYSQL_USER", out["user"])
    out["password"] = os.environ.get("BISHE_MYSQL_PASSWORD", out["password"])
    out["database"] = os.environ.get("BISHE_MYSQL_DB", out["database"])
    return out


def apply_mysql(rows: list[dict], db: dict) -> None:
    try:
        import pymysql
    except ImportError as e:  # noqa: BLE001
        raise RuntimeError("缺少 PyMySQL，请使用 backend/scripts/.venv 运行或 pip install pymysql") from e
    conn = pymysql.connect(
        host=db["host"],
        port=db["port"],
        user=db["user"],
        password=db["password"],
        database=db["database"],
        charset="utf8mb4",
        autocommit=False,
    )
    cols = [
        "slug", "name_zh", "name_original", "aliases", "short_intro", "intro",
        "cover", "developer", "release_date", "release_year", "release_note",
        "platforms", "genres", "rating", "rating_count", "rating_source",
        "rating_source_url", "moegirl_url", "bangumi_url", "vndb_url",
        "official_url", "steam_url", "sources_json", "source_note",
        "featured", "sort_order", "fetched_at",
    ]
    placeholders = ", ".join(["%s"] * len(cols))
    updates = ", ".join(f"`{c}` = VALUES(`{c}`)" for c in cols if c != "slug")
    sql = (
        f"INSERT INTO `tb_galgame` ({', '.join(f'`{c}`' for c in cols)}) VALUES ({placeholders}) "
        f"ON DUPLICATE KEY UPDATE {updates}"
    )
    try:
        with conn.cursor() as cur:
            cur.execute(DDL)
            cur.executemany(sql, [[row.get(c) for c in cols] for row in rows])
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main() -> int:
    ap = argparse.ArgumentParser(description="采集 Galgame 资料并生成/写入 tb_galgame")
    ap.add_argument("--refresh", action="store_true", help="忽略 7 天缓存，重新请求来源")
    ap.add_argument("--apply", action="store_true", help="写入 MySQL（默认只生成 JSON/SQL）")
    ap.add_argument("--no-covers", action="store_true", help="不下载封面，仅保留 VNDB 远程地址")
    ap.add_argument("--limit", type=int, default=0, help="仅处理前 N 部，调试用")
    ap.add_argument("--mysql-user", default="")
    ap.add_argument("--mysql-pass", default="")
    ap.add_argument("--mysql-db", default="")
    args = ap.parse_args()

    ensure_dirs()
    catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    if args.limit > 0:
        catalog["items"] = catalog["items"][: args.limit]

    session = requests.Session()
    session.headers.update({"User-Agent": UA, "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.6"})

    bangumi = probe_bangumi(session)
    if bangumi["ok"]:
        log("Bangumi API 可用（本轮仍以 VNDB/萌娘百科为主，未写入 Bangumi 评分）。")
    else:
        log(f"Bangumi 不可用，已记录原因：{bangumi['error']}")

    rows, errors = build_rows(catalog, session, args.refresh, not args.no_covers)

    missing = {
        "cover": [r["slug"] for r in rows if not r["cover"]],
        "developer": [r["slug"] for r in rows if not r["developer"]],
        "release_date": [r["slug"] for r in rows if not r["release_date"]],
        "rating": [r["slug"] for r in rows if r["rating"] is None],
        "moegirl": [r["slug"] for r in rows if not r["moegirl_url"]],
    }
    report = {
        "generated_at": now_iso(),
        "catalog_version": catalog.get("version"),
        "requested": len(catalog["items"]),
        "collected": len(rows),
        "sources": {
            "vndb": {"ok": len(rows), "url": "https://vndb.org/"},
            "moegirl": {"ok": len(rows) - len(missing["moegirl"]), "url": "https://zh.moegirl.org.cn/"},
            "bangumi": bangumi,
            "steam": {"ok": sum(1 for r in rows if r["steam_url"])},
        },
        "missing_fields": missing,
        "errors": errors,
        "limitations": [
            "Bangumi：api.bgm.tv / bgm.tv 在当前网络连接超时，未使用其条目与评分。",
            "Key、07th Expansion、AQUAPLUS 等日文官网与 Fandom 专题 Wiki 在当前网络连接超时，未作为来源；可访问的 Steam 商店页用于核对。",
            "露西 -她所期望的一切- 的萌娘百科页面摘要为空，简介依据 VNDB 与公开商店页整理。",
            "ATRI 的发行日期在 VNDB/Steam 为 2020-06-18，萌娘百科记为 2020-06-19，已在 release_note 保留差异。",
        ],
        "note": "评分统一为 VNDB 十分制，并保留评分人数；Bangumi 未接入时不展示 Bangumi 评分。",
    }
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    SEED_JSON_PATH.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_sql(rows)

    log("")
    log(f"已采集 {len(rows)}/{len(catalog['items'])} 部作品。")
    for key, value in missing.items():
        if value:
            log(f"缺失字段 {key}: {', '.join(value)}")
    log(f"数据: {SEED_JSON_PATH}")
    log(f"SQL : {SEED_SQL_PATH}")
    log(f"报告: {REPORT_PATH}")

    if args.apply:
        db = load_db_defaults()
        if args.mysql_user:
            db["user"] = args.mysql_user
        if args.mysql_pass:
            db["password"] = args.mysql_pass
        if args.mysql_db:
            db["database"] = args.mysql_db
        apply_mysql(rows, db)
        log(f"已写入 MySQL {db['host']}:{db['port']}/{db['database']} 的 tb_galgame。")

    return 0 if rows else 1


if __name__ == "__main__":
    sys.exit(main())
