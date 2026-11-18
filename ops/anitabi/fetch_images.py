"""Download anitabi images into a batch folder and upload ONE zip to OSS.

Cross-border PUTs of 50k small objects from a GitHub runner are far too slow
(~0.6/s), so the runner packs everything into one archive and the server fans it
out into the bucket (server -> OSS is same-region and fast).

Output on OSS:
  xunli-v2/batches/batch-<tag>.zip    images named <sha256(url)><ext>
  xunli-v2/batches/batch-<tag>.json   {url: filename}

    python fetch_images.py --export <dir> --tag <runid>
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
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

UA = 'Mozilla/5.0 (compatible; 0721acgn-image-batch)'
MAP_KEY = 'xunli-v2/image-map-v2.json'


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
    p.add_argument('--tag', default=time.strftime('%Y%m%d-%H%M%S'))
    p.add_argument('--workdir', default='batch')
    p.add_argument('--threads', type=int, default=16)
    p.add_argument('--limit', type=int, default=0)
    args = p.parse_args()

    import oss2

    bucket = oss2.Bucket(
        oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
        os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])

    existing = {}
    try:
        existing = json.loads(bucket.get_object(MAP_KEY).read().decode('utf-8'))
        print(f'existing map: {len(existing)} urls', flush=True)
    except Exception:
        print('no existing map', flush=True)

    urls = sorted(u for u in load_urls(Path(args.export)) if u not in existing)
    if args.limit:
        urls = urls[:args.limit]
    print(f'to fetch: {len(urls)}', flush=True)
    if not urls:
        print('nothing to do', flush=True)
        return 0

    work = Path(args.workdir)
    if work.exists():
        for f in work.iterdir():
            f.unlink()
    work.mkdir(parents=True, exist_ok=True)

    mapping = {}
    stats = {'ok': 0, 'fail': 0}
    errors = []
    lock = threading.Lock()

    def one(url):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': UA})
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read()
            if not body:
                raise ValueError('empty')
            name = hashlib.sha256(url.encode()).hexdigest() + ext_of(body[:16])
            (work / name).write_bytes(body)
            with lock:
                mapping[url] = name
            return True
        except Exception as exc:
            with lock:
                errors.append(f'{url} :: {type(exc).__name__}')
            return False

    started = time.time()
    with ThreadPoolExecutor(max_workers=args.threads) as pool:
        futures = [pool.submit(one, u) for u in urls]
        for i, fut in enumerate(as_completed(futures), 1):
            stats['ok' if fut.result() else 'fail'] += 1
            if i % 2000 == 0:
                print(f'  {i}/{len(futures)} ok={stats["ok"]} fail={stats["fail"]} '
                      f'({i/max(1e-6, time.time()-started):.0f}/s)', flush=True)

    manifest_local = work / 'manifest.json'
    manifest_local.write_text(json.dumps(mapping, ensure_ascii=False), encoding='utf-8')

    zip_path = Path(f'batch-{args.tag}.zip')
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_STORED, allowZip64=True) as zf:
        for name in mapping.values():
            zf.write(work / name, arcname=name)
        zf.write(manifest_local, arcname='manifest.json')
    size = zip_path.stat().st_size
    print(f'zip {zip_path.name}: {size/1024/1024:.1f} MiB', flush=True)

    oss2.resumable_upload(bucket, f'xunli-v2/batches/batch-{args.tag}.zip', str(zip_path),
                          multipart_threshold=100 * 1024 * 1024, part_size=20 * 1024 * 1024,
                          num_threads=4)
    bucket.put_object(f'xunli-v2/batches/batch-{args.tag}.json',
                      manifest_local.read_bytes(), headers={'Content-Type': 'application/json'})
    print(f'uploaded batch-{args.tag}.zip', flush=True)

    status = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'tag': args.tag,
              **stats, 'zip_mib': round(size / 1024 / 1024, 1),
              'map_entries': len(existing) + len(mapping)}
    if errors:
        status['errors_sample'] = errors[:5]
    print(json.dumps(status, ensure_ascii=False), flush=True)
    return 0 if stats['fail'] == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
