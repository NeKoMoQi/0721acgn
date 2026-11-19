"""Find the working search URL on mangacopy (form action or candidate routes)."""
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
KEYWORD = '魔女之旅'


def build_driver():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    return webdriver.Chrome(options=opts)


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'keyword': KEYWORD}
    driver = build_driver()
    try:
        driver.get(BASE + '/')
        time.sleep(3)
        forms = driver.execute_script("""
          return Array.from(document.querySelectorAll('form')).map(function(f){
            return {action: f.getAttribute('action'), method: f.getAttribute('method'),
                    inputs: Array.from(f.querySelectorAll('input')).map(function(i){
                      return {name: i.getAttribute('name'), type: i.getAttribute('type'),
                              placeholder: i.getAttribute('placeholder')};})};
          });
        """)
        out['forms'] = forms
        inputs = driver.execute_script("""
          return Array.from(document.querySelectorAll('input')).slice(0,20).map(function(i){
            return {name: i.getAttribute('name'), id: i.getAttribute('id'), type: i.getAttribute('type'),
                    placeholder: i.getAttribute('placeholder')};});
        """)
        out['inputs'] = inputs
        out['search_links'] = driver.execute_script("""
          return Array.from(document.querySelectorAll('a[href*="search"],a[href*="keyword"]'))
            .slice(0,10).map(function(a){return a.getAttribute('href');});
        """)

        candidates = [
            f'/search?q={urllib.parse.quote(KEYWORD)}',
            f'/search?keyword={urllib.parse.quote(KEYWORD)}',
            f'/search?key={urllib.parse.quote(KEYWORD)}',
            f'/search?word={urllib.parse.quote(KEYWORD)}',
            f'/search/{urllib.parse.quote(KEYWORD)}',
            f'/comics?keyword={urllib.parse.quote(KEYWORD)}',
            f'/comics?search={urllib.parse.quote(KEYWORD)}',
            f'/web/search?q={urllib.parse.quote(KEYWORD)}',
        ]
        results = {}
        for path in candidates:
            driver.get(BASE + path)
            time.sleep(2.5)
            soup = BeautifulSoup(driver.page_source, 'html.parser')
            links = [a['href'] for a in soup.find_all('a', href=True) if '/comic/' in a['href']]
            body = soup.get_text(' ', strip=True)
            results[path] = {
                'url': driver.current_url,
                'title': soup.title.get_text(strip=True) if soup.title else '',
                'comic_links': len(links),
                'keyword_present': KEYWORD in body,
                'first_links': links[:3],
            }
            print(path, results[path], flush=True)
        out['candidates'] = results
    finally:
        driver.quit()

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-search.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps({k: (v['comic_links'], v['keyword_present'], v['url']) for k, v in out['candidates'].items()},
                     ensure_ascii=False))


if __name__ == '__main__':
    main()
