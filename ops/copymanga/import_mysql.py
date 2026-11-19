"""Import copymanga manifests (from OSS) into the site's MySQL cartoon tables.

Manifest layout (written by crawl_selenium.py):
  copymanga/manifests/<path_word>.json
  {title, cover, chapter_total, chapters: [{index, uuid, name, pages: [{page, url, bytes}]}]}

Mapping tables keep it idempotent:
  tb_copymanga_map(path_word -> comicid)
  tb_copymanga_chapter_map(path_word, uuid -> chapterid)

    python import_mysql.py --path-word HAIBIANZHIYE --dry-run
"""
import argparse
import json
import os
import sys
import time
import urllib.request

import pymysql.cursors

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

MANIFEST_BASE = os.environ.get(
    'COPY_MANIFEST_BASE', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/copymanga/manifests')

DDL = [
    """CREATE TABLE IF NOT EXISTS tb_copymanga_map(
         path_word VARCHAR(64) PRIMARY KEY,
         comicid INT NOT NULL,
         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
       ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    """CREATE TABLE IF NOT EXISTS tb_copymanga_chapter_map(
         path_word VARCHAR(64) NOT NULL,
         uuid VARCHAR(64) NOT NULL,
         chapterid INT NOT NULL,
         page_count INT NOT NULL DEFAULT 0,
         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
         PRIMARY KEY (path_word, uuid)
       ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
]


def fetch_manifest(path_word):
    url = f'{MANIFEST_BASE}/{path_word}.json'
    req = urllib.request.Request(url, headers={'User-Agent': '0721acgn-importer'})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode('utf-8'))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--path-word', required=True)
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--host', default=os.environ.get('MYSQL_HOST', '127.0.0.1'))
    p.add_argument('--port', type=int, default=int(os.environ.get('MYSQL_PORT', '3306')))
    p.add_argument('--user', default=os.environ.get('MYSQL_USER', 'root'))
    p.add_argument('--password', default=os.environ.get('MYSQL_PASSWORD', ''))
    p.add_argument('--database', default=os.environ.get('MYSQL_DATABASE', '0721acgn'))
    args = p.parse_args()

    manifest = fetch_manifest(args.path_word)
    chapters = manifest.get('chapters') or []
    print(f'manifest: {manifest.get("title")} chapters={len(chapters)} '
          f'pages={sum(len(c.get("pages", [])) for c in chapters)}', flush=True)
    if not chapters:
        print('nothing to import')
        return 0

    conn = pymysql.connect(host=args.host, port=args.port, user=args.user, password=args.password,
                           database=args.database, charset='utf8mb4', autocommit=False,
                           cursorclass=pymysql.cursors.DictCursor)
    stats = {'chapters_new': 0, 'pages_new': 0, 'chapters_skipped': 0, 'comicid': None}
    try:
        cur = conn.cursor()
        for ddl in DDL:
            cur.execute(ddl)

        cur.execute('SELECT comicid FROM tb_copymanga_map WHERE path_word=%s', (args.path_word,))
        row = cur.fetchone()
        if row:
            comicid = row['comicid']
        else:
            cur.execute('SELECT COALESCE(MAX(comicid), 0) + 1 AS nxt FROM tb_cartoons')
            comicid = cur.fetchone()['nxt']
            cur.execute(
                """INSERT INTO tb_cartoons(comicid, name, picurl, chaptercount, updated_at)
                   VALUES(%s, %s, %s, %s, NOW())""",
                (comicid, manifest.get('title') or args.path_word,
                 manifest.get('cover') or None, str(manifest.get('chapter_total') or len(chapters))))
            cur.execute('INSERT INTO tb_copymanga_map(path_word, comicid) VALUES(%s, %s)',
                        (args.path_word, comicid))
            print(f'comic created: comicid={comicid} {manifest.get("title")}', flush=True)
        stats['comicid'] = comicid

        cur.execute('SELECT uuid FROM tb_copymanga_chapter_map WHERE path_word=%s', (args.path_word,))
        done = {r['uuid'] for r in cur.fetchall()}

        for chapter in sorted(chapters, key=lambda c: c['index']):
            if chapter['uuid'] in done:
                stats['chapters_skipped'] += 1
                continue
            cur.execute('SELECT COALESCE(MAX(chapterid), 0) + 1 AS nxt FROM tb_cartoonchapters')
            chapterid = cur.fetchone()['nxt']
            cur.execute('SELECT COALESCE(MAX(id), 0) + 1 AS nxt FROM tb_cartoonchapters')
            chapter_row_id = cur.fetchone()['nxt']
            cur.execute(
                """INSERT INTO tb_cartoonchapters(id, chapterid, comicid, chapterno, title)
                   VALUES(%s, %s, %s, %s, %s)""",
                (chapter_row_id, chapterid, comicid, chapter['index'], chapter.get('name') or f'第{chapter["index"]}话'))

            cur.execute('SELECT COALESCE(MAX(id), 0) AS mx FROM tb_cartoonpage')
            next_page_id = cur.fetchone()['mx'] + 1
            rows = []
            for page in sorted(chapter.get('pages', []), key=lambda x: x['page']):
                rows.append((next_page_id, chapterid, page['page'], page['url'], f'{page["page"]:04d}'))
                next_page_id += 1
            if rows:
                cur.executemany(
                    """INSERT INTO tb_cartoonpage(id, chapterid, pageid, fileurl, pageorder)
                       VALUES(%s, %s, %s, %s, %s)""", rows)
            cur.execute(
                """INSERT INTO tb_copymanga_chapter_map(path_word, uuid, chapterid, page_count)
                   VALUES(%s, %s, %s, %s)""",
                (args.path_word, chapter['uuid'], chapterid, len(rows)))
            stats['chapters_new'] += 1
            stats['pages_new'] += len(rows)
            print(f'  chapter {chapter["index"]} {chapter.get("name")}: {len(rows)} pages', flush=True)

        cur.execute('SELECT COUNT(*) AS c FROM tb_copymanga_chapter_map WHERE path_word=%s', (args.path_word,))
        total = cur.fetchone()['c']
        cur.execute('UPDATE tb_cartoons SET chaptercount=%s, updated_at=NOW() WHERE comicid=%s',
                    (str(total), comicid))
        if args.dry_run:
            conn.rollback()
            print('dry run, rolled back', flush=True)
        else:
            conn.commit()
            print('committed', flush=True)
        print(json.dumps(stats, ensure_ascii=False), flush=True)
        return 0
    finally:
        conn.close()


if __name__ == '__main__':
    sys.exit(main())
