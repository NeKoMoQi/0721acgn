"""Look for a domain that still exposes the classic copymanga v3 API."""
import json
import os
import re
import ssl
import sys
import time
import urllib.request

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
HOSTS = ['www.copymanga.site', 'copymanga.site', 'www.copymanga.org',
         'www.copymanga.fun', 'www.2026copy.com', 'www.mangacopy.com']
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE


def get(url, accept=None, limit=60000):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    if accept:
        req.add_header('Accept', accept)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=20, context=CTX) as resp:
            body = resp.read(limit).decode('utf-8', 'replace')
            return {'status': resp.status, 'type': resp.headers.get('Content-Type'),
                    'seconds': round(time.monotonic() - started, 2), 'body': body}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'hosts': {}}
    for host in HOSTS:
        row = {}
        root = get(f'https://{host}/')
        row['root'] = {k: v for k, v in root.items() if k != 'body'}
        html = root.get('body', '')
        row['title'] = (html.split('<title>', 1)[1].split('</title>', 1)[0].strip()[:80]
                        if '<title>' in html else '')
        for path in ('/api/v3/search/comic?q=%E6%B5%B7%E8%B4%BC%E7%8E%8B&limit=1&offset=0&platform=1',
                     '/api/v3/comic2/HAIBIANZHIYE?platform=1'):
            r = get('https://' + host + path, accept='application/json', limit=20000)
            item = {k: v for k, v in r.items() if k != 'body'}
            text = r.get('body', '')
            item['head'] = text[:160].replace('\n', ' ')
            try:
                data = json.loads(text)
                item['code'] = data.get('code') if isinstance(data, dict) else None
                res = data.get('results') if isinstance(data, dict) else None
                if isinstance(res, dict):
                    item['total'] = res.get('total')
                    item['keys'] = sorted(res.keys())[:10]
            except ValueError:
                item['json'] = False
            row[path.split('?')[0]] = item
        out['hosts'][host] = row
        print(host, json.dumps(row, ensure_ascii=False)[:300], flush=True)

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-v3.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)


if __name__ == '__main__':
    main()
