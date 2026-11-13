"""Import the latest Anitabi export into the site's PostgreSQL pilgrimage tables.

Reads <data>/CURRENT -> 巡礼番剧.csv + 具体点.csv (written by crawler.py) and
appends new works / new points into acgn0721_gis.tb_xunli / tb_xunlipoint,
keeping a stable mapping in acgn0721_gis.tb_anitabi_map / tb_anitabi_point_map.

Idempotent: every work is imported once, every point once (keyed by upstream
point id). New rows get the next free integer ids, existing ids are untouched.
Run with --dry-run to execute everything inside a transaction and roll back.
"""
import argparse
import csv
import json
import os
from pathlib import Path
import sys

import psycopg2

SCHEMA = os.environ.get('ANITABI_SCHEMA', 'acgn0721_gis')

DDL = f"""
CREATE TABLE IF NOT EXISTS {SCHEMA}.tb_anitabi_map (
    bangumi_id integer PRIMARY KEY,
    xunliid    integer NOT NULL UNIQUE,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS {SCHEMA}.tb_anitabi_point_map (
    bangumi_id   integer NOT NULL,
    point_id     text    NOT NULL,
    point_row_id integer NOT NULL,
    PRIMARY KEY (bangumi_id, point_id)
);
"""


def point_of(value):
    """Accept '[lat, lng]' (JSON), 'lat,lng', or WKB hex from the legacy CSV."""
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    if text.startswith('['):
        pair = json.loads(text)
    elif text.startswith('0101000020') and len(text) >= 50:
        import binascii
        import struct
        raw = binascii.unhexlify(text)
        x, y = struct.unpack('<dd', raw[9:25])
        return float(y), float(x)  # hex WKB stores x=lng, y=lat
    else:
        parts = text.replace('(', ' ').replace(')', ' ').replace(',', ' ').split()
        pair = [float(p) for p in parts[-2:]]
    if len(pair) != 2:
        raise ValueError(f'bad coordinate: {value!r}')
    lat, lng = float(pair[0]), float(pair[1])
    if not (-90 <= lat <= 90 and -180 <= lng <= 180):
        raise ValueError(f'coordinate out of range: {value!r}')
    return lat, lng


def read_export(data_dir):
    pointer = Path(data_dir) / 'CURRENT'
    if not pointer.is_file():
        return [], {}
    folder = Path(data_dir) / pointer.read_text(encoding='utf-8-sig').strip()
    main_csv = folder / '巡礼番剧.csv'
    point_csv = folder / '具体点.csv'
    if not main_csv.is_file() or not point_csv.is_file():
        raise SystemExit(f'export incomplete under {folder}')

    works, points = [], {}
    with main_csv.open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            if not row.get('xunliid'):
                continue
            works.append(row)
    with point_csv.open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            if not row.get('xunliid'):
                continue
            points.setdefault(int(row['xunliid']), []).append(row)
    return works, points


def value(row, key):
    text = (row.get(key) or '').strip()
    return text or None


def clip(text, limit=254):
    return text[:limit] if text else text


