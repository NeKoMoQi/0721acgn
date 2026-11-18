"""Upload anitabi images straight to OSS from a machine that can reach them.

Connection-per-call was the bottleneck (50k objects from a GitHub runner took
~0.6/s), so every worker keeps its own oss2 Bucket (= keep-alive session) and we
run 32 of them. Progress is persisted to the OSS map every few thousand objects.

    python push_images.py --export <dir> --threads 32
    python push_images.py --export <dir> --limit 300      # quick rate test
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

UA = 'Mozilla/5.0 (compatible; 0721acgn-image-push)'
MAP_KEY = 'xunli-v2/image-map-v2.json'
FINAL_KEY = 'xunli-v2/image-map-final.json'
SHARD_PREFIX = 'xunli-v2/map-shards/shard'


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
    return {u for u in urls if 'acgn0721.oss-cn-hangzhou.aliyuncs.com' not in u}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--export', required=True)
    p.add_argument('--prefix', default='xunli-v2/images')
    p.add_argument('--threads', type=int, default=32)
    p.add_argument('--limit', type=int, default=0)
    p.add_argument('--shard', default='0/1', help='i/n: only upload urls whose hash %% n == i')
    args = p.parse_args()
    shard_i, shard_n = (int(x) for x in args.shard.split('/'))

    import oss2

    auth = oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET'])
    bucket = oss2.Bucket(auth, os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
    public = os.environ['OSS_PUBLIC_BASE'].rstrip('/')
    local = threading.local()

    def tl_bucket():
        if not hasattr(local, 'b'):
            local.b = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                                  os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
        return local.b

    skip = {}
    for key in (FINAL_KEY, MAP_KEY):
        try:
            skip.update(json.loads(bucket.get_object(key).read().decode('utf-8')))
        except Exception:
            pass
    shard_key = f'{SHARD_PREFIX}-{shard_i}.json'
    existing = {}
    try:
        existing = json.loads(bucket.get_object(shard_key).read().decode('utf-8'))
        print(f'shard map: {len(existing)}', flush=True)
    except Exception:
        print('no shard map yet', flush=True)
    skip.update(existing)
    print(f'skip set: {len(skip)}', flush=True)

    urls = [u for u in load_urls(Path(args.export)) if u not in skip]
    if shard_n > 1:
        urls = [u for u in urls if int(hashlib.sha256(u.encode()).hexdigest()[:8], 16) % shard_n == shard_i]
    urls.sort()
    if args.limit:
        urls = urls[:args.limit]
    print(f'to upload: {len(urls)}', flush=True)
    if not urls:
        return 0

    lock = threading.Lock()
    stats = {'ok': 0, 'fail': 0}
    errors = []
    started = time.time()
    last_save = time.time()

    def one(url):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': UA})
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read()
            if not body:
                raise ValueError('empty')
            name = hashlib.sha256(url.encode()).hexdigest() + ext_of(body[:16])
            tl_bucket().put_object(f'{args.prefix}/{name}', body,
                                   headers={'Content-Type': 'image/' + ('jpeg' if name.endswith('.jpg') else name.rsplit('.', 1)[-1])})
            return url, f'{public}/{args.prefix}/{name}'
        except Exception as exc:
            with lock:
                errors.append(f'{url} :: {type(exc).__name__}')
            return None

    def save_map():
        with lock:
            payload = dict(existing)
        try:
            bucket.put_object(shard_key, json.dumps(payload, ensure_ascii=False).encode('utf-8'),
                              headers={'Content-Type': 'application/json'})
        except Exception as exc:
            print(f'  map save failed: {type(exc).__name__}', flush=True)

    with ThreadPoolExecutor(max_workers=args.threads) as pool:
        futures = [pool.submit(one, u) for u in urls]
        for i, fut in enumerate(as_completed(futures), 1):
            res = fut.result()
            if res:
                existing[res[0]] = res[1]
                stats['ok'] += 1
            else:
                stats['fail'] += 1
            if i % 2000 == 0:
                rate = i / max(1e-6, time.time() - started)
                print(f'  {i}/{len(futures)} ok={stats["ok"]} fail={stats["fail"]} {rate:.0f}/s', flush=True)
            if i % 5000 == 0 or time.time() - last_save > 300:
                last_save = time.time()
                save_map()

    save_map()
    elapsed = round(time.time() - started, 1)
    status = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), **stats,
              'mapped': len(existing), 'seconds': elapsed,
              'rate': round(stats['ok'] / max(1e-6, elapsed), 1)}
    if errors:
        status['errors_sample'] = errors[:5]
    print(json.dumps(status, ensure_ascii=False), flush=True)
    return 0 if stats['fail'] == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
