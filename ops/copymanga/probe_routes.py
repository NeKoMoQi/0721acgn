"""Probe search/category routes on mangacopy to find a usable catalog source."""
import json
import os
import sys
import time
import urllib.parse

from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE = 'https://www.mangacopy.com'
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
KW = '魔女之旅'
Q = urllib.parse.quote(KW)

ROUTES = [
    f'/search?keyword={Q}',
    f'/search?q={Q}',
    f'/search?key={Q}',
    f'/search?word={Q}',
    f'/comics?keyword={Q}',
    f'/comics?search={Q}',
    f'/comics?q={Q}',
    '/comics?theme=aiqing',
    '/comics',
    '/filter',
    '/rank',
]


def build_driver():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    return webdriver.Chrome(options=opts)


def links_of(html):
    soup = BeautifulSoup(html, 'html.parser')
    out = []
    seen = set()
    for a in soup.find_all('a', href=True):
        href = a['href']
        if '/comic/' not in href:
            continue
        pw = href.rstrip('/').split('/')[-1]
        if pw in seen:
            continue
        seen.add(pw)
        title = a.get_text(' ', strip=True)
        if not title:
            img = a.find('img')
            title = (img.get('alt') or img.get('title') or '') if img else ''
        if not title:
            parent = a.find_parent(['li', 'div', 'p'])
            if parent:
                title = parent.get_text(' ', strip=True)[:40]
        out.append({'path_word': pw, 'title': title, 'href': href})
    pages = sorted({h for h in (a['href'] for a in soup.find_all('a', href=True)) if 'page=' in h})
    return out, pages[:10]


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'routes': {}}
    driver = build_driver()
    try:
        driver.get(BASE + '/')
        time.sleep(2.5)
        out['forms'] = driver.execute_script("""
          return Array.from(document.querySelectorAll('form')).map(function(f){
            return {action: f.getAttribute('action'), method: f.getAttribute('method'),
                    html: (f.outerHTML||'').slice(0,280)};});
        """)
        out['search_inputs'] = driver.execute_script("""
          return Array.from(document.querySelectorAll('input')).filter(function(i){
            return (i.getAttribute('placeholder')||'').match(/搜索|漫畫|漫画|關鍵|key/i) ||
                   (i.getAttribute('name')||'').match(/key|search|q/i);
          }).map(function(i){return {name:i.getAttribute('name'), id:i.getAttribute('id'),
            placeholder:i.getAttribute('placeholder'), html:(i.outerHTML||'').slice(0,200)};});
        """)
        for route in ROUTES:
            item = {}
            try:
                driver.get(BASE + route)
                time.sleep(2.5)
                html = driver.page_source
                links, pages = links_of(html)
                text = BeautifulSoup(html, 'html.parser').get_text(' ', strip=True)
                item = {'url': driver.current_url, 'count': len(links), 'pages': pages[:5],
                        'keyword_present': KW in text, 'sample': links[:5]}
            except Exception as exc:
                item = {'error': f'{type(exc).__name__}: {exc}'}
            out['routes'][route] = item
            print(route, json.dumps(item, ensure_ascii=False)[:300], flush=True)
    finally:
        driver.quit()

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-routes.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print('written runs/copymanga-routes.json', flush=True)


if __name__ == '__main__':
    main()
