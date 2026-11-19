"""Selenium-based copymanga crawler (inspired by muzi-xiaoren/kaobei_Crawler).

Renders the comic/chapter pages with headless Chrome (the site's images are
decrypted client-side), reads img[data-src], downloads them and uploads to OSS.
Per-comic manifest (chapter -> page -> OSS url) is stored in OSS so the server can
import it and so reruns only fetch new chapters.

    python crawl_selenium.py --comic HAIBIANZHIYE --max-chapters 1
"""
import argparse
import json
import os
import sys
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
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

BASE = os.environ.get('COPY_BASE', 'https://www.mangacopy.com').rstrip('/')
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36')
USER = os.environ.get('COPY_USER', '')
PASSWORD = os.environ.get('COPY_PASS', '')
PREFIX = 'copymanga'
MANIFEST_PREFIX = f'{PREFIX}/manifests'


def build_driver():
    opts = Options()
    for arg in ('--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900',
                '--lang=zh-CN', f'--user-agent={UA}'):
        opts.add_argument(arg)
    return webdriver.Chrome(options=opts)


def login(driver):
    if not USER or not PASSWORD:
        return 'skipped'
    try:
        driver.get(f'{BASE}/web/login/loginByAccount?url=person%2Fhome')
        time.sleep(2)
        driver.find_element(By.XPATH, '//*[@id="pane-login"]/form/div[1]/div/div/input').send_keys(USER)
        driver.find_element(By.XPATH, '//*[@id="pane-login"]/form/div[2]/div/div/input').send_keys(PASSWORD)
        driver.execute_script('arguments[0].click()', driver.find_element(By.CLASS_NAME, 'el-button--primary'))
        time.sleep(2)
        return 'submitted'
    except Exception as exc:
        return f'failed: {type(exc).__name__}'


