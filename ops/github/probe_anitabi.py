"""Probe anitabi from a GitHub Actions runner.

Checks whether api.anitabi.cn / image.anitabi.cn allow this egress, then (when
they do) runs the real collector for a couple of subjects in local mode to prove
the full path. Result goes to runs/anitabi-probe.json, which the workflow commits
back so the server can read it without any token.
"""
import json
import os
import subprocess
import sys
import time
from pathlib import Path

import requests

SUBJECT = 188412
UA = 'AnitabiIncrementalCollector/1.0'
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'runs' / 'anitabi-probe.json'


def probe(url, session, stream=False):
    started = time.monotonic()
    try:
        r = session.get(url, timeout=(10, 60), stream=stream, allow_redirects=False)
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}
    info = {
        'status': r.status_code,
        'type': r.headers.get('content-type'),
        'cf_ray': r.headers.get('cf-ray'),
        'server': r.headers.get('server'),
        'seconds': round(time.monotonic() - started, 2),
    }
    if r.ok and stream:
        n, magic = 0, b''
        for chunk in r.iter_content(65536):
            if not magic:
                magic = chunk[:8]
            n += len(chunk)
            if n > 4 * 1024 * 1024:
                break
        info['bytes'] = n
        info['image_magic'] = magic.hex()
    elif r.ok:
        info['bytes'] = len(r.content)
        try:
            info['json_keys'] = sorted(r.json().keys())[:12] if isinstance(r.json(), dict) else 'list'
        except ValueError:
            info['note'] = '200 but not JSON'
    else:
        info['body'] = r.text[:120].replace('\n', ' ')
    r.close()
    return info


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'subject': SUBJECT}
    try:
        out['egress_ip'] = requests.get('https://api.ipify.org', timeout=15).text.strip()
    except Exception as exc:
        out['egress_ip'] = f'error: {type(exc).__name__}'

    session = requests.Session()
    session.headers['User-Agent'] = UA

    out['lite'] = probe(f'https://api.anitabi.cn/bangumi/{SUBJECT}/lite', session)
    out['points'] = probe(f'https://api.anitabi.cn/bangumi/{SUBJECT}/points/detail?haveImage=true', session)

    lite_ok = out['lite'].get('status') == 200
    if lite_ok:
        data = session.get(f'https://api.anitabi.cn/bangumi/{SUBJECT}/lite', timeout=30).json()
        cover = (data.get('cover') or '').split('?')[0] + '?plan=h360'
        if cover.startswith('http'):
            out['cover'] = probe(cover, session, stream=True)

    points_ok = out['points'].get('status') == 200
    if points_ok:
        rows = session.get(
            f'https://api.anitabi.cn/bangumi/{SUBJECT}/points/detail?haveImage=true', timeout=30).json()
        if isinstance(rows, list) and rows and rows[0].get('image'):
            img = rows[0]['image'].split('?')[0] + '?plan=h360'
            out['point_image'] = probe(img, session, stream=True)
        out['point_count'] = len(rows) if isinstance(rows, list) else None
        if isinstance(rows, list) and rows:
            out['point_sample'] = rows[0]
            out['point_keys'] = sorted(rows[0].keys())
            out['points_with_geo'] = sum(1 for r in rows if isinstance(r, dict) and r.get('geo'))

    # points/detail 现在只给 id/image/name/origin/originURL，没有 geo/ep/s。
    # 试几种候选接口，确认哪一个还能拿到坐标。
    variants = {}
    for path in ('/points', '/points?haveImage=true', '/points/detail', '/points/detail?haveImage=true',
                 '', '/full', '/points/compact'):
        url = f'https://api.anitabi.cn/bangumi/{SUBJECT}{path}'
        try:
            r = session.get(url, timeout=30)
            item = {'status': r.status_code, 'type': r.headers.get('content-type')}
            if r.ok:
                body = r.json()
                if isinstance(body, list):
                    item['count'] = len(body)
                    item['keys'] = sorted(body[0].keys()) if body else []
                    item['with_geo'] = sum(1 for x in body if isinstance(x, dict) and x.get('geo'))
                    item['sample'] = body[0] if body else None
                elif isinstance(body, dict):
                    item['keys'] = sorted(body.keys())[:20]
                    pts = body.get('points')
                    if isinstance(pts, list):
                        item['points'] = len(pts)
                        item['with_geo'] = sum(1 for x in pts if isinstance(x, dict) and x.get('geo'))
                        item['point_keys'] = sorted(pts[0].keys()) if pts else []
                        item['sample'] = pts[0] if pts else None
            variants[path or '/'] = item
        except Exception as exc:
            variants[path or '/'] = {'error': f'{type(exc).__name__}: {exc}'}
    out['point_variants'] = variants

    out['api_ok'] = bool(lite_ok and points_ok)

    # 下载/上传速度基线：同一张点位图连取 8 次（顺序），记录每次字节数和耗时。
    if points_ok:
        try:
            rows = session.get(
                f'https://api.anitabi.cn/bangumi/{SUBJECT}/points/detail?haveImage=true', timeout=30).json()
            img = rows[0]['image'].split('?')[0] + '?plan=h360'
            samples = []
            for _ in range(8):
                t0 = time.monotonic()
                resp = session.get(img, timeout=45)
                samples.append({'status': resp.status_code, 'bytes': len(resp.content),
                                'seconds': round(time.monotonic() - t0, 2)})
            out['image_timing'] = samples
        except Exception as exc:
            out['image_timing'] = {'error': f'{type(exc).__name__}: {exc}'}

    if out['api_ok'] and os.environ.get('PROBE_CRAWL', '1') == '1':
        ids = ROOT / 'ops' / 'anitabi' / 'ids-probe.txt'
        data_dir = ROOT / 'runs' / 'probe-data'
        cmd = [sys.executable, str(ROOT / 'ops' / 'anitabi' / 'crawler.py'),
               '--ids', str(ids), '--mode', 'local', '--data', str(data_dir), '--interval', '3']
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=900)
        out['crawler'] = {
            'exit': proc.returncode,
            'tail': (proc.stdout or '')[-1500:] + (proc.stderr or '')[-1500:],
        }
        current = data_dir / 'CURRENT'
        if current.is_file():
            export = data_dir / current.read_text(encoding='utf-8-sig').strip()
            rows = (export / '具体点.csv').read_text(encoding='utf-8-sig').splitlines()
            out['crawler']['point_rows'] = max(0, len(rows) - 1)
            out['crawler']['images'] = sum(1 for _ in (data_dir / 'images').glob('*')) if (data_dir / 'images').is_dir() else 0

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(out, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
