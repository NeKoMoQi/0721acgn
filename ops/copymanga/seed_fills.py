"""Link matched site comics to mangacopy path_words and queue them for backfill.

Input: ops/copymanga/comics.txt (from match_comics.py)
Effects: tb_copymanga_map, tb_copymanga_queue (state=imported), tb_cartoons.status
"""
import argparse
import os
import re
import sys

import pymysql.cursors

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--file', default=os.path.join(ROOT, 'ops', 'copymanga', 'comics.txt'))
    p.add_argument('--dry-run', action='store_true')
    args = p.parse_args()

    rows = []
    with open(args.file, encoding='utf-8-sig') as fh:
        for line in fh:
            parts = line.rstrip('\n').split('\t')
            if len(parts) >= 4 and parts[0].strip():
                rows.append({'path_word': parts[0].strip(), 'comicid': int(parts[1]),
                             'site_name': parts[2], 'title': parts[3],
                             'status': parts[4] if len(parts) > 4 else '未知',
                             'total': int(parts[5]) if len(parts) > 5 and parts[5].isdigit() else 0})
    print(f'comics.txt rows: {len(rows)}', flush=True)

    conn = pymysql.connect(host=os.environ.get('MYSQL_HOST', '127.0.0.1'),
                           port=int(os.environ.get('MYSQL_PORT', '3306')),
                           user=os.environ.get('MYSQL_USER', 'root'),
                           password=os.environ.get('MYSQL_PASSWORD', ''),
                           database=os.environ.get('MYSQL_DATABASE', '0721acgn'),
                           charset='utf8mb4', autocommit=False,
                           cursorclass=pymysql.cursors.DictCursor)
    linked = 0
    try:
        cur = conn.cursor()
        for row in rows:
            cur.execute('SELECT COALESCE(MAX(chapterno),0) AS mx FROM tb_cartoonchapters WHERE comicid=%s',
                        (row['comicid'],))
            have = int(cur.fetchone()['mx'] or 0)
            cur.execute('SELECT comicid FROM tb_copymanga_map WHERE path_word=%s', (row['path_word'],))
            mapped = cur.fetchone()
            if not mapped:
                cur.execute('INSERT INTO tb_copymanga_map(path_word, comicid) VALUES(%s,%s)',
                            (row['path_word'], row['comicid']))
                linked += 1
            state = 'done' if row['status'] == '完结' else 'imported'
            cur.execute("""INSERT INTO tb_copymanga_queue(path_word,comicid,title,status,state,
                             chapters_total,chapters_imported)
                           VALUES(%s,%s,%s,%s,%s,%s,%s)
                           ON DUPLICATE KEY UPDATE comicid=VALUES(comicid), title=VALUES(title),
                             status=VALUES(status), state=VALUES(state), chapters_total=VALUES(chapters_total),
                             chapters_imported=GREATEST(chapters_imported, VALUES(chapters_imported))""",
                        (row['path_word'], row['comicid'], row['title'] or row['site_name'],
                         row['status'], state, row['total'], have))
            cur.execute('UPDATE tb_cartoons SET status=%s WHERE comicid=%s', (row['status'], row['comicid']))
            print(f"  {row['comicid']:>3} {row['site_name'][:14]:<14} -> {row['path_word']:<24} "
                  f"{row['status']} 站内{have}话/源{row['total']}话", flush=True)
        if args.dry_run:
            conn.rollback()
            print('dry run, rolled back', flush=True)
        else:
            conn.commit()
            print(f'committed, new mappings: {linked}', flush=True)
        return 0
    finally:
        conn.close()


if __name__ == '__main__':
    sys.exit(main())
