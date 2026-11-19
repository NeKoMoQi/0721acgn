"""Pick today's works from the rank list (runs in Actions, reads/writes OSS only).

Rules (v1):
- scan from rank 1 down, stop at --top (default 100)
- skip works already mapped (imported) or already in the queue
- dedupe by alias: if the normalized title matches an existing comic, treat as
  "fill" of that comic instead of creating a new one
- at most --limit works (default 5); fewer is fine

Writes copymanga/plan/latest.json + copymanga/plan/<date>.json
"""
import argparse
import json
import os
import re
import sys
import time

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass


def norm(text):
    return re.sub(r'[\s\-_·:：!！?？,，.。()（）\[\]【】]+', '', text or '').lower()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--top', type=int, default=100)
    p.add_argument('--limit', type=int, default=5)
    p.add_argument('--state', default='copymanga/state.json')
    p.add_argument('--rank', default='copymanga/rank/latest.json')
    p.add_argument('--out', default='copymanga/plan/latest.json')
    args = p.parse_args()

    import oss2
    bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                         os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])

    def load(key, default):
        try:
            return json.loads(bucket.get_object(key).read().decode('utf-8'))
        except Exception:
            return default

    state = load(args.state, {})
    rank = load(args.rank, {'items': []})
    mapped = state.get('mapped') or {}
    queued = state.get('queued') or {}
    titles = {t['norm']: t for t in (state.get('titles') or []) if t.get('norm')}

    updates = []
    for q in (state.get('queue') or []):
        pw = q.get('path_word')
        if not pw or q.get('state') == 'done':
            continue
        remaining = int(q.get('chapters_total') or 0) - int(q.get('chapters_imported') or 0)
        # 连载中一直追更；完结但还没补齐的也继续（一次性补齐队列）
        if q.get('status') != '连载中' and remaining <= 0:
            continue
        have = mapped.get(pw, {}).get('have', q.get('chapters_imported') or 0)
        updates.append({'rank': None, 'path_word': pw, 'title': q.get('title') or '',
                        'action': 'update', 'comicid': q.get('comicid'),
                        'start': int(have) + 1})
    for u in updates:
        queued[u['path_word']] = queued.get(u['path_word'], 'update')

    picked = []
    skipped = []
    for item in rank.get('items', [])[:args.top]:
        if len(picked) >= args.limit:
            break
        pw = item.get('path_word') or ''
        title = (item.get('title') or '').strip()
        if not pw:
            continue
        if pw in mapped or pw in queued:
            skipped.append({'rank': item.get('rank'), 'path_word': pw, 'title': title, 'why': 'already'})
            continue
        n = norm(title)
        alias = titles.get(n) if n else None
        action = 'fill' if alias else 'new'
        entry = {
            'rank': item.get('rank'),
            'path_word': pw,
            'title': title,
            'action': action,
            'comicid': alias['comicid'] if alias else None,
            'start': (mapped.get(pw, {}).get('have', 0) + 1) if pw in mapped else 1,
        }
        picked.append(entry)

    items = updates + picked
    plan = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
            'count': len(items), 'items': items, 'updates': len(updates),
            'picked': len(picked), 'skipped': skipped[:20]}
    payload = json.dumps(plan, ensure_ascii=False).encode('utf-8')
    bucket.put_object(args.out, payload, headers={'Content-Type': 'application/json'})
    bucket.put_object(f'copymanga/plan/{time.strftime("%Y%m%d")}.json', payload,
                      headers={'Content-Type': 'application/json'})
    print(json.dumps(plan, ensure_ascii=False, indent=2)[:2500], flush=True)
    print(f'updates {len(updates)} + picked {len(picked)} / limit {args.limit}, skipped {len(skipped)}', flush=True)


if __name__ == '__main__':
    main()
