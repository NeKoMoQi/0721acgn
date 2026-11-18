"""Pull the latest anitabi export from the GitHub repo (API, no SSH needed) and
optionally import it into PostgreSQL.

Used by the server side: the GitHub Actions collector commits
runs/anitabi-export/latest/*.csv to the repo every day, this script fetches those
two CSVs through api.github.com (works from the blocked server) and hands them to
import_to_pg.py --export.

    python pull_github_export.py --out data/github-export
    python pull_github_export.py --out data/github-export --import
"""
import argparse
import base64
import json
import os
import subprocess
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = 'https://api.github.com'


def get(url, accept='application/vnd.github+json'):
    req = urllib.request.Request(url, headers={'Accept': accept, 'User-Agent': '0721acgn-collector'})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read(), resp.headers.get('content-type', '')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo', default='NeKoMoQi/0721acgn')
    p.add_argument('--ref', default='main')
    p.add_argument('--out', default='data/github-export')
    p.add_argument('--import', dest='do_import', action='store_true')
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--allow-links', action='store_true', help='import even when the collector ran without OSS')
    args = p.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    listing_url = f'{API}/repos/{args.repo}/contents/runs/anitabi-export/latest?ref={args.ref}'
    try:
        raw, ctype = get(listing_url)
    except Exception as exc:
        print(f'listing failed: {type(exc).__name__}: {exc}', flush=True)
        return 1
    items = json.loads(raw.decode('utf-8'))
    if not isinstance(items, list):
        print(f'unexpected listing: {items!r:.200}', flush=True)
        return 1

    saved = []
    for item in items:
        name = item.get('name', '')
        if not name.endswith('.csv'):
            continue
        url = item.get('git_url') or item.get('url')
        blob, _ = get(url)
        payload = json.loads(blob.decode('utf-8'))
        data = base64.b64decode(payload['content'])
        target = out / name
        target.write_bytes(data)
        saved.append((name, len(data)))
    if not saved:
        print('no CSV found in runs/anitabi-export/latest', flush=True)
        return 1
    for name, size in saved:
        print(f'saved {name} ({size} bytes)', flush=True)

    status = {}
    status_url = f'{API}/repos/{args.repo}/contents/runs/anitabi-status.json?ref={args.ref}'
    try:
        raw, _ = get(status_url)
        status = json.loads(base64.b64decode(json.loads(raw.decode('utf-8'))['content']))
        (out / 'status.json').write_text(json.dumps(status, ensure_ascii=False, indent=2), encoding='utf-8')
        print('collector status:', json.dumps(status, ensure_ascii=False), flush=True)
    except Exception as exc:
        print(f'status unavailable: {type(exc).__name__}', flush=True)

    if args.do_import:
        mode = str(status.get('mode') or '')
        if mode != 'oss' and not args.allow_links:
            print(f'collector mode is "{mode}", not "oss": images are still upstream URLs, '
                  f'skip import (use --allow-links to force)', flush=True)
            return 0
        cmd = [sys.executable, str(Path(__file__).with_name('import_to_pg.py')), '--export', str(out)]
        if args.dry_run:
            cmd.append('--dry-run')
        print('running:', ' '.join(cmd), flush=True)
        return subprocess.call(cmd)
    return 0


if __name__ == '__main__':
    sys.exit(main())
