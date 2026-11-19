"""Match the site's comics to mangacopy path_words (search route + title verify).

站点可用搜索路由是 /search?q=；搜索结果卡片没有稳定标题，所以取前几个结果后
逐一打开作品页，用 h6 标题做相似度校验。
"""
import difflib
import json
import os
import re
import sys
import time
import urllib.parse
from pathlib import Path

import zhconv

from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.alert import Alert

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
    try:
        opts.unhandled_prompt_behavior = 'dismiss'
    except Exception:
        pass
    return webdriver.Chrome(options=opts)


def dismiss(driver):
    try:
        Alert(driver).accept()
        time.sleep(0.5)
    except Exception:
        pass


def variants(name):
    out = [name]
    for target in ('zh-hant', 'zh-hans'):
        try:
            v = zhconv.convert(name, target)
        except Exception:
            v = name
        if v and v not in out:
            out.append(v)
    return out


def similarity(name, title):
    best = 0.0
    for a in variants(name):
        for b in variants(title):
            best = max(best, difflib.SequenceMatcher(None, normalize(a), normalize(b)).ratio())
    return best


def normalize(text):
    return re.sub(r'[\s\-_·:：!！?？,，.。()（）\[\]【】]+', '', text or '').lower()


def goto(driver, url):
    try:
        driver.get(url)
    except Exception:
        try:
            driver.execute_script('window.stop()')
        except Exception:
            pass
    time.sleep(2.2)
    dismiss(driver)


def search_paths(driver, name):
    goto(driver, BASE + '/search?q=' + urllib.parse.quote(name))
    soup = BeautifulSoup(driver.page_source, 'html.parser')
    paths = []
    for a in soup.find_all('a', href=True):
        m = re.search(r'/comic/([A-Za-z0-9_-]+)', a['href'])
        if m:
            pw = m.group(1)
            if pw not in paths:
                paths.append(pw)
    return paths[:4]


def comic_meta(driver, path_word):
    goto(driver, f'{BASE}/comic/{path_word}')
    soup = BeautifulSoup(driver.page_source, 'html.parser')
    h6 = soup.find('h6')
    title = h6.get_text(strip=True) if h6 else ''
    text = soup.get_text(' ', strip=True)
    status = '未知'
    for token, label in (('已完结', '完结'), ('已完結', '完结'), ('连载中', '连载中'),
                         ('連載中', '连载中'), ('连载', '连载中'), ('連載', '连载中')):
        if token in text:
            status = label
            break
    box = soup.find('div', id='default全部')
    if box is None:
        for div in soup.find_all('div', id=True):
            if div['id'].startswith('default'):
                box = div
                break
    chapters = len([a for a in box.find_all('a', href=True) if 'chapter' in a['href']]) if box else 0
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
            entry.update({'path_word': '', 'matched_title': '', 'score': 0.0,
                          'status': '未匹配', 'source_chapters': 0})
            try:
                paths = []
                for query in variants(row['name']):
                    for pw in search_paths(driver, query):
                        if pw not in paths:
                            paths.append(pw)
                paths = paths[:5]
                entry['candidates'] = paths
                best = None
                for pw in paths[:5]:
                    meta = comic_meta(driver, pw)
                    score = similarity(row['name'], meta['title'])
                    if best is None or score > best['score']:
                        best = {'path_word': pw, 'score': round(score, 2), **meta}
                    if score >= 0.85:
                        break
                if best and best['score'] >= 0.55 and (best['chapters'] == 0 or best['chapters'] >= row['site_chapters'] * 0.6):
                    entry.update({'path_word': best['path_word'], 'matched_title': best['title'],
                                  'score': best['score'], 'status': best['status'],
                                  'source_chapters': best['chapters'], 'cover': best.get('cover', '')})
            except Exception as exc:
                entry['status'] = f'ERROR {type(exc).__name__}'
            matches.append(entry)
            print(f"{row['comicid']:>3} {row['name'][:14]:<14} -> {entry['path_word'] or '-':<24} "
                  f"{entry['status']} score={entry['score']} src={entry['source_chapters']}", flush=True)
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
    print(f"matched {len(lines)}/{len(matches)}", flush=True)


if __name__ == '__main__':
    main()
