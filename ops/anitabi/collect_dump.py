"""Build the collector export from anitabi's public static data files.

The open API (api.anitabi.cn) blocks this server's egress, but the site itself
serves its map dataset from https://www.anitabi.cn/d/ (g.json + g0..gN.json):
 - g.json: [works, shardSize, updatedAt]; work = [id, cn, ?, title, city?, color, cover, zoom, type, 0,0,0, [pid,lat,lng,priority...]]
 - gN.json: shards of points; entry = [workId, theme, [[pid, name, 0, flag, mapId, 0, imagePath, ...], ...], ts]

This turns that dump into the same CSV layout crawler.py produces, so
import_to_pg.py can consume it unchanged.

    python collect_dump.py --cache data/site-dump --out data/dump-export
    python collect_dump.py --ids ids.txt --out data/dump-export
"""
import argparse
import csv
import json
import os
import time
import urllib.request
from pathlib import Path

BASE = 'https://www.anitabi.cn/d/'
IMAGE_BASE = 'https://image.anitabi.cn'
UA = 'Mozilla/5.0 (compatible; 0721acgn-collector)'


def fetch(url, timeout=120):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read()


def download_dataset(cache, refresh=False, max_age=6 * 3600):
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / 'g.json'
    if refresh or not path.is_file() or time.time() - path.stat().st_mtime > max_age:
        path.write_bytes(fetch(f'{BASE}g.json?d=0721'))
        print(f'downloaded {path.name} ({path.stat().st_size} bytes)', flush=True)


def load(cache):
    g = json.loads((cache / 'g.json').read_text(encoding='utf-8'))
    works_raw, shard_size = g[0], g[1]
    works = {}
    for w in works_raw:
        if not (isinstance(w, list) and w):
            continue
        item = {
            'id': w[0],
            'cn': w[1] if len(w) > 1 and isinstance(w[1], str) else '',
            'title': w[3] if len(w) > 3 and isinstance(w[3], str) else '',
            'city': w[4] if len(w) > 4 and isinstance(w[4], str) else '',
            'color': w[5] if len(w) > 5 and isinstance(w[5], str) else '',
            'cover': w[6] if len(w) > 6 and isinstance(w[6], str) else '',
            'zoom': w[7] if len(w) > 7 else None,
            'type': w[8] if len(w) > 8 and isinstance(w[8], str) else '',
            'points': w[12] if len(w) > 12 and isinstance(w[12], list) else [],
        }
        works[item['id']] = item

    points = {}
    index = 0
    while True:
        path = cache / f'g{index}.json'
        if not path.is_file():
            try:
                path.write_bytes(fetch(f'{BASE}g{index}.json?d=0721'))
                print(f'downloaded {path.name} ({path.stat().st_size} bytes)', flush=True)
            except Exception:
                break
        try:
            rows = json.loads(path.read_text(encoding='utf-8'))
        except Exception:
            break
        for row in rows:
            if isinstance(row, list) and row and isinstance(row[0], int):
                points.setdefault(row[0], []).extend(row[2] if len(row) > 2 and isinstance(row[2], list) else [])
        index += 1
    return works, points, shard_size


def image_url(path):
    if not isinstance(path, str) or not path:
        return ''
    if path.startswith('http'):
        return path
    if path.startswith('/images/'):
        return IMAGE_BASE + path[len('/images'):]
    return ''


def resized(url):
    if not url:
        return ''
    if 'bgm-api.anitabi.cn' in url:
        return url
    return url.split('?')[0] + '?plan=h360'


def origin_info(map_id):
    if not isinstance(map_id, str) or not map_id:
        return '', ''
    return 'Google Maps', f'https://www.google.com/maps/d/viewer?mid={map_id}'


def build(works, points, subject_filter, min_points=1, limit=0):
    rows_work, rows_point = [], []
    for work_id, work in works.items():
        if subject_filter and work_id not in subject_filter:
            continue
        shard = points.get(work_id, [])
        geo_by_id = {}
        flat = work['points']
        for i in range(0, len(flat) - 3, 4):
            pid, lat, lng = flat[i], flat[i + 1], flat[i + 2]
            if isinstance(pid, str) and isinstance(lat, (int, float)) and isinstance(lng, (int, float)):
                geo_by_id[pid] = (lat, lng)
        points_out = []
        for item in shard:
            if not isinstance(item, list) or len(item) < 7:
                continue
            pid = item[0]
            geo = geo_by_id.get(pid)
            if not geo:
                continue
            origin, origin_link = origin_info(item[4])
            image = resized(image_url(item[6]))
            points_out.append(dict(
                xunliid=work_id,
                xunliname=item[1] or '',
                xunlipic=image,
                gaoqingpic=image,
                ep='',
                time='',
                geo=list(geo),
                point_id=pid,
                origin=origin,
                originURL=origin_link,
            ))
        if len(points_out) < min_points:
            continue
        rows_work.append(dict(
            xunliid=work_id,
            xunliname=work['cn'] or work['title'] or str(work_id),
            japanname=work['title'] or '',
            city=work['city'] or '',
            bcimg=resized(image_url(work['cover'])),
            xunlicolor=work['color'] or '',
            maingeo=points_out[0]['geo'],
            zoom=work['zoom'] if work['zoom'] not in (None, 0) else 8,
            count=len(points_out),
            hengpic='',
        ))
        rows_point.extend(points_out)
        if limit and len(rows_work) >= limit:
            break
    return rows_work, rows_point


def write_export(out, rows_work, rows_point):
    folder = out / f'export-{time.strftime("%Y%m%d-%H%M%S")}'
    folder.mkdir(parents=True, exist_ok=True)
    main_fields = 'xunliid xunliname japanname city bcimg xunlicolor maingeo zoom count hengpic'.split()
    point_fields = 'xunliid xunliname xunlipic gaoqingpic ep time geo point_id origin originURL'.split()
    with (folder / '巡礼番剧.csv').open('w', encoding='utf-8-sig', newline='') as fh:
        writer = csv.DictWriter(fh, main_fields)
        writer.writeheader()
        writer.writerows(rows_work)
    with (folder / '具体点.csv').open('w', encoding='utf-8-sig', newline='') as fh:
        writer = csv.DictWriter(fh, point_fields)
        writer.writeheader()
        writer.writerows(rows_point)
    pointer = out / 'CURRENT.tmp'
    pointer.write_text(folder.name, encoding='utf-8')
    os.replace(pointer, out / 'CURRENT')
    return folder


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--cache', default='data/site-dump')
    p.add_argument('--out', default='data/dump-export')
    p.add_argument('--ids', help='only these subject ids (one per line)')
    p.add_argument('--limit', type=int, default=0, help='cap number of works (0 = all)')
    p.add_argument('--min-points', type=int, default=1)
    p.add_argument('--refresh', action='store_true')
    args = p.parse_args()

    cache, out = Path(args.cache), Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    download_dataset(cache, args.refresh)
    works, points, shard_size = load(cache)
    print(f'works={len(works)} with_points={len(points)} points={sum(len(v) for v in points.values())}', flush=True)

    subject_filter = None
    if args.ids:
        subject_filter = {int(x.split('#')[0].strip()) for x in Path(args.ids).read_text(encoding='utf-8-sig').splitlines()
                          if x.split('#')[0].strip()}

    rows_work, rows_point = build(works, points, subject_filter, args.min_points, args.limit)
    folder = write_export(out, rows_work, rows_point)
    print(f'export: {len(rows_work)} works, {len(rows_point)} points -> {folder}', flush=True)


if __name__ == '__main__':
    main()
