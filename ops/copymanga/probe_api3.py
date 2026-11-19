"""Inspect the current copymanga comic page structure (HTML scraping fallback)."""
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


def get(url, limit=900000):
    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Referer': BASE + '/'})
    start = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return {'status': resp.status, 'type': resp.headers.get('Content-Type'),
                    'seconds': round(time.monotonic() - start, 2),
                    'text': resp.read(limit).decode('utf-8', 'replace')}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'base': BASE, 'path_word': PATH_WORD}
    page = get(f'{BASE}/comic/{PATH_WORD}')
    out['page'] = {k: v for k, v in page.items() if k != 'text'}
    html = page.get('text', '')
    out['chapter_links'] = sorted(set(re.findall(r'href="([^"]*(?:chapter|/c/)[^"]*)"', html)))[:10]
    out['chapter_links_any'] = sorted(set(re.findall(r'href="([^"]+)"', html)))[:40]
    out['img_attrs'] = sorted(set(re.findall(r'(?:data-src|src)="([^"]+\.(?:jpg|jpeg|png|webp)[^"]*)"', html)))[:10]
    out['forms'] = sorted(set(re.findall(r'<form[^>]*action="([^"]*)"', html)))[:10]
    out['inline_api'] = sorted(set(re.findall(r'[\'"]?(/api/[A-Za-z0-9/_\-?=&.]{2,60})', html)))[:20]

    if out['chapter_links']:
        chapter_url = urllib.parse.urljoin(BASE + '/', out['chapter_links'][0])
        ch = get(chapter_url)
        out['chapter_page'] = {'url': chapter_url, **{k: v for k, v in ch.items() if k != 'text'}}
        chtml = ch.get('text', '')
        imgs = sorted(set(re.findall(r'(?:data-src|src)="([^"]+\.(?:jpg|jpeg|png|webp)[^"]*)"', chtml)))
        out['chapter_imgs'] = imgs[:8]
        out['chapter_img_count'] = len(imgs)
        out['chapter_inline_js'] = sorted(set(re.findall(r'(?:var|let|const)\s+(\w+)\s*=\s*[\'"]([^\'"]{5,80})[\'"]', chtml)))[:10]

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-api3.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps(out, ensure_ascii=False, indent=2)[:4000])


if __name__ == '__main__':
    main()
