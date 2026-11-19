"""Probe copymanga domains and API from a GitHub Actions runner.

The server is in mainland China and every known copymanga domain is unreachable
there. This checks, from the runner's egress, which domain (if any) still serves
the site and its /api/v3 JSON API.
"""
import json
import sys
import time
import urllib.error
import urllib.request

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
HOSTS = [
    'www.2026copy.com', '2026copy.com', 'www.copymanga.site', 'copymanga.site',
    'www.copymanga.org', 'copymanga.org', 'www.mangacopy.com', 'mangacopy.com',
    'www.copymanga.cc', 'www.copy20.com', 'www.copymanga.tv', 'mangacopy.net',
    'www.copy-manga.com', 'www.copymanga.fun', 'www.copymanga.xyz',
]


def get(url, timeout=15, accept=None):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    if accept:
        req.add_header('Accept', accept)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read(400)
            return {'status': resp.status, 'url': resp.geturl(),
                    'type': resp.headers.get('Content-Type'),
                    'bytes_read': len(body), 'seconds': round(time.monotonic() - started, 2),
                    'head': body[:160].decode('utf-8', 'replace').replace('\n', ' ')}
    except urllib.error.HTTPError as exc:
        body = exc.read(200)
        return {'status': exc.code, 'type': exc.headers.get('Content-Type'),
                'seconds': round(time.monotonic() - started, 2),
                'head': body[:160].decode('utf-8', 'replace').replace('\n', ' ')}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}',
                'seconds': round(time.monotonic() - started, 2)}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'hosts': {}}
    for host in HOSTS:
        row = {'root': get(f'https://{host}/')}
        root_ok = row['root'].get('status') == 200 and 'text/html' in (row['root'].get('type') or '')
        html = ''
        if root_ok:
            try:
                req = urllib.request.Request(f'https://{host}/', headers={'User-Agent': UA})
                with urllib.request.urlopen(req, timeout=15) as resp:
                    html = resp.read(200000).decode('utf-8', 'replace')
            except Exception:
                pass
        title = ''
        if '<title>' in html:
            title = html.split('<title>', 1)[1].split('</title>', 1)[0].strip()[:80]
        row['title'] = title
        row['parked'] = any(k in title.lower() for k in ('parked', 'domain', 'for sale', 'loading'))
        if root_ok and not row['parked']:
            row['api'] = get(f'https://{host}/api/v3/search/comic?q=%E6%B5%B7%E8%B4%BC%E7%8E%8B&limit=1&offset=0',
                             accept='application/json')
        out['hosts'][host] = row
        print(host, json.dumps(row, ensure_ascii=False)[:300], flush=True)

    import os
    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-probe.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps({h: (v['root'].get('status'), v.get('title'), bool(v.get('api'))) for h, v in out['hosts'].items()},
                     ensure_ascii=False))


if __name__ == '__main__':
    main()
