"""Server side: import today's plan manifests into MySQL and update the queue/report tables.

Reads copymanga/plan/latest.json + copymanga/manifests/<path_word>.json from OSS.
Idempotent: chapters are keyed by manifest uuid, pages by (chapterid, page).
"""
import argparse
import json
import os
import re
import sys
import time
import urllib.request

import pymysql.cursors

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

OSS_BASE = os.environ.get('OSS_PUBLIC_BASE', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com').rstrip('/')

DDL = [
    """CREATE TABLE IF NOT EXISTS tb_copymanga_queue(
         path_word VARCHAR(64) PRIMARY KEY, comicid INT NULL, title VARCHAR(255) NULL,
         status VARCHAR(32) NULL, rank_today INT NULL, state VARCHAR(32) NOT NULL DEFAULT 'planned',
         chapters_total INT NOT NULL DEFAULT 0, chapters_imported INT NOT NULL DEFAULT 0,
         note VARCHAR(255) NULL,
         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
       ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    """CREATE TABLE IF NOT EXISTS tb_copymanga_daily(
         id INT AUTO_INCREMENT PRIMARY KEY, day DATE NOT NULL, rank_today INT NULL,
         path_word VARCHAR(64) NOT NULL, title VARCHAR(255) NULL,
         action VARCHAR(32) NOT NULL, result VARCHAR(32) NULL,
         chapters_imported INT NOT NULL DEFAULT 0, chapters_remaining INT NOT NULL DEFAULT 0,
         note VARCHAR(255) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
         UNIQUE KEY uk_day_pw(day, path_word)
       ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
]


def norm(text):
    return re.sub(r'[\s\-_·:：!！?？,，.。()（）\[\]【】]+', '', text or '').lower()


def fetch_json(url, timeout=60):
    req = urllib.request.Request(url, headers={'User-Agent': '0721acgn-importer'})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode('utf-8'))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--plan', default=f'{OSS_BASE}/copymanga/plan/latest.json')
    p.add_argument('--dry-run', action='store_true')
    args = p.parse_args()

    plan = fetch_json(args.plan)
    items = plan.get('items') or []
    print(f"plan: {len(items)} works", flush=True)
    if not items:
        return 0

    conn = pymysql.connect(host=os.environ.get('MYSQL_HOST', '127.0.0.1'),
                           port=int(os.environ.get('MYSQL_PORT', '3306')),
                           user=os.environ.get('MYSQL_USER', 'root'),
                           password=os.environ.get('MYSQL_PASSWORD', ''),
                           database=os.environ.get('MYSQL_DATABASE', '0721acgn'),
                           charset='utf8mb4', autocommit=False,
                           cursorclass=pymysql.cursors.DictCursor)
    today = time.strftime('%Y-%m-%d')
    results = []
    try:
        cur = conn.cursor()
        for ddl in DDL:
            cur.execute(ddl)

        for item in items:
            pw = item.get('path_word')
            title = item.get('title') or pw
            action = item.get('action') or 'new'
            row = {'path_word': pw, 'title': title, 'action': action}
            try:
                manifest = fetch_json(f'{OSS_BASE}/copymanga/manifests/{pw}.json')
            except Exception:
                row.update({'result': 'failed', 'note': 'manifest missing'})
                results.append(row)
                continue

            chapters = manifest.get('chapters') or []
            status = manifest.get('status') or '未知'
            total = manifest.get('chapter_total') or len(chapters)

            cur.execute('SELECT comicid FROM tb_copymanga_map WHERE path_word=%s', (pw,))
            m = cur.fetchone()
            comicid = m['comicid'] if m else (item.get('comicid') or None)
            if not comicid:
                n = norm(manifest.get('title') or title)
                cur.execute('SELECT comicid, name FROM tb_cartoons')
                for c in cur.fetchall():
                    if n and norm(c['name']) == n:
                        comicid = c['comicid']
                        break
            if not comicid:
                cur.execute('SELECT COALESCE(MAX(comicid),0)+1 AS nxt FROM tb_cartoons')
                comicid = cur.fetchone()['nxt']
                cur.execute("""INSERT INTO tb_cartoons(comicid,name,picurl,chaptercount,status,updated_at)
                               VALUES(%s,%s,%s,%s,%s,NOW())""",
                            (comicid, manifest.get('title') or title, manifest.get('cover') or None,
                             str(total), status))
                row['action'] = 'new'
            if not m:
                cur.execute('INSERT INTO tb_copymanga_map(path_word, comicid) VALUES(%s,%s)',
                            (pw, comicid))

            cur.execute('SELECT COALESCE(MAX(chapterno),0) AS mx FROM tb_cartoonchapters WHERE comicid=%s',
                        (comicid,))
            have = int(cur.fetchone()['mx'] or 0)
            cur.execute('SELECT uuid FROM tb_copymanga_chapter_map WHERE path_word=%s', (pw,))
            done_uuids = {r['uuid'] for r in cur.fetchall()}

            imported = 0
            for ch in sorted(chapters, key=lambda c: c['index']):
                if ch['uuid'] in done_uuids or int(ch['index']) <= have:
                    continue
                pages = ch.get('pages') or []
                if not pages:
                    continue
                cur.execute('SELECT COALESCE(MAX(chapterid),0)+1 AS nxt FROM tb_cartoonchapters')
                chapterid = cur.fetchone()['nxt']
                cur.execute('SELECT COALESCE(MAX(id),0)+1 AS nxt FROM tb_cartoonchapters')
                chapter_row = cur.fetchone()['nxt']
                cur.execute("""INSERT INTO tb_cartoonchapters(id,chapterid,comicid,chapterno,title)
                               VALUES(%s,%s,%s,%s,%s)""",
                            (chapter_row, chapterid, comicid, ch['index'],
                             ch.get('name') or f"第{ch['index']}话"))
                cur.execute('SELECT COALESCE(MAX(id),0) AS mx FROM tb_cartoonpage')
                next_page_id = int(cur.fetchone()['mx']) + 1
                rows = []
                for page in sorted(pages, key=lambda x: x['page']):
                    rows.append((next_page_id, chapterid, page['page'], page['url'], f"{page['page']:04d}"))
                    next_page_id += 1
                cur.executemany("""INSERT INTO tb_cartoonpage(id,chapterid,pageid,fileurl,pageorder)
                                   VALUES(%s,%s,%s,%s,%s)""", rows)
                cur.execute("""INSERT INTO tb_copymanga_chapter_map(path_word,uuid,chapterid,page_count)
                               VALUES(%s,%s,%s,%s)""", (pw, ch['uuid'], chapterid, len(rows)))
                imported += 1

            cur.execute('SELECT COUNT(*) AS c FROM tb_copymanga_chapter_map WHERE path_word=%s', (pw,))
            mapped_chapters = int(cur.fetchone()['c'])
            cur.execute('UPDATE tb_cartoons SET status=%s, chaptercount=%s, updated_at=NOW() WHERE comicid=%s',
                        (status, str(mapped_chapters), comicid))

            state = 'done' if status == '完结' and mapped_chapters >= total else 'imported'
            cur.execute("""INSERT INTO tb_copymanga_queue(path_word,comicid,title,status,rank_today,state,
                             chapters_total,chapters_imported)
                           VALUES(%s,%s,%s,%s,%s,%s,%s,%s)
                           ON DUPLICATE KEY UPDATE comicid=VALUES(comicid), title=VALUES(title),
                             status=VALUES(status), rank_today=VALUES(rank_today), state=VALUES(state),
                             chapters_total=VALUES(chapters_total), chapters_imported=VALUES(chapters_imported)""",
                        (pw, comicid, manifest.get('title') or title, status, item.get('rank'), state,
                         total, mapped_chapters))
            remaining = max(0, total - mapped_chapters)
            cur.execute("""INSERT INTO tb_copymanga_daily(day,rank_today,path_word,title,action,result,
                             chapters_imported,chapters_remaining)
                           VALUES(%s,%s,%s,%s,%s,%s,%s,%s)
                           ON DUPLICATE KEY UPDATE rank_today=VALUES(rank_today), title=VALUES(title),
                             action=VALUES(action), result=VALUES(result),
                             chapters_imported=VALUES(chapters_imported),
                             chapters_remaining=VALUES(chapters_remaining)""",
                        (today, item.get('rank'), pw, manifest.get('title') or title, row['action'],
                         'imported' if imported else 'no-new', imported, remaining))
            row.update({'comicid': comicid, 'imported': imported, 'have': mapped_chapters,
                        'total': total, 'status': status, 'result': 'imported'})
            results.append(row)
            print(f"  {pw} {manifest.get('title')} status={status} +{imported} chapters "
                  f"({mapped_chapters}/{total})", flush=True)
            conn.commit()

        if args.dry_run:
            conn.rollback()
            print('dry run, rolled back', flush=True)
        else:
            conn.commit()
        print(json.dumps({'items': results}, ensure_ascii=False)[:2000], flush=True)
        return 0
    finally:
        conn.close()


if __name__ == '__main__':
    sys.exit(main())