def import_all(conn, works, points_by_work, apply):
    cur = conn.cursor()
    # Serialise imports; the transaction-level lock is released on commit/rollback.
    cur.execute("SELECT pg_advisory_xact_lock(hashtext('0721acgn_anitabi_import'))")
    cur.execute(DDL)
    cur.execute(f'SELECT COALESCE(MAX(id), 0), COALESCE(MAX(xunliid), 0) FROM {SCHEMA}.tb_xunli')
    next_work_id, next_xunliid = cur.fetchone()
    cur.execute(f'SELECT COALESCE(MAX(id), 0) FROM {SCHEMA}.tb_xunlipoint')
    next_point_id = cur.fetchone()[0]

    cur.execute(f'SELECT bangumi_id, xunliid FROM {SCHEMA}.tb_anitabi_map')
    mapped = dict(cur.fetchall())
    cur.execute(f'SELECT bangumi_id, point_id FROM {SCHEMA}.tb_anitabi_point_map')
    known_points = set(cur.fetchall())

    stats = dict(works_new=0, works_matched=0, works_updated=0, works_skipped=0,
                 points_new=0, points_skipped=0, failed=0)
    point_index = {}

    def existing_points(xunliid):
        """(name, lat, lng) -> row id, for legacy points imported before the mapping existed."""
        if xunliid not in point_index:
            cur.execute(
                f"""SELECT id, TRIM(xunliname), round(ST_Y(geopoint)::numeric, 5),
                           round(ST_X(geopoint)::numeric, 5)
                    FROM {SCHEMA}.tb_xunlipoint WHERE xunliid = %s""", (xunliid,))
            point_index[xunliid] = {(n, float(y), float(x)): pid for pid, n, y, x in cur.fetchall()}
        return point_index[xunliid]

    for row in works:
        bangumi_id = int(row['xunliid'])
        name = value(row, 'xunliname') or value(row, 'japanname') or f'anitabi-{bangumi_id}'
        jp_name = value(row, 'japanname')
        rows = points_by_work.get(bangumi_id, [])
        if not rows:
            print(f'[skip] {bangumi_id} {name}: no points with images', flush=True)
            stats['works_skipped'] += 1
            continue

        sp = f'work_{bangumi_id}'
        cur.execute(f'SAVEPOINT {sp}')
        try:
            if bangumi_id in mapped:
                xunliid = mapped[bangumi_id]
                cur.execute(f'UPDATE {SCHEMA}.tb_anitabi_map SET updated_at = now() WHERE bangumi_id = %s',
                            (bangumi_id,))
                stats['works_updated'] += 1
            else:
                # The site already holds works collected before this mapping existed:
                # adopt them by exact name instead of creating duplicates.
                cur.execute(
                    f"""SELECT DISTINCT xunliid FROM {SCHEMA}.tb_xunli
                        WHERE TRIM(xunliname) = %s OR (%s IS NOT NULL AND TRIM(japanname) = %s)""",
                    (name, jp_name, jp_name))
                candidates = [r[0] for r in cur.fetchall()]
                if len(candidates) == 1:
                    xunliid = candidates[0]
                    cur.execute(f'INSERT INTO {SCHEMA}.tb_anitabi_map(bangumi_id, xunliid) VALUES (%s,%s)',
                                (bangumi_id, xunliid))
                    mapped[bangumi_id] = xunliid
                    stats['works_matched'] += 1
                    print(f'[adopt] {bangumi_id} {name} -> existing xunliid={xunliid}', flush=True)
                elif len(candidates) > 1:
                    cur.execute(f'ROLLBACK TO SAVEPOINT {sp}')
                    cur.execute(f'RELEASE SAVEPOINT {sp}')
                    print(f'[skip] {bangumi_id} {name}: ambiguous, matches xunliid={candidates}', flush=True)
                    stats['works_skipped'] += 1
                    continue
                else:
                    next_work_id += 1
                    next_xunliid += 1
                    xunliid = next_xunliid
                    lat, lng = point_of(row.get('maingeo')) or (None, None)
                    cur.execute(
                        f"""INSERT INTO {SCHEMA}.tb_xunli
                            (id, xunliname, japanname, city, bcimg, xunlicolor, maingeo, zoom, count, hengpic, xunliid)
                            VALUES (%s,%s,%s,%s,%s,%s,
                                    CASE WHEN %s IS NULL THEN NULL ELSE ST_SetSRID(ST_MakePoint(%s,%s),4326) END,
                                    %s,%s,%s,%s)""",
                        (next_work_id, clip(name), clip(jp_name), clip(value(row, 'city')),
                         value(row, 'bcimg'), clip(value(row, 'xunlicolor')),
                         lat, lng, lat, zoom_of(row), str(len(rows)), value(row, 'hengpic'), xunliid))
                    cur.execute(f'INSERT INTO {SCHEMA}.tb_anitabi_map(bangumi_id, xunliid) VALUES (%s,%s)',
                                (bangumi_id, xunliid))
                    mapped[bangumi_id] = xunliid
                    stats['works_new'] += 1

            legacy = existing_points(xunliid)
            for point in rows:
                pid = value(point, 'point_id')
                lat, lng = point_of(point.get('geo')) or (None, None)
                if not pid:
                    pid = f"anon-{point.get('xunliname')}-{point.get('geo')}"
                if (bangumi_id, pid) in known_points:
                    stats['points_skipped'] += 1
                    continue
                if lat is None:
                    stats['points_skipped'] += 1
                    continue
                key = (clip(value(point, 'xunliname'), 254), round(lat, 5), round(lng, 5))
                if key in legacy:
                    cur.execute(
                        f'INSERT INTO {SCHEMA}.tb_anitabi_point_map(bangumi_id, point_id, point_row_id) VALUES (%s,%s,%s)',
                        (bangumi_id, pid, legacy[key]))
                    known_points.add((bangumi_id, pid))
                    stats['points_skipped'] += 1
                    continue
                next_point_id += 1
                cur.execute(
                    f"""INSERT INTO {SCHEMA}.tb_xunlipoint
                        (id, xunliid, xunliname, xunlipic, gaoqingpic, ep, time, geopoint)
                        VALUES (%s,%s,%s,%s,%s,%s,%s, ST_SetSRID(ST_MakePoint(%s,%s),4326))""",
                    (next_point_id, xunliid, key[0],
                     value(point, 'xunlipic'), value(point, 'gaoqingpic'),
                     value(point, 'ep'), value(point, 'time'), lng, lat))
                cur.execute(
                    f'INSERT INTO {SCHEMA}.tb_anitabi_point_map(bangumi_id, point_id, point_row_id) VALUES (%s,%s,%s)',
                    (bangumi_id, pid, next_point_id))
                known_points.add((bangumi_id, pid))
                legacy[key] = next_point_id
                stats['points_new'] += 1

            # count mirrors the rows the map can actually show for this work.
            cur.execute(f'SELECT count(*) FROM {SCHEMA}.tb_xunlipoint WHERE xunliid = %s', (xunliid,))
            total = cur.fetchone()[0]
            cur.execute(f'UPDATE {SCHEMA}.tb_xunli SET count = %s WHERE xunliid = %s', (str(total), xunliid))
            cur.execute(f'RELEASE SAVEPOINT {sp}')
        except Exception as exc:  # one bad work must not poison the whole batch
            cur.execute(f'ROLLBACK TO SAVEPOINT {sp}')
            cur.execute(f'RELEASE SAVEPOINT {sp}')
            stats['failed'] += 1
            print(f'[fail] {bangumi_id} {name}: {type(exc).__name__}: {exc}', flush=True)

    if apply:
        conn.commit()
    else:
        conn.rollback()
    return stats



