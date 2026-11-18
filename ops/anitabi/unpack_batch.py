"""Fan an image batch (one zip produced by fetch_images.py) out into OSS objects.

Downloads xunli-v2/batches/batch-<tag>.zip from OSS, extracts it, uploads every
file to xunli-v2/images/<name>, and merges manifest {url: name} into
xunli-v2/image-map.json. Runs on the server (same region as the bucket).

    python unpack_batch.py --tag 20261118-174500
    python unpack_batch.py --tag ... --keep-zip
"""
import argparse
import io
import json
import os
import shutil
import sys
import tempfile
import threading
import time
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

MAP_KEY = 'xunli-v2/image-map.json'
TYPES = {'.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png',
         '.webp': 'image/webp', '.gif': 'image/gif', '.bin': 'application/octet-stream'}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--tag', required=True)
    p.add_argument('--threads', type=int, default=16)
    p.add_argument('--keep-zip', action='store_true')
    args = p.parse_args()

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

    zip_key = f'xunli-v2/batches/batch-{args.tag}.zip'
    manifest_key = f'xunli-v2/batches/batch-{args.tag}.json'
    tmp = Path(tempfile.mkdtemp(prefix='batch-'))
    try:
        zip_local = tmp / 'batch.zip'
        print(f'downloading {zip_key}', flush=True)
        bucket.get_object_to_file(zip_key, str(zip_local))
        print(f'{zip_local.stat().st_size/1024/1024:.1f} MiB', flush=True)

        with zipfile.ZipFile(zip_local) as zf:
            zf.extractall(tmp / 'files')
        manifest = json.loads((tmp / 'files' / 'manifest.json').read_text(encoding='utf-8'))
        print(f'manifest: {len(manifest)} urls', flush=True)

        existing = {}
        try:
            existing = json.loads(bucket.get_object(MAP_KEY).read().decode('utf-8'))
        except Exception:
            pass

        def put(item):
            url, name = item
            ext = Path(name).suffix.lower()
            body = (tmp / 'files' / name).read_bytes()
            tl_bucket().put_object(f'xunli-v2/images/{name}', body,
                                   headers={'Content-Type': TYPES.get(ext, 'application/octet-stream')})
            return url, f'{public}/xunli-v2/images/{name}'

        done, failed = 0, 0
        started = time.time()
        with ThreadPoolExecutor(max_workers=args.threads) as pool:
            futures = [pool.submit(put, item) for item in manifest.items()]
            for i, fut in enumerate(as_completed(futures), 1):
                try:
                    url, oss_url = fut.result()
                    existing[url] = oss_url
                    done += 1
                except Exception:
                    failed += 1
                if i % 2000 == 0:
                    print(f'  {i}/{len(futures)} ok={done} fail={failed} '
                          f'({i/max(1e-6, time.time()-started):.0f}/s)', flush=True)

        bucket.put_object(MAP_KEY, json.dumps(existing, ensure_ascii=False).encode('utf-8'),
                          headers={'Content-Type': 'application/json'})
        print(f'image map: {len(existing)} entries', flush=True)
        if not args.keep_zip:
            bucket.delete_object(zip_key)
            bucket.delete_object(manifest_key)
            print('batch objects removed from OSS', flush=True)
        print(json.dumps({'tag': args.tag, 'ok': done, 'fail': failed, 'map': len(existing)}, ensure_ascii=False), flush=True)
        return 0 if failed == 0 else 1
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    sys.exit(main())
