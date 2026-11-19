"""Backfill points that the first import skipped because of coordinate-only dedupe.

The first pass deduped by lat/lng rounded to 4 decimals, which merged different
scenes shot at the same location. This pass matches on (lat4, lng4, name) and
inserts only the genuinely missing points of already-imported works.

    python backfill_points.py --data data/dump-export --dry-run
    python backfill_points.py --data data/dump-export
"""
import argparse
import csv
import os
import sys
from pathlib import Path

import psycopg2

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

SCHEMA = os.environ.get('ANITABI_SCHEMA', 'acgn0721_gis')


def coords(text):
    parts = str(text).strip().strip('[]').split(',')
    return round(float(parts[0]), 4), round(float(parts[1]), 4)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--data', default='data/dump-export')
    p.add_argument('--dry-run', action='store_true')
    args = p.parse_args()

    base = Path(args.data) / Path(args.data, 'CURRENT').read_text(encoding='utf-8-sig').strip()
    rows = list(csv.DictReader((base / '具体点.csv').open(encoding='utf-8-sig')))
    by_work = {}
    for r in rows:
        by_work.setdefault(int(r['xunliid']), []).append(r)

    conn = psycopg2.connect(host=os.environ.get('PGHOST', '127.0.0.1'),
                            port=int(os.environ.get('PGPORT', '5432')),
                            dbname=os.environ.get('PGDATABASE', 'postgres'),
                            user=os.environ.get('PGUSER', 'postgres'),
                            password=os.environ.get('PGPASSWORD', ''))
    cur = conn.cursor()
    cur.execute(f'SELECT bangumi_id, xunliid FROM {SCHEMA}.tb_anitabi_map')
    mapped = {b: x for b, x in cur.fetchall()}
    cur.execute(f'SELECT COALESCE(MAX(id), 0) FROM {SCHEMA}.tb_xunlipoint')
    next_id = cur.fetchone()[0]

    inserted = 0
    for bid, xid in mapped.items():
        cur.execute(f"""SELECT round(ST_Y(geopoint)::numeric,4), round(ST_X(geopoint)::numeric,4), TRIM(xunliname)
                        FROM {SCHEMA}.tb_xunlipoint WHERE xunliid = %s""", (xid,))
        have = {(float(y), float(x), n or '') for y, x, n in cur.fetchall()}
        for r in by_work.get(bid, []):
            lat, lng = coords(r['geo'])
            name = (r['xunliname'] or '').strip()
            if (lat, lng, name) in have:
                continue
            next_id += 1
            cur.execute(
                f"""INSERT INTO {SCHEMA}.tb_xunlipoint
                    (id, xunliid, xunliname, xunlipic, gaoqingpic, ep, time, geopoint)
                    VALUES (%s,%s,%s,%s,%s,%s,%s, ST_SetSRID(ST_MakePoint(%s,%s),4326))""",
                (next_id, xid, name[:254], r.get('xunlipic') or None, r.get('gaoqingpic') or None,
                 None, None, lng, lat))
            cur.execute(
                f"""INSERT INTO {SCHEMA}.tb_anitabi_point_map(bangumi_id, point_id, point_row_id)
                    VALUES (%s,%s,%s)
                    ON CONFLICT (bangumi_id, point_id) DO UPDATE SET point_row_id = EXCLUDED.point_row_id""",
                (bid, r['point_id'], next_id))
            have.add((lat, lng, name))
            inserted += 1
        if inserted:
            cur.execute(f'SELECT count(*) FROM {SCHEMA}.tb_xunlipoint WHERE xunliid = %s', (xid,))
            cur.execute(f'UPDATE {SCHEMA}.tb_xunli SET count = %s WHERE xunliid = %s',
                        (str(cur.fetchone()[0]), xid))
        if inserted % 200 == 0 and inserted:
            print(f'  inserted {inserted}', flush=True)

    print(f'backfilled {inserted} points', flush=True)
    if args.dry_run:
        conn.rollback()
        print('dry run, rolled back')
    else:
        conn.commit()
        print('committed')
    conn.close()
    return 0


if __name__ == '__main__':
    sys.exit(main())
