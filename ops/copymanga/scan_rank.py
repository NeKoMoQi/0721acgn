"""Scan the mangacopy ranking page (top N) and upload the list to OSS.

Output: copymanga/rank/latest.json + copymanga/rank/<date>.json
        [{rank, path_word, title, url}]
"""
import json
import os
import re
import sys
import time

from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE = os.environ.get('COPY_BASE', 'https://www.mangacopy.com').rstrip('/')
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
TOP_N = int(os.environ.get('TOP_N', '100'))


def build_driver():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    return webdriver.Chrome(options=opts)


def parse(html):
    soup = BeautifulSoup(html, 'html.parser')
    rows = []
    seen = set()
    for a in soup.find_all('a', href=True):
        m = re.search(r'/comic/([A-Za-z0-9_-]+)', a['href'])
        if not m:
            continue
        pw = m.group(1)
        if pw in seen:
            continue
        seen.add(pw)
        title = a.get_text(' ', strip=True)
        if not title:
            img = a.find('img')
            title = (img.get('alt') or img.get('title') or '') if img else ''
        rows.append({'path_word': pw, 'title': title, 'url': BASE + a['href']})
    return rows


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'base': BASE}
    driver = build_driver()
    items = []
    try:
        for path in ('/rank', '/rank/all', '/rank/0'):
            driver.get(BASE + path)
            time.sleep(3)
            page = parse(driver.page_source)
            out.setdefault('probe', {})[path] = {'count': len(page), 'url': driver.current_url,
                                                 'sample': page[:3]}
            if len(page) > len(items):
                items = page
            if len(items) >= TOP_N:
                break
        # 排行榜常见分页：?page=2 ...
        page_no = 2
        while len(items) < TOP_N and page_no <= 10:
            driver.get(f'{BASE}/rank?page={page_no}')
            time.sleep(2)
            more = parse(driver.page_source)
            new = [x for x in more if x['path_word'] not in {i['path_word'] for i in items}]
            if not new:
                break
            items.extend(new)
            page_no += 1
    finally:
        driver.quit()

    items = items[:TOP_N]
    for i, item in enumerate(items, 1):
        item['rank'] = i
    out['count'] = len(items)
    out['items'] = items
    print(f'rank items: {len(items)}; sample: {items[:3]}', flush=True)

    if os.environ.get('OSS_ACCESS_KEY_ID'):
        import oss2
        bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                             os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
        payload = json.dumps({'utc': out['utc'], 'items': items}, ensure_ascii=False).encode('utf-8')
        bucket.put_object('copymanga/rank/latest.json', payload, headers={'Content-Type': 'application/json'})
        bucket.put_object(f'copymanga/rank/{time.strftime("%Y%m%d")}.json', payload,
                          headers={'Content-Type': 'application/json'})
        print('uploaded rank to OSS', flush=True)

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-rank.json', 'w', encoding='utf-8') as fh:
        json.dump({'utc': out['utc'], 'count': len(items), 'probe': out.get('probe', {}),
                   'items': items[:10]}, fh, ensure_ascii=False, indent=2)


if __name__ == '__main__':
    main()
