"""Find the current copymanga API shape: read the comic page, its JS bundles,
then try candidate endpoints for one path_word.
"""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE = os.environ.get('COPY_BASE', 'https://www.2026copy.com').rstrip('/')
PATH_WORD = os.environ.get('PATH_WORD', 'pingchengyuzhaihuiyilu')
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
TOKEN = os.environ.get('COPY_TOKEN', '')


def fetch(url, limit=800000, accept=None):
    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Referer': BASE + '/'})
    if accept:
        req.add_header('Accept', accept)
    if TOKEN:
        req.add_header('Authorization', 'Token ' + TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return {'status': resp.status, 'type': resp.headers.get('Content-Type'),
                    'body': resp.read(limit)}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'base': BASE, 'path_word': PATH_WORD}
    page = fetch(f'{BASE}/comic/{PATH_WORD}')
    if 'error' in page:
        out['page'] = page
    else:
        html = page['body'].decode('utf-8', 'replace')
        out['page'] = {'status': page['status'], 'type': page['type'], 'len': len(html),
                       'title': (html.split('<title>', 1)[1].split('</title>', 1)[0][:80]
                                 if '<title>' in html else '')}
        scripts = re.findall(r'<script[^>]+src=["\']([^"\']+)', html)
        out['scripts'] = scripts[:10]
        api_hits = sorted(set(re.findall(r'/api/v[0-9]+[A-Za-z0-9/_\-{}$.:]*', html)))
        out['api_in_html'] = api_hits[:20]

        found = set()
        for src in scripts[:6]:
            url = src if src.startswith('http') else urllib.parse.urljoin(BASE + '/', src)
            js = fetch(url, limit=1500000)
            if 'error' in js:
                continue
            text = js['body'].decode('utf-8', 'replace')
            found.update(re.findall(r'/api/v[0-9]+/[A-Za-z0-9/_\-${}.]*', text))
            found.update(re.findall(r'api/v[0-9]+/[A-Za-z0-9/_\-${}.]*', text)[:50])
        out['api_in_js'] = sorted(x for x in found if len(x) < 80)[:40]

    candidates = [
        f'/api/v3/comic2/{PATH_WORD}?platform=1',
        f'/api/v3/comic/{PATH_WORD}?platform=1',
        f'/api/v3/comic/{PATH_WORD}',
        f'/api/v3/comic/{PATH_WORD}/chapters?limit=1&offset=0',
        f'/api/v3/comic/{PATH_WORD}/group/default/chapters?limit=1&offset=0',
        f'/api/v3/comic/{PATH_WORD}/group/default/chapters?limit=1&offset=0&platform=1',
        f'/api/v3/comics/{PATH_WORD}?platform=1',
    ]
    results = {}
    for path in candidates:
        url = BASE + path
        resp = fetch(url, limit=20000, accept='application/json')
        item = {'status': resp.get('status'), 'error': resp.get('error')}
        if 'body' in resp:
            text = resp['body'].decode('utf-8', 'replace')
            item['head'] = text[:160].replace('\n', ' ')
            try:
                data = json.loads(text)
                item['code'] = data.get('code') if isinstance(data, dict) else None
                res = data.get('results') if isinstance(data, dict) else None
                if isinstance(res, dict):
                    item['result_keys'] = sorted(res.keys())[:20]
                    item['total'] = res.get('total')
                elif isinstance(res, list):
                    item['list_len'] = len(res)
            except ValueError:
                item['json'] = False
        results[path] = item
    out['candidates'] = results

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-api2.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps(out, ensure_ascii=False, indent=2)[:4000])


if __name__ == '__main__':
    main()