def ext_of(body: bytes, url: str) -> str:
    if body.startswith(b'\xff\xd8'):
        return 'jpg'
    if body.startswith(b'\x89PNG'):
        return 'png'
    if body[:4] == b'RIFF' and body[8:12] == b'WEBP':
        return 'webp'
    suffix = url.split('?')[0].rsplit('.', 1)[-1].lower()
    return suffix if suffix in ('jpg', 'jpeg', 'png', 'webp') else 'jpg'


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--comic', required=True, help='path_word or full comic URL')
    p.add_argument('--max-chapters', type=int, default=0, help='new chapters this run (0 = all)')
    p.add_argument('--start', type=int, default=1, help='first chapter index (1-based)')
    p.add_argument('--threads', type=int, default=8)
    p.add_argument('--dry-run', action='store_true')
    args = p.parse_args()

    path_word = args.comic.rstrip('/').split('/')[-1]
    comic_url = args.comic if args.comic.startswith('http') else f'{BASE}/comic/{path_word}'

    import oss2
    bucket = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                         os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
    public = os.environ['OSS_PUBLIC_BASE'].rstrip('/')
    local = threading.local()

    def tl_bucket():
        if not hasattr(local, 'b'):
            local.b = oss2.Bucket(oss2.Auth(os.environ['OSS_ACCESS_KEY_ID'], os.environ['OSS_ACCESS_KEY_SECRET']),
                                  os.environ['OSS_ENDPOINT'], os.environ['OSS_BUCKET'])
        return local.b

    manifest_key = f'{MANIFEST_PREFIX}/{path_word}.json'
    manifest = {'path_word': path_word, 'comic_url': comic_url, 'chapters': []}
    try:
        manifest = json.loads(bucket.get_object(manifest_key).read().decode('utf-8'))
        print(f'manifest: {len(manifest.get("chapters", []))} chapters', flush=True)
    except Exception:
        print('manifest: new', flush=True)
    done = {c['uuid']: c for c in manifest.get('chapters', [])}

    driver = build_driver()
    try:
        login(driver)
        try:
            driver.get(comic_url)
        except Exception:
            driver.execute_script('window.stop()')
        time.sleep(3)
        soup = BeautifulSoup(driver.page_source, 'html.parser')
        h6 = soup.find('h6')
        title = h6.get_text(strip=True) if h6 else path_word
        cover = ''
        for img in soup.find_all('img'):
            src = img.get('data-src') or img.get('src') or ''
            if '/cover/' in src:
                cover = src
                break
        box = soup.find('div', id='default全部')
        if box is None:
            for div in soup.find_all('div', id=True):
                if div['id'].startswith('default'):
                    box = div
                    break
        entries = []
        if box is not None:
            for a in box.find_all('a'):
                href = a.get('href') or ''
                if 'chapter' not in href:
                    continue
                entries.append({'url': BASE + href, 'name': a.get_text(strip=True)})
        print(f'comic={title} chapters={len(entries)}', flush=True)
        manifest.update({'title': title, 'cover': cover, 'chapter_total': len(entries)})

        todo = []
        for idx, entry in enumerate(entries, 1):
            uuid = entry['url'].rstrip('/').split('/')[-1]
            if idx < args.start:
                continue
            if uuid in done:
                continue
            todo.append({'index': idx, 'uuid': uuid, **entry})
            if args.max_chapters and len(todo) >= args.max_chapters:
                break
        print(f'to fetch: {len(todo)} chapters', flush=True)

        for item in todo:
            chapter_started = time.time()
            try:
                driver.get(item['url'])
            except Exception:
                driver.execute_script('window.stop()')
            time.sleep(2.5)
            expected = 0
            imgs = []
            for attempt in range(10):
                soup = BeautifulSoup(driver.page_source, 'html.parser')
                counter = soup.find('span', class_='comicCount')
                try:
                    expected = int(counter.get_text(strip=True)) if counter else 0
                except ValueError:
                    expected = 0
                imgs = [i.get('data-src') for i in soup.find_all('img') if i.get('data-src')]
                if expected and len(imgs) >= expected:
                    break
                for _ in range(6):
                    driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.SPACE)
                    time.sleep(0.15)
                time.sleep(1.2)
            print(f'chapter {item["index"]} imgs={len(imgs)} expected={expected}', flush=True)
            if not imgs:
                print(f'chapter {item["index"]}: no images, skipped', flush=True)
                continue

            pages = [None] * len(imgs)
            errors = []

            def fetch(i, url):
                try:
                    req = urllib.request.Request(url, headers={'User-Agent': UA, 'Referer': item['url']})
                    with urllib.request.urlopen(req, timeout=40) as resp:
                        body = resp.read()
                    if not body:
                        raise ValueError('empty')
                    key = f'{PREFIX}/images/{path_word}/{item["uuid"]}/{i:03d}.{ext_of(body, url)}'
                    tl_bucket().put_object(key, body, headers={'Content-Type': 'image/' +
                                                               ('jpeg' if key.endswith('jpg') else key.rsplit('.', 1)[-1])})
                    return i, {'page': i, 'url': f'{public}/{key}', 'bytes': len(body)}
                except Exception as exc:
                    with threading.Lock():
                        errors.append(f'{i}:{type(exc).__name__}')
                    return i, None

            with ThreadPoolExecutor(max_workers=args.threads) as pool:
                futures = [pool.submit(fetch, i + 1, u) for i, u in enumerate(imgs)]
                for fut in as_completed(futures):
                    i, info = fut.result()
                    if info:
                        pages[i - 1] = info

            pages = [x for x in pages if x]
            if expected and len(pages) < expected:
                print(f'chapter {item["index"]}: incomplete {len(pages)}/{expected}', flush=True)
                continue
            chapter = {'index': item['index'], 'uuid': item['uuid'], 'name': item['name'],
                       'pages': pages, 'fetched_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
                       'seconds': round(time.time() - chapter_started, 1)}
            if errors:
                chapter['errors'] = errors[:5]
            done[item['uuid']] = chapter
            manifest['chapters'] = sorted(done.values(), key=lambda c: c['index'])
            manifest['updated_at'] = chapter['fetched_at']
            bucket.put_object(manifest_key, json.dumps(manifest, ensure_ascii=False).encode('utf-8'),
                              headers={'Content-Type': 'application/json'})
            print(f'chapter {item["index"]} saved ({len(pages)} pages, {chapter["seconds"]}s)', flush=True)
    finally:
        driver.quit()

    summary = {'path_word': path_word, 'title': manifest.get('title'),
               'chapters_done': len(manifest.get('chapters', [])),
               'chapter_total': manifest.get('chapter_total'),
               'pages': sum(len(c.get('pages', [])) for c in manifest.get('chapters', []))}
    os.makedirs('runs', exist_ok=True)
    Path('runs/copymanga-crawl.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(summary, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
