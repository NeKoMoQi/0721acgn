"""Swap remote anitabi image URLs in PostgreSQL for their OSS copies.

Reads the image map produced by sync_images.py (stored in OSS) and rewrites
tb_xunlipoint.xunlipic/gaoqingpic and tb_xunli.bcimg when a copy exists.
Idempotent: already-OSS URLs and unmapped URLs are left alone.

    python rewrite_images.py            # apply
    python rewrite_images.py --dry-run
"""
import argparse
import json
import os
import sys
import urllib.request

import psycopg2

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

SCHEMA = os.environ.get('ANITABI_SCHEMA', 'acgn0721_gis')
DEFAULT_MAP = 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/xunli-v2/image-map-v2.json'


def load_map(source):
    if source.startswith('http'):
        req = urllib.request.Request(source, headers={'User-Agent': '0721acgn-rewrite'})
        with urllib.request.urlopen(req, timeout=120) as resp:
            return json.loads(resp.read().decode('utf-8'))
    with open(source, encoding='utf-8') as fh:
        return json.load(fh)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--map', default=DEFAULT_MAP)
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--host', default=os.environ.get('PGHOST', '127.0.0.1'))
    p.add_argument('--port', type=int, default=int(os.environ.get('PGPORT', '5432')))
    p.add_argument('--db', default=os.environ.get('PGDATABASE', 'postgres'))
    p.add_argument('--user', default=os.environ.get('PGUSER', 'postgres'))
    p.add_argument('--password', default=os.environ.get('PGPASSWORD', ''))
    args = p.parse_args()

    try:
        mapping = load_map(args.map)
    except Exception as exc:
        print(f'image map unavailable ({type(exc).__name__}): {exc}', flush=True)
        return 0
    print(f'image map: {len(mapping)} entries', flush=True)

    conn = psycopg2.connect(host=args.host, port=args.port, dbname=args.db,
                            user=args.user, password=args.password, connect_timeout=15)
    try:
        cur = conn.cursor()
        cur.execute(f"""SELECT id, xunlipic, gaoqingpic FROM {SCHEMA}.tb_xunlipoint
                        WHERE (xunlipic LIKE 'http%' OR gaoqingpic LIKE 'http%')
                          AND COALESCE(xunlipic,'') NOT LIKE '%acgn0721.oss-cn-hangzhou.aliyuncs.com%'""")
        point_updates = []
        for row_id, pic, hi in cur.fetchall():
            new_pic = mapping.get(pic, pic)
            new_hi = mapping.get(hi, hi)
            if new_pic != pic or new_hi != hi:
                point_updates.append((new_pic, new_hi, row_id))

        cur.execute(f"""SELECT xunliid, bcimg FROM {SCHEMA}.tb_xunli
                        WHERE bcimg LIKE 'http%'
                          AND bcimg NOT LIKE '%acgn0721.oss-cn-hangzhou.aliyuncs.com%'""")
        work_updates = []
        for xunliid, cover in cur.fetchall():
            new_cover = mapping.get(cover, cover)
            if new_cover != cover:
                work_updates.append((new_cover, xunliid))

        print(f'points to rewrite: {len(point_updates)}, covers to rewrite: {len(work_updates)}', flush=True)
        if args.dry_run:
            for item in (point_updates[:3] + work_updates[:3]):
                print('  sample', item, flush=True)
            print('dry run, database unchanged', flush=True)
            return 0

        for start in range(0, len(point_updates), 1000):
            chunk = point_updates[start:start + 1000]
            cur.executemany(
                f'UPDATE {SCHEMA}.tb_xunlipoint SET xunlipic = %s, gaoqingpic = %s WHERE id = %s', chunk)
            conn.commit()
            print(f'  points {min(start + 1000, len(point_updates))}/{len(point_updates)}', flush=True)
        for start in range(0, len(work_updates), 1000):
            chunk = work_updates[start:start + 1000]
            cur.executemany(f'UPDATE {SCHEMA}.tb_xunli SET bcimg = %s WHERE xunliid = %s', chunk)
            conn.commit()
        print('rewrite done', flush=True)
        return 0
    finally:
        conn.close()


if __name__ == '__main__':
    sys.exit(main())
