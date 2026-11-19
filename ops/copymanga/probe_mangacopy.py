"""Probe https://www.mangacopy.com specifically (user-supplied domain)."""
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

BASE = 'https://www.mangacopy.com'
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')


def get(url, accept=None, limit=900000):
    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Referer': BASE + '/'})
    if accept:
        req.add_header('Accept', accept)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return {'status': resp.status, 'type': resp.headers.get('Content-Type'),
                    'seconds': round(time.monotonic() - started, 2),
                    'text': resp.read(limit).decode('utf-8', 'replace')}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'base': BASE}
    root = get(BASE + '/')
    out['root'] = {k: v for k, v in root.items() if k != 'text'}
    html = root.get('text', '')
    out['title'] = (html.split('<title>', 1)[1].split('</title>', 1)[0].strip()[:100]
                    if '<title>' in html else '')
    links = sorted(set(re.findall(r'/comic/([A-Za-z0-9_-]{3,40})', html)))
    out['comic_links'] = links[:8]
    out['api_in_html'] = sorted(set(re.findall(r'/api/v\d/[A-Za-z0-9/_{}$.\-]{2,60}', html)))[:15]

    search = get(BASE + '/api/v3/search/comic?q=%E6%B5%B7%E8%B4%BC%E7%8E%8B&limit=1&offset=0&platform=1',
                 accept='application/json', limit=20000)
    out['search'] = {k: v for k, v in search.items() if k != 'text'}
    out['search_body'] = (search.get('text') or '')[:300]

    path_word = links[0] if links else os.environ.get('PATH_WORD', '')
    out['path_word'] = path_word
    if path_word:
        for path in (f'/api/v3/comic2/{path_word}?platform=1',
                     f'/api/v3/comic/{path_word}?platform=1',
                     f'/api/v3/comic/{path_word}/group/default/chapters?limit=1&offset=0'):
            resp = get(BASE + path, accept='application/json', limit=30000)
            item = {k: v for k, v in resp.items() if k != 'text'}
            text = resp.get('text') or ''
            item['head'] = text[:200].replace('\n', ' ')
            try:
                data = json.loads(text)
                item['code'] = data.get('code') if isinstance(data, dict) else None
                res = data.get('results') if isinstance(data, dict) else None
                if isinstance(res, dict):
                    item['keys'] = sorted(res.keys())[:15]
                    item['total'] = res.get('total')
                elif isinstance(res, list):
                    item['len'] = len(res)
            except ValueError:
                item['json'] = False
            out.setdefault('api', {})[path] = item

    # 章节页：找图片是怎么加载的
    chapter = None
    comic_page = get(f'{BASE}/comic/{path_word}') if path_word else {}
    chtml = comic_page.get('text', '') if isinstance(comic_page, dict) else ''
    ch_links = sorted(set(re.findall(r'/comic/' + re.escape(path_word or '') + r'/chapter/[A-Za-z0-9-]+', chtml)))
    out['chapter_links'] = ch_links[:3]
    if ch_links:
        ch = get(BASE + ch_links[0])
        out['chapter_page'] = {k: v for k, v in ch.items() if k != 'text'}
        text = ch.get('text', '')
        out['chapter_scripts'] = re.findall(r'<script[^>]+src=["\']([^"\']+)', text)[:10]
        inline = re.findall(r'<script(?![^>]*src=)[^>]*>([\s\S]{0,4000}?)</script>', text)
        out['chapter_inline'] = [x.strip()[:600] for x in inline if re.search(r'api|ajax|fetch|image|img|chapter', x, re.I)][:5]
        out['chapter_imgs'] = sorted(set(re.findall(r'(?:data-src|data-original|src)=["\']([^"\']+\.(?:jpg|jpeg|png|webp)[^"\']*)', text)))[:10]
        out['chapter_api_hits'] = sorted(set(re.findall(r'/api/[A-Za-z0-9/_\-?=&.]{2,70}', text)))[:20]
        out['c3_hits'] = sorted(set(re.findall(r'c3\.[A-Za-z0-9.\-]+', text)))[:5]

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-mangacopy.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps(out, ensure_ascii=False, indent=2)[:3000])


if __name__ == '__main__':
    main()
