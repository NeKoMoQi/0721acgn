"""Dump search page state for specific queries (why no results?)."""
import json
import os
import sys
import time
import urllib.parse

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
QUERIES = ['異世界叔叔', '异世界舅舅', '關於我轉生後成為史萊姆的那件事', '史萊姆', '轉生史萊姆']


def main():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    driver = webdriver.Chrome(options=opts)
    out = {}
    try:
        for q in QUERIES:
            url = BASE + '/search?q=' + urllib.parse.quote(q)
            try:
                driver.get(url)
            except Exception:
                driver.execute_script('window.stop()')
            time.sleep(3)
            try:
                Alert(driver).accept()
            except Exception:
                pass
            soup = BeautifulSoup(driver.page_source, 'html.parser')
            links = []
            for a in soup.find_all('a', href=True):
                if '/comic/' in a['href']:
                    pw = a['href'].rstrip('/').split('/')[-1]
                    if pw not in links:
                        links.append(pw)
            text = soup.get_text(' ', strip=True)
            out[q] = {'url': driver.current_url, 'count': len(links), 'links': links[:6],
                      'text': text[:300]}
            print(q, '=>', out[q], flush=True)
    finally:
        driver.quit()
    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-query.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)


if __name__ == '__main__':
    main()