def zoom_of(row):
    try:
        return int(round(float(row.get('zoom') or 0))) or None
    except (TypeError, ValueError):
        return None


def connect(args):
    return psycopg2.connect(host=args.host, port=args.port, dbname=args.db,
                            user=args.user, password=args.password, connect_timeout=15)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--data', default='data')
    p.add_argument('--host', default=os.environ.get('PGHOST', '127.0.0.1'))
    p.add_argument('--port', type=int, default=int(os.environ.get('PGPORT', '5432')))
    p.add_argument('--db', default=os.environ.get('PGDATABASE', 'postgres'))
    p.add_argument('--user', default=os.environ.get('PGUSER', 'postgres'))
    p.add_argument('--password', default=os.environ.get('PGPASSWORD', ''))
    p.add_argument('--dry-run', action='store_true', help='do everything, then roll back')
    args = p.parse_args()

    works, points_by_work = read_export(args.data)
    print(f'export: {len(works)} works, {sum(len(v) for v in points_by_work.values())} points', flush=True)
    if not works:
        print('nothing to import', flush=True)
        return 0

    conn = connect(args)
    try:
        stats = import_all(conn, works, points_by_work, apply=not args.dry_run)
    finally:
        conn.close()
    print(json.dumps(stats, ensure_ascii=False), flush=True)
    print('DRY RUN: rolled back, database unchanged' if args.dry_run else 'committed', flush=True)
    return 1 if stats['failed'] else 0


if __name__ == '__main__':
    sys.exit(main())
