"""Probe the copymanga API shape from a GitHub Actions runner.

Confirms: search -> comic2 -> chapter list -> chapter detail -> image download,
and records the fields the MySQL importer will need.
"""
import json
import os
import sys
import time
import urllib.parse
import urllib.request

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE = os.environ.get('COPY_BASE', 'https://www.2026copy.com').rstrip('/')
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
TOKEN = os.environ.get('COPY_TOKEN', '')


def api(path, **params):
    url = f'{BASE}/api/v3/{path}'
    if params:
        url += '?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Accept': 'application/json'})
    if TOKEN:
        req.add_header('Authorization', 'Token ' + TOKEN)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read()
            data = json.loads(body.decode('utf-8'))
            return {'status': resp.status, 'seconds': round(time.monotonic() - started, 2),
                    'code': data.get('code'), 'results': data.get('results')}, data
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}, None


def head(url, limit=2048):
    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Referer': BASE + '/'})
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read(limit)
            return {'status': resp.status, 'type': resp.headers.get('Content-Type'),
                    'bytes': len(body), 'seconds': round(time.monotonic() - started, 2)}
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'base': BASE}
    meta, data = api('search/comic', q='出包王女', limit=1, offset=0)
    out['search'] = meta
    path_word = None
    if data and isinstance(data.get('results'), dict):
        lst = data['results'].get('list') or []
        if lst:
            first = lst[0]
            path_word = first.get('path_word')
            out['search_first'] = {k: first.get(k) for k in
                                   ('name', 'path_word', 'author', 'cover', 'status', 'popular', 'theme')}
    if not path_word:
        print(json.dumps(out, ensure_ascii=False))
        return 1

    meta, info = api(f'comic2/{path_word}', platform=1)
    out['comic2'] = meta
    if info and isinstance(info.get('results'), dict):
        r = info['results']
        out['comic_fields'] = sorted(r.keys())
        out['comic_sample'] = {k: r.get(k) for k in
                               ('name', 'path_word', 'author', 'cover', 'status', 'describe', 'popular')}
        out['groups'] = r.get('groups')
        group = (r.get('groups') or [{}])[0].get('path_word') if isinstance(r.get('groups'), list) else None
    else:
        group = None
    out['group'] = group

    chapter_uuid = None
    if group:
        meta, chapters = api(f'comic/{path_word}/group/{group}/chapters', limit=1, offset=0)
        out['chapters'] = meta
        if chapters and isinstance(chapters.get('results'), dict):
            lst = chapters['results'].get('list') or []
            out['chapters_total'] = chapters['results'].get('total')
            if lst:
                chapter_uuid = lst[0].get('uuid')
                out['chapter_sample'] = {k: lst[0].get(k) for k in ('uuid', 'name', 'index', 'size', 'count')}

    if chapter_uuid:
        meta, chapter = api(f'comic/{path_word}/chapter2/{chapter_uuid}', platform=1)
        out['chapter2'] = meta
        if chapter and isinstance(chapter.get('results'), dict):
            ch = chapter['results'].get('chapter') or {}
            out['chapter_fields'] = sorted(ch.keys())
            contents = ch.get('contents') or []
            words = ch.get('words') or []
            out['pages'] = len(contents)
            out['page_sample'] = contents[0] if contents else None
            if contents:
                out['image'] = head(contents[0].get('url', ''))
    out['ok'] = bool(out.get('comic_fields'))
    print(json.dumps(out, ensure_ascii=False, indent=2)[:3000])

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-api.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    return 0


if __name__ == '__main__':
    sys.exit(main())
