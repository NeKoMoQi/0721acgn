"""Resolve the remaining unmatched comics: dump candidate path_words per query (ASCII output only)."""
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
TARGETS = {
    1: ['出包王女', '出包王女 Darkness', '妖神记'],
    2: ['異世界叔叔', '异世界舅舅'],
    9: ['關於我轉生後成為史萊姆的那件事', '關於我轉生變成史萊姆這檔事', '史萊姆'],
    14: ['譚雅戰記', '幼女戰記'],
    16: ['關於我在無意間被隔壁的天使變成廢柴這件事', '隔壁的天使'],
    35: ['總之就是非常可愛', '总之就是非常可爱'],
}


def main():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    driver = webdriver.Chrome(options=opts)
    out = {}
    try:
        for cid, queries in TARGETS.items():
            out[cid] = {}
            for q in queries:
                try:
                    driver.get(BASE + '/search?q=' + urllib.parse.quote(q))
                except Exception:
                    driver.execute_script('window.stop()')
                time.sleep(2.5)
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
                out[cid][q] = links[:8]
                print(f'comic {cid} query {len(q)} chars -> {len(links)} links: {links[:4]}', flush=True)
    finally:
        driver.quit()
    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-remaining.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print('written', flush=True)


if __name__ == '__main__':
    main()
