"""Incremental Anitabi collector. Python 3.10+."""
import argparse
import csv
import hashlib
import json
import logging
import os
from pathlib import Path
import sqlite3
import tempfile
import time
from urllib.parse import urlsplit, urlunsplit, parse_qsl, urlencode

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

LOG = logging.getLogger('anitabi')
API = 'https://api.anitabi.cn'


def resized(url, original=False):
    p = urlsplit(url)
    if p.scheme != 'https' or not p.hostname:
        raise ValueError('Image URL must use HTTPS')
    query = [(k, v) for k, v in parse_qsl(p.query) if k != 'plan']
    if not original:
        query.append(('plan', 'h360'))
    return urlunsplit((p.scheme, p.netloc, p.path, urlencode(query), ''))


class Collector:
    def __init__(self, root, mode='local', interval=1.0, days=7):
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(self.root / 'state.sqlite3')
        self.db.executescript('''
          PRAGMA journal_mode=WAL;
          CREATE TABLE IF NOT EXISTS http(url TEXT PRIMARY KEY, body TEXT, etag TEXT, modified TEXT);
          CREATE TABLE IF NOT EXISTS works(id INTEGER PRIMARY KEY, checked REAL, body TEXT);
          CREATE TABLE IF NOT EXISTS assets(key TEXT PRIMARY KEY, target TEXT);
        ''')
        self.mode, self.interval, self.days = mode, interval, days
        self.last = 0
        self.session = requests.Session()
        self.session.headers['User-Agent'] = 'AnitabiIncrementalCollector/1.0'
        retry = Retry(total=3, backoff_factor=2, status_forcelist=[429,500,502,503,504],
                      allowed_methods=['GET'], respect_retry_after_header=True)
        self.session.mount('https://', HTTPAdapter(max_retries=retry))
        self.bucket = None
        if mode == 'oss':
            import oss2
            self.bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'],
                os.environ['OSS_ACCESS_KEY_SECRET']), os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
            self.public = os.environ['OSS_PUBLIC_BASE'].rstrip('/')

    def get(self, url, **kwargs):
        time.sleep(max(0, self.interval - (time.monotonic() - self.last)))
        self.last = time.monotonic()
        return self.session.get(url, timeout=(10, 60), **kwargs)

    def json(self, url):
        old = self.db.execute('SELECT body,etag,modified FROM http WHERE url=?', (url,)).fetchone()
        headers = {}
        if old:
            if old[1]: headers['If-None-Match'] = old[1]
            if old[2]: headers['If-Modified-Since'] = old[2]
        with self.get(url, headers=headers) as r:
            if r.status_code == 304 and old:
                return json.loads(old[0])
            r.raise_for_status()
            value = r.json()
            with self.db:
                self.db.execute('INSERT OR REPLACE INTO http VALUES(?,?,?,?)',
                    (url, json.dumps(value, ensure_ascii=False), r.headers.get('ETag'), r.headers.get('Last-Modified')))
            return value

    def image(self, url, version):
        if not url: return ''
        if self.mode == 'links': return url
        # Source modification participates in the key: a changed image at the same URL is refreshed.
        key = hashlib.sha256((url + '|' + str(version)).encode()).hexdigest()
        old = self.db.execute('SELECT target FROM assets WHERE key=?', (key,)).fetchone()
        if old and (self.mode == 'oss' or (self.root / old[0]).is_file()): return old[0]
        folder = self.root / 'images'
        folder.mkdir(exist_ok=True)
        path = folder / key
        if not path.exists():
            tmp = path.with_suffix('.part')
            try:
                with self.get(url, stream=True) as r:
                    r.raise_for_status()
                    if not r.headers.get('Content-Type', '').lower().startswith('image/'):
                        raise ValueError('Server did not return an image')
                    size = 0
                    with tmp.open('wb') as f:
                        for chunk in r.iter_content(65536):
                            size += len(chunk)
                            if size > 30 * 1024 * 1024: raise ValueError('Image exceeds 30 MiB')
                            f.write(chunk)
                    if not size: raise ValueError('Empty image')
                os.replace(tmp, path)
            finally:
                tmp.unlink(missing_ok=True)
        target = 'images/' + key
        if self.bucket:
            object_key = 'xunli-v2/' + key
            with path.open('rb') as f:
                magic = f.read(16)
            mime = ('image/png' if magic.startswith(b'\x89PNG') else
                    'image/webp' if magic.startswith(b'RIFF') and magic[8:12] == b'WEBP' else
                    'image/gif' if magic.startswith(b'GIF8') else
                    'image/jpeg' if magic.startswith(b'\xff\xd8') else 'application/octet-stream')
            self.bucket.put_object_from_file(object_key, str(path), headers={'Content-Type': mime})
            target = self.public + '/' + object_key
        with self.db:
            self.db.execute('INSERT OR REPLACE INTO assets VALUES(?,?)', (key, target))
        if self.bucket: path.unlink(missing_ok=True)
        return target

    def collect(self, subject, original=False, force=False):
        old = self.db.execute('SELECT checked FROM works WHERE id=?', (subject,)).fetchone()
        if old and not force and time.time() - old[0] < self.days * 86400:
            return False
        base = self.json(f'{API}/bangumi/{subject}/lite')
        points = self.json(f'{API}/bangumi/{subject}/points/detail?haveImage=true')
        if not isinstance(base, dict) or not base.get('id') or not isinstance(points, list):
            raise ValueError('Unexpected upstream schema')
        version = base.get('modified', '')
        cover = self.image(resized(base['cover']), version) if base.get('cover') else ''
        rows = []
        for point in points:
            if not isinstance(point, dict): raise ValueError('Unexpected point schema')
            if not point.get('image'): continue
            small = self.image(resized(point['image']), version)
            full = self.image(resized(point['image'], True), version) if original else small
            rows.append(dict(xunliid=subject, xunliname=point.get('cn') or point.get('name'),
                xunlipic=full, gaoqingpic=small, ep=point.get('ep'), time=point.get('s'),
                geo=point.get('geo'), point_id=point.get('id'), origin=point.get('origin'),
                originURL=point.get('originURL')))
        expected = base.get('imagesLength')
        if isinstance(expected, int) and len(rows) < expected:
            LOG.warning('id=%s API returned %s image points; advertised=%s; completeness not guaranteed', subject, len(rows), expected)
        main = dict(xunliid=subject, xunliname=base.get('cn') or base.get('title'),
            japanname=base.get('title'), city=base.get('city'), bcimg=cover,
            xunlicolor=base.get('color'), maingeo=base.get('geo'), zoom=base.get('zoom'), count=len(rows), hengpic='')
        payload = dict(main=main, points=rows, source=base, raw_points=points)
        with self.db:
            self.db.execute('INSERT OR REPLACE INTO works VALUES(?,?,?)', (subject, time.time(), json.dumps(payload, ensure_ascii=False)))
        return True

    def export(self):
        main_fields = 'xunliid xunliname japanname city bcimg xunlicolor maingeo zoom count hengpic'.split()
        point_fields = 'xunliid xunliname xunlipic gaoqingpic ep time geo point_id origin originURL'.split()
        # Publish both files as one generation; CURRENT changes only after both are complete.
        folder = Path(tempfile.mkdtemp(prefix='export-', dir=self.root))
        with (folder/'巡礼番剧.csv').open('w', encoding='utf-8-sig', newline='') as a, (folder/'具体点.csv').open('w', encoding='utf-8-sig', newline='') as b:
            wa, wb = csv.DictWriter(a, main_fields), csv.DictWriter(b, point_fields)
            wa.writeheader(); wb.writeheader()
            for (body,) in self.db.execute('SELECT body FROM works ORDER BY id'):
                d = json.loads(body)
                wa.writerow(d['main']); wb.writerows(d['points'])
        pointer = self.root / 'CURRENT.tmp'
        pointer.write_text(folder.name, encoding='utf-8')
        os.replace(pointer, self.root / 'CURRENT')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--data', default='data')
    p.add_argument('--ids', help='UTF-8 text file: one Bangumi ID per line; # comments allowed')
    p.add_argument('--discover', action='store_true', help='Try undocumented /bangumi list; never scan ID ranges')
    p.add_argument('--mode', choices=['local','oss','links'], default='local')
    p.add_argument('--days', type=float, default=7)
    p.add_argument('--interval', type=float, default=1)
    p.add_argument('--original', action='store_true')
    p.add_argument('--force', action='store_true', help='Ignore refresh TTL, retain HTTP and image caches')
    p.add_argument('--export-only', action='store_true')
    args = p.parse_args()
    if args.days < 0 or args.interval < 0: p.error('days and interval must be nonnegative')
    if not (args.ids or args.discover or args.export_only): p.error('provide --ids or --discover')
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    Path(args.data).mkdir(parents=True, exist_ok=True)
    lock = open(Path(args.data)/'run.lock', 'a+')
    if os.name == 'posix':
        import fcntl
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: raise SystemExit('Another collector is running')
    c = Collector(args.data, args.mode, args.interval, args.days)
    # Prevent silently reusing links/local records after switching output policy.
    policy = Path(args.data)/'policy.json'
    wanted = dict(mode=args.mode, original=args.original, public=os.environ.get('OSS_PUBLIC_BASE', ''), bucket=os.environ.get('OSS_BUCKET', ''))
    if policy.exists() and json.loads(policy.read_text()) != wanted:
        raise SystemExit('Output policy changed: use a different --data directory')
    policy.write_text(json.dumps(wanted), encoding='utf-8')
    ids = set()
    if args.ids:
        for line in Path(args.ids).read_text(encoding='utf-8-sig').splitlines():
            value = line.split('#',1)[0].strip()
            if value:
                n = int(value)
                if n <= 0: raise ValueError('IDs must be positive')
                ids.add(n)
    if args.discover:
        listing = c.json(API+'/bangumi')
        if not isinstance(listing, list) or not listing: raise ValueError('Unexpected/empty discovery response; supply --ids')
        ids.update(int(row['id']) for row in listing)
    failed = 0
    try:
        for subject in sorted(ids):
            try:
                changed = c.collect(subject, args.original, args.force)
                LOG.info('id=%s %s', subject, 'saved' if changed else 'cached')
            except requests.HTTPError as exc:
                failed += 1
                status = exc.response.status_code
                LOG.error('id=%s HTTP %s', subject, status)
                if status in (401,403,429): break
            except Exception as exc:
                failed += 1
                LOG.error('id=%s failed (%s); retry next run', subject, type(exc).__name__)
    finally:
        c.export()
        c.session.close(); c.db.close(); lock.close()
    return 1 if failed else 0


if __name__ == '__main__':
    raise SystemExit(main())
