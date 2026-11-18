"""Merge sharded image maps (and the legacy partial map) into one final map.

    python merge_maps.py
    python merge_maps.py --dry-run
"""
import argparse
import json
import os
import sys

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

FINAL_KEY = 'xunli-v2/image-map-final.json'
PARTIAL_KEYS = ('xunli-v2/image-map-v2.json', 'xunli-v2/image-map.json')
SHARD_PREFIX = 'xunli-v2/map-shards/'


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--dry-run', action='store_true')
    args = p.parse_args()

    import oss2

    bucket = oss2.Bucket(
        oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
        os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])

    merged = {}
    for key in (FINAL_KEY,) + PARTIAL_KEYS:
        try:
            data = json.loads(bucket.get_object(key).read().decode('utf-8'))
            merged.update(data)
            print(f'{key}: {len(data)}')
        except Exception:
            pass

    shards = 0
    for obj in oss2.ObjectIterator(bucket, prefix=SHARD_PREFIX):
        try:
            data = json.loads(bucket.get_object(obj.key).read().decode('utf-8'))
        except Exception:
            continue
        merged.update(data)
        shards += 1
        print(f'{obj.key}: {len(data)}')

    print(f'shards={shards} merged={len(merged)}', flush=True)
    if args.dry_run:
        return 0
    bucket.put_object(FINAL_KEY, json.dumps(merged, ensure_ascii=False).encode('utf-8'),
                      headers={'Content-Type': 'application/json'})
    print(f'wrote {FINAL_KEY}', flush=True)
    return 0


if __name__ == '__main__':
    sys.exit(main())
