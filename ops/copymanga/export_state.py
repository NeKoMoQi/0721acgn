"""Export the site's copymanga state to OSS so the Actions job can plan the day.

copymanga/state.json = {
  utc, mapped: {path_word: {comicid, have}}, queued: {path_word: state},
  titles: [{comicid, name, norm}], queue: [...]
}
"""
import json
import os
import re
import sys
import time

import pymysql.cursors

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass


def norm(text):
    return re.sub(r'[\s\-_·:：!！?？,，.。()（）\[\]【】]+', '', text or '').lower()


def main():
    import oss2
    conn = pymysql.connect(host=os.environ.get('MYSQL_HOST', '127.0.0.1'),
                           port=int(os.environ.get('MYSQL_PORT', '3306')),
                           user=os.environ.get('MYSQL_USER', 'root'),
                           password=os.environ.get('MYSQL_PASSWORD', ''),
                           database=os.environ.get('MYSQL_DATABASE', '0721acgn'),
                           charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
    cur = conn.cursor()
    state = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
             'mapped': {}, 'queued': {}, 'titles': [], 'queue': []}
    try:
        cur.execute('SHOW TABLES LIKE "tb_copymanga_map"')
        if not cur.fetchone():
            print('tb_copymanga_map missing', flush=True)
            return 1
        cur.execute('SELECT path_word, comicid FROM tb_copymanga_map')
        for row in cur.fetchall():
            cur.execute('SELECT COALESCE(MAX(chapterno),0) AS m FROM tb_cartoonchapters WHERE comicid=%s',
                        (row['comicid'],))
            have = cur.fetchone()['m']
            state['mapped'][row['path_word']] = {'comicid': row['comicid'], 'have': int(have or 0)}
        cur.execute('SHOW TABLES LIKE "tb_copymanga_queue"')
        if cur.fetchone():
            cur.execute('SELECT path_word, state, status, chapters_total, chapters_imported, updated_at FROM tb_copymanga_queue')
            for row in cur.fetchall():
                state['queued'][row['path_word']] = row['state']
                state['queue'].append(row)
        cur.execute('SELECT comicid, name FROM tb_cartoons')
        state['titles'] = [{'comicid': r['comicid'], 'name': r['name'], 'norm': norm(r['name'])}
                           for r in cur.fetchall()]
    finally:
        conn.close()

    bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                         os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
    bucket.put_object('copymanga/state.json', json.dumps(state, ensure_ascii=False).encode('utf-8'),
                      headers={'Content-Type': 'application/json'})
    print(f"state exported: mapped={len(state['mapped'])} queued={len(state['queued'])} "
          f"titles={len(state['titles'])}", flush=True)
    return 0


if __name__ == '__main__':
    sys.exit(main())
