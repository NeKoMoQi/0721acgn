"""Run today's plan from OSS (Actions side): crawl each planned work in batches."""
import argparse
import json
import os
import subprocess
import sys
import time
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

HERE = Path(__file__).resolve().parent


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--plan', default='copymanga/plan/latest.json')
    p.add_argument('--budget', type=int, default=20, help='chapters per work per day')
    p.add_argument('--time-budget', type=int, default=14000, help='seconds for the whole loop')
    args = p.parse_args()

    import oss2
    bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                         os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
    try:
        plan = json.loads(bucket.get_object(args.plan).read().decode('utf-8'))
    except Exception:
        print(f'no plan at {args.plan}', flush=True)
        return 1

    started = time.time()
    results = []
    for item in plan.get('items', []):
        if time.time() - started > args.time_budget:
            results.append({'path_word': item['path_word'], 'result': 'skipped-time'})
            continue
        cmd = [sys.executable, str(HERE / 'crawl_selenium.py'),
               '--comic', item['path_word'],
               '--start', str(item.get('start') or 1),
               '--max-chapters', str(args.budget)]
        print('RUN', ' '.join(cmd), flush=True)
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=args.time_budget)
        tail = (proc.stdout or '')[-600:] + (proc.stderr or '')[-600:]
        print(tail, flush=True)
        results.append({'rank': item.get('rank'), 'path_word': item['path_word'],
                        'title': item.get('title'), 'action': item.get('action'),
                        'start': item.get('start'), 'exit': proc.returncode})

    summary = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
               'plan_count': len(plan.get('items', [])), 'results': results}
    os.makedirs('runs', exist_ok=True)
    Path('runs/copymanga-daily.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(summary, ensure_ascii=False)[:1500], flush=True)
    return 0


if __name__ == '__main__':
    sys.exit(main())
