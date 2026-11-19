"""Match the site's existing comics to mangacopy path_words via the site search UI.

Input : ops/copymanga/site_comics.tsv  (comicid <TAB> name <TAB> chaptercount)
Output: runs/copymanga-matches.json + ops/copymanga/comics.txt
        comics.txt lines: path_word <TAB> site_comicid <TAB> site_name <TAB> matched_title <TAB> status <TAB> source_chapters
"""
import difflib
import json
import os
import re
import sys
import time
from pathlib import Path

from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE = 'https://www.mangacopy.com'
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
ROOT = Path(__file__).resolve().parents[2]


def build_driver():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    return webdriver.Chrome(options=opts)


def normalize(text: str) -> str:
    return re.sub(r'[\s\-_·:：!！?？,，.。()（）\[\]【】]+', '', text or '').lower()


def search(driver, name):
    driver.get(BASE + '/')
    time.sleep(2)
    box = None
    for sel in ('input[type=text]', 'input[placeholder*="搜索"]', 'input[placeholder*="漫畫"]',
                'input[placeholder*="漫画"]', '#searchKey', 'input[name="key"]'):
        els = driver.find_elements(By.CSS_SELECTOR, sel)
        if els:
            box = els[0]
            break
    if box is None:
        return {'error': 'no search box', 'url': driver.current_url}
    box.clear()
    box.send_keys(name)
    box.send_keys(Keys.ENTER)
    time.sleep(3)
    search_url = driver.current_url
    soup = BeautifulSoup(driver.page_source, 'html.parser')
    results = []
    seen = set()
    for a in soup.find_all('a', href=True):
        href = a['href']
        m = re.search(r'/comic/([A-Za-z0-9_-]+)', href)
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
        results.append({'path_word': pw, 'title': title, 'url': BASE + href})
    return {'search_url': search_url, 'results': results}


def comic_meta(driver, url):
    driver.get(url)
    time.sleep(2.5)
    soup = BeautifulSoup(driver.page_source, 'html.parser')
    h6 = soup.find('h6')
    title = h6.get_text(strip=True) if h6 else ''
    text = soup.get_text(' ', strip=True)
    status = '未知'
    for token, label in (('已完结', '完结'), ('已完結', '完结'), ('连载中', '连载中'),
                         ('連載中', '连载中'), ('完结', '完结'), ('連載', '连载中')):
        if token in text:
            status = label
            break
    box = soup.find('div', id='default全部')
    if box is None:
        for div in soup.find_all('div', id=True):
            if div['id'].startswith('default'):
                box = div
                break
    chapters = 0
    if box is not None:
        chapters = len([a for a in box.find_all('a', href=True) if 'chapter' in a['href']])
    cover = ''
    for img in soup.find_all('img'):
        src = img.get('data-src') or img.get('src') or ''
        if '/cover/' in src:
            cover = src
            break
    return {'title': title, 'status': status, 'chapters': chapters, 'cover': cover}


def main():
    rows = []
    for line in (ROOT / 'ops' / 'copymanga' / 'site_comics.tsv').read_text(encoding='utf-8-sig').splitlines():
        parts = line.split('\t')
        if len(parts) >= 2 and parts[0].strip().isdigit():
            rows.append({'comicid': int(parts[0]), 'name': parts[1].strip(),
                         'site_chapters': int(parts[2]) if len(parts) > 2 and parts[2].strip().isdigit() else 0})
    print(f'site comics: {len(rows)}', flush=True)

    driver = build_driver()
    matches = []
    try:
        for row in rows:
            entry = dict(row)
            try:
                res = search(driver, row['name'])
                entry['search'] = {'url': res.get('search_url'), 'count': len(res.get('results', []))}
                best = None
                best_score = 0.0
                for cand in res.get('results', []):
                    score = difflib.SequenceMatcher(None, normalize(row['name']), normalize(cand['title'])).ratio()
                    if score > best_score:
                        best, best_score = cand, score
                if best and best_score >= 0.45:
                    meta = comic_meta(driver, best['url'])
                    entry.update({'path_word': best['path_word'], 'matched_title': meta['title'] or best['title'],
                                  'score': round(best_score, 2), 'status': meta['status'],
                                  'source_chapters': meta['chapters'], 'cover': meta['cover']})
                else:
                    entry.update({'path_word': '', 'matched_title': '', 'score': round(best_score, 2),
                                  'status': '未匹配', 'source_chapters': 0})
                entries = res.get('results', [])
                entry['top3'] = [{'title': c['title'], 'path_word': c['path_word']} for c in entries[:3]]
            except Exception as exc:
                entry.update({'path_word': '', 'status': f'ERROR {type(exc).__name__}', 'source_chapters': 0})
            matches.append(entry)
            print(f"{row['comicid']:>3} {row['name'][:18]:<18} -> {entry.get('path_word','')!s:<22} "
                  f"{entry.get('status','')} 打分={entry.get('score')} 源章节={entry.get('source_chapters')}", flush=True)
    finally:
        driver.quit()

    os.makedirs('runs', exist_ok=True)
    Path('runs/copymanga-matches.json').write_text(json.dumps(matches, ensure_ascii=False, indent=2), encoding='utf-8')
    lines = []
    for m in matches:
        if not m.get('path_word'):
            continue
        lines.append('\t'.join([m['path_word'], str(m['comicid']), m['name'], m.get('matched_title') or '',
                                m.get('status') or '', str(m.get('source_chapters') or 0)]))
    (ROOT / 'ops' / 'copymanga' / 'comics.txt').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    matched = sum(1 for m in matches if m.get('path_word'))
    print(f'matched {matched}/{len(matches)}', flush=True)


if __name__ == '__main__':
    main()
