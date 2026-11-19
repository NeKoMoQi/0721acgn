"""Feasibility probe: render mangacopy with headless Chrome and read image src.

The site loads chapter images via obfuscated JS; a real browser exposes them as
img[data-src]. This checks that from a GitHub Actions runner, without login first.
"""
import json
import os
import sys
import time

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
COMIC = os.environ.get('COMIC_URL', f'{BASE}/comic/HAIBIANZHIYE')
USER = os.environ.get('COPY_USER', '')
PASSWORD = os.environ.get('COPY_PASS', '')


def build_driver():
    opts = Options()
    opts.add_argument('--headless=new')
    opts.add_argument('--disable-gpu')
    opts.add_argument('--no-sandbox')
    opts.add_argument('--window-size=1440,900')
    opts.add_argument('--lang=zh-CN')
    opts.add_argument('--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
                      'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
    return webdriver.Chrome(options=opts)


def login(driver):
    if not USER or not PASSWORD:
        return 'skipped (no credentials)'
    driver.get(f'{BASE}/web/login/loginByAccount?url=person%2Fhome')
    time.sleep(2)
    driver.find_element(By.XPATH, '//*[@id="pane-login"]/form/div[1]/div/div/input').send_keys(USER)
    driver.find_element(By.XPATH, '//*[@id="pane-login"]/form/div[2]/div/div/input').send_keys(PASSWORD)
    driver.execute_script('arguments[0].click()', driver.find_element(By.CLASS_NAME, 'el-button--primary'))
    time.sleep(2)
    return 'submitted'


def main():
    out = {'utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'comic': COMIC}
    driver = build_driver()
    try:
        try:
            driver.get(COMIC)
        except Exception:
            driver.execute_script('window.stop()')
        time.sleep(3)
        out['login'] = login(driver)
        if USER:
            driver.get(COMIC)
            time.sleep(3)

        html = driver.page_source
        soup = BeautifulSoup(html, 'html.parser')
        h6 = soup.find('h6')
        out['title'] = h6.get_text(strip=True) if h6 else ''
        box = soup.find(name='div', attrs={'id': 'default全部'})
        if box is None:
            # 有些版本用 class 或 id 前缀
            for div in soup.find_all('div', id=True):
                if div['id'].startswith('default'):
                    box = div
                    break
        links = []
        if box is not None:
            links = [BASE + a['href'] for a in box.find_all('a')
                     if a.get('href') and 'comic' in a['href']]
        out['chapter_count'] = len(links)
        out['chapter_sample'] = links[:3]

        if links:
            chapter = links[0]
            out['chapter'] = chapter
            try:
                driver.get(chapter)
            except Exception:
                driver.execute_script('window.stop()')
            time.sleep(3)
            for _ in range(8):
                driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.SPACE)
                time.sleep(0.3)
            time.sleep(2)
            soup2 = BeautifulSoup(driver.page_source, 'html.parser')
            imgs = [i.get('data-src') for i in soup2.find_all('img') if i.get('data-src')]
            out['img_count'] = len(imgs)
            out['img_sample'] = imgs[:3]
            counter = soup2.find('span', class_='comicCount')
            out['comicCount'] = counter.get_text(strip=True) if counter else None
            if imgs:
                import urllib.request
                req = urllib.request.Request(imgs[0], headers={
                    'User-Agent': 'Mozilla/5.0', 'Referer': chapter})
                with urllib.request.urlopen(req, timeout=30) as resp:
                    body = resp.read(200000)
                    out['first_image'] = {'status': resp.status,
                                          'type': resp.headers.get('Content-Type'),
                                          'bytes': len(body),
                                          'url': imgs[0][:120]}
    finally:
        driver.quit()

    os.makedirs('runs', exist_ok=True)
    with open('runs/copymanga-selenium.json', 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    print(json.dumps(out, ensure_ascii=False, indent=2)[:3000])


if __name__ == '__main__':
    main()
