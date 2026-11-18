"""Copy anitabi images (point thumbnails + covers) into the site's OSS bucket.

Runs anywhere that can reach image.anitabi.cn (GitHub Actions runner, home PC);
the blocked server cannot. Input is the export folder produced by collect_dump.py
(巡礼番剧.csv + 具体点.csv); output is:
  - objects under  OSS_PREFIX/<sha256(url)><ext>
  - a map file    OSS_PREFIX/../image-map.json  ({url: oss_url})
The server then runs rewrite_images.py to swap the remote URLs in PostgreSQL.

    python sync_images.py --export data/dump-export/export-... --threads 8
    python sync_images.py --export ... --dry-run
"""
import argparse
import csv
import hashlib
import json
import os
import sys
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

UA = 'Mozilla/5.0 (compatible; 0721acgn-image-sync)'
MAP_NAME = 'image-map.json'


def load_urls(export_dir: Path):
    urls = set()
    with (export_dir / '具体点.csv').open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            for key in ('xunlipic', 'gaoqingpic'):
                u = (row.get(key) or '').strip()
                if u.startswith('http'):
                    urls.add(u)
    with (export_dir / '巡礼番剧.csv').open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            u = (row.get('bcimg') or '').strip()
            if u.startswith('http'):
                urls.add(u)
    return urls


def ext_of(magic: bytes) -> str:
    if magic.startswith(b'\xff\xd8'):
        return '.jpg'
    if magic.startswith(b'\x89PNG'):
        return '.png'
    if magic[:4] == b'RIFF' and magic[8:12] == b'WEBP':
        return '.webp'
    if magic.startswith(b'GIF8'):
        return '.gif'
    return '.bin'


def fetch(url, timeout=45):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read(), resp.headers.get('Content-Type', '')
    return b'', ''


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--export', required=True, help='export folder (with the two CSVs)')
    p.add_argument('--prefix', default='xunli-v2/images')
    p.add_argument('--threads', type=int, default=8)
    p.add_argument('--limit', type=int, default=0)
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--skip-existing', action='store_true', default=True)
    args = p.parse_args()

    import oss2

    bucket = oss2.Bucket(
        oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
        os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
    public = os.environ['OSS_PUBLIC_BASE'].rstrip('/')
    map_key = args.prefix.rsplit('/', 1)[0] + '/' + MAP_NAME

    mapping = {}
    try:
        text = bucket.get_object(map_key).read().decode('utf-8')
        mapping = json.loads(text)
        print(f'loaded existing map: {len(mapping)} urls', flush=True)
    except Exception as exc:
        print(f'no existing map ({type(exc).__name__}), starting fresh', flush=True)

    urls = sorted(u for u in load_urls(Path(args.export)) if 'acgn0721.oss-cn-hangzhou.aliyuncs.com' not in u)
    todo = [u for u in urls if u not in mapping]
    if args.limit:
        todo = todo[:args.limit]
    print(f'images: total={len(urls)} new={len(todo)}', flush=True)
    if args.dry_run:
        for u in todo[:10]:
            print('  would fetch', u, flush=True)
        print('dry run, nothing uploaded', flush=True)
        return 0

    lock = threading.Lock()
    stats = {'ok': 0, 'skip': 0, 'fail': 0}
    errors = []

    def work(url):
        digest = hashlib.sha256(url.encode()).hexdigest()
        try:
            body, ctype = fetch(url)
            if not body:
                raise ValueError('empty body')
            ext = ext_of(body[:16])
            key = f'{args.prefix}/{digest}{ext}'
            bucket.put_object(key, body, headers={'Content-Type': ctype or 'application/octet-stream'})
            with lock:
                mapping[url] = f'{public}/{key}'
            return True, url
        except Exception as exc:
            with lock:
                errors.append(f'{url} :: {type(exc).__name__}: {exc}')
            return False, url

    started = time.time()
    with ThreadPoolExecutor(max_workers=args.threads) as pool:
        futures = [pool.submit(work, u) for u in todo]
        for i, fut in enumerate(as_completed(futures), 1):
            ok, _ = fut.result()
            stats['ok' if ok else 'fail'] += 1
            if i % 500 == 0 or i == len(futures):
                rate = i / max(1e-6, time.time() - started)
                print(f'  {i}/{len(futures)} ok={stats["ok"]} fail={stats["fail"]} ({rate:.1f}/s)', flush=True)

    if not args.dry_run and mapping:
        bucket.put_object(map_key, json.dumps(mapping, ensure_ascii=False).encode('utf-8'),
                          headers={'Content-Type': 'application/json'})
        print(f'uploaded map {map_key}: {len(mapping)} entries', flush=True)

    status = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), **stats,
              'mapped': len(mapping), 'map': f'{public}/{map_key}'}
    if errors:
        status['errors_sample'] = errors[:10]
    print(json.dumps(status, ensure_ascii=False), flush=True)
    return 0 if stats['fail'] == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
