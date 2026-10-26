import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import worker


class NewsroomTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.cfg = json.loads(Path(worker.__file__).with_name('config.example.json').read_text(encoding="utf-8"))
        self.cfg.update(data_dir=self.temp.name, official_domains=['official.example'], auto_publish=False,
                        verify_with_web=True, platforms=[])
        self.room = worker.Newsroom(self.cfg)
        self.date = worker.today()
        self.source = {'url': 'https://official.example/news/1', 'title': '作品新刊发布',
                       'content': '原始发布公告。' * 40, 'date': worker.stamp()}
        self.draft = {'title': '作品新刊发布', 'summary': '新刊公开了发售安排。',
                      'paragraphs': ['根据作品官方网站的公告，新刊已公布。'], 'source_ids': [1],
                      'event_key': '作品-新刊-' + self.date, 'event_date': self.date,
                      'kind': 'news', 'needs_review': False}

    def tearDown(self):
        self.room.close()
        self.temp.cleanup()

    def queue(self, url=None):
        self.room.remember({**self.source, 'url': url or self.source['url']}, '轻小说')
        return self.room.db.execute('SELECT * FROM candidates ORDER BY created DESC').fetchone()['id']

    def test_url_dedupe_survives_restart(self):
        self.queue('https://official.example/news/1?utm_source=bili')
        self.queue()
        self.room.close()
        self.room = worker.Newsroom(self.cfg)
        self.assertEqual(self.room.db.execute('SELECT count(*) FROM candidates').fetchone()[0], 1)

    def test_draft_is_private_until_approved_and_can_be_withdrawn(self):
        ident = self.queue()
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=self.draft):
            self.room.process()
        self.room.export()
        public = Path(self.temp.name) / 'published.json'
        self.assertEqual(json.loads(public.read_text(encoding="utf-8"))['articles'], [])
        self.room.review(ident, 'approve')
        article = json.loads(public.read_text(encoding="utf-8"))['articles'][0]
        self.assertEqual(article['title'], self.draft['title'])
        self.assertNotIn('content', article['sources'][0])
        self.room.review(ident, 'reject')
        self.assertEqual(json.loads(public.read_text(encoding="utf-8"))['articles'], [])

    def test_cross_url_event_dedupe(self):
        self.queue()
        self.queue('https://official.example/news/2')
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=self.draft):
            self.room.process()
        self.assertEqual(self.room.db.execute('SELECT count(*) FROM articles').fetchone()[0], 1)
        self.assertEqual(self.room.db.execute("SELECT count(*) FROM candidates WHERE state='duplicate'").fetchone()[0], 1)

    def test_failed_candidate_retries_without_blocking_other_candidates(self):
        self.queue()
        self.queue('https://official.example/news/2')
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', side_effect=[RuntimeError('offline'), self.draft]):
            self.room.process()
        rows = self.room.db.execute('SELECT * FROM candidates').fetchall()
        self.assertEqual(sum(r['attempts'] for r in rows), 1)
        self.assertEqual(self.room.db.execute('SELECT count(*) FROM articles').fetchone()[0], 1)
        failed = next(r for r in rows if r['attempts'])
        self.assertGreater(failed['next_try'], worker.time.time())

    def test_community_discussion_can_publish_without_official_domain(self):
        self.queue('https://bilibili.com/video/BV1234567890')
        self.cfg['auto_publish'] = True
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value={**self.draft, 'kind': 'discussion'}):
            self.room.process()
        self.assertEqual(self.room.db.execute("SELECT count(*) FROM articles WHERE status='published'").fetchone()[0], 1)

    def test_source_id_hallucinations_rejected(self):
        sources = [{'id': 1, 'is_official': True, **self.source}]
        for ids in ([999], [True], [], [1, 999]):
            with self.assertRaises(ValueError):
                self.room.validate({**self.draft, 'source_ids': ids}, sources)

    def test_old_and_future_events_rejected(self):
        for date in ('2020-01-01', '2099-01-01'):
            with self.assertRaises(ValueError):
                self.room.validate({**self.draft, 'event_date': date}, [{'id': 1, 'is_official': True, **self.source}])

    def test_daily_api_cap_counts_failures_and_persists(self):
        self.cfg['max_api_calls_per_day'] = 1
        with patch.dict(os.environ, {'DEEPSEEK_API_KEY': 'test-key'}), patch('urllib.request.urlopen', side_effect=worker.urllib.error.HTTPError('x', 401, '', {}, None)):
            with self.assertRaisesRegex(RuntimeError, '401'):
                self.room.api.post('/v1/tools/search', {})
            with self.assertRaises(worker.BudgetExceeded):
                self.room.api.post('/v1/tools/search', {})
        self.assertEqual(self.room.status()['budget']['calls'], 1)

    def test_daily_draft_cap(self):
        self.cfg['max_drafts_per_day'] = 1
        self.queue()
        self.queue('https://official.example/news/2')
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=self.draft) as write:
            self.room.process()
        self.assertEqual(write.call_count, 1)

    def test_auto_publish_is_opt_in(self):
        self.cfg['auto_publish'] = True
        self.queue()
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=self.draft):
            self.room.process()
        self.room.export()
        self.assertEqual(len(json.loads((Path(self.temp.name) / 'published.json').read_text(encoding="utf-8"))['articles']), 1)

    def test_domain_matching_is_not_suffix_spoofable(self):
        self.assertFalse(worker.on_domain('https://official.example.evil.test/', ['official.example']))
        self.assertTrue(worker.on_domain('https://news.official.example/', ['official.example']))

    def test_report_import_is_idempotent(self):
        self.cfg['report_dir'] = self.temp.name
        (Path(self.temp.name) / 'report.md').write_text('### BV1234567890\n线索文本', encoding='utf-8')
        self.room.import_reports()
        self.room.import_reports()
        self.assertEqual(self.room.db.execute('SELECT count(*) FROM candidates').fetchone()[0], 1)

    def test_review_html_escapes_external_text(self):
        self.queue()
        draft = {**self.draft, 'title': '<script>alert(1)</script>'}
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=draft):
            self.room.process()
        self.room.review_page()
        page = (Path(self.temp.name) / 'review.html').read_text(encoding='utf-8')
        self.assertNotIn('<script>', page)
        self.assertIn('&lt;script&gt;', page)

    def test_lock_is_released_after_exception(self):
        with self.assertRaises(ValueError):
            with worker.exclusive_lock(self.temp.name):
                raise ValueError('crash')
        with worker.exclusive_lock(self.temp.name):
            pass

    def test_unverified_news_and_rumor_are_held_but_discussion_is_not(self):
        sources = [{'id': 1, 'is_official': False, **self.source}]
        news = self.room.validate(self.draft, sources)
        self.assertTrue(news['needs_review'])
        discussion = self.room.validate({**self.draft, 'kind': 'discussion'}, sources)
        self.assertFalse(discussion['needs_review'])
        rumor = self.room.validate({**self.draft, 'kind': 'rumor'}, sources)
        self.assertTrue(rumor['needs_review'])
        self.assertIn('未证实', rumor['title'])

    def test_unknown_event_date_is_not_invented(self):
        value = self.room.validate({**self.draft, 'event_date': '', 'kind': 'discussion'},
                                  [{'id': 1, 'is_official': False, **self.source}])
        self.assertEqual(value['event_date'], '')

    def test_metric_growth_persists_and_missing_metrics_are_not_zero(self):
        source = {**self.source, 'metrics': {'views': 100, 'comments': 5, 'likes': None}}
        with patch('worker.time.time', return_value=10000):
            self.room.remember(source, '游戏')
        self.room.close()
        self.room = worker.Newsroom(self.cfg)
        with patch('worker.time.time', return_value=13600):
            self.room.remember({**source, 'metrics': {'views': 160, 'comments': 8, 'likes': None}}, '游戏')
        payload = json.loads(self.room.db.execute('SELECT payload FROM candidates').fetchone()[0])
        self.assertEqual(payload['growth_per_hour'], {'views': 60.0, 'comments': 3.0})
        self.assertIsNone(payload['metrics']['likes'])

    def test_metric_refresh_preserves_transcript_and_terminal_state(self):
        self.room.remember({**self.source, 'content_type': 'subtitle', 'metrics': {'views': 10}}, '动画')
        self.room.db.execute("UPDATE candidates SET state='done'")
        self.room.db.commit()
        self.room.remember({**self.source, 'content': '', 'metrics': {'views': 20}}, '动画')
        row = self.room.db.execute('SELECT * FROM candidates').fetchone()
        self.assertEqual(row['state'], 'done')
        self.assertEqual(json.loads(row['payload'])['content'], self.source['content'])

    def test_platform_failure_is_visible_without_substituting_web_search(self):
        self.cfg['platforms'] = ['bilibili']
        collector = unittest.mock.Mock()
        collector.collect.return_value = ([], [{'operation': 'popular', 'error': 'HTTP412'}])
        self.room.collectors['bilibili'] = collector
        with patch.object(self.room.api, 'search') as search:
            with self.assertRaisesRegex(RuntimeError, 'collection failed'):
                self.room.discover()
        search.assert_not_called()
        self.assertEqual(self.room.status()['platforms']['bilibili']['state'], 'error')

    def test_generated_images_exist_before_article_is_published(self):
        self.queue()
        self.cfg['auto_publish'] = True
        with patch.object(self.room.api, 'search', return_value=[]), patch.object(self.room.api, 'write', return_value=self.draft):
            self.room.process()
        row = self.room.db.execute('SELECT * FROM articles').fetchone()
        images = json.loads(row['payload'])['images']
        self.assertTrue(images)
        self.assertEqual((Path(self.temp.name) / 'media' / (row['id'] + '-cover.png')).read_bytes()[:8], b'\x89PNG\r\n\x1a\n')

    def test_full_platform_to_public_snapshot_without_official_domains(self):
        self.cfg.update(platforms=['bilibili'], official_domains=[], auto_publish=True, verify_with_web=False)
        collector = unittest.mock.Mock()
        item = {**self.source, 'url': 'https://www.bilibili.com/video/BV1234567890',
                'platform': 'bilibili', 'platform_id': 'BV1234567890', 'content': '',
                'metrics': {'views': 12000, 'comments': 30, 'likes': 400}}
        collector.collect.return_value = ([item], [])
        collector.enrich.return_value = {**item, 'content': '这是一份实际内容形状的字幕材料。' * 30,
                                        'content_type': 'subtitle', 'comments': [{'text': '一条观点', 'likes': 3}]}
        self.room.collectors['bilibili'] = collector
        with patch.object(self.room.api, 'write', return_value={**self.draft, 'kind': 'discussion'}), patch.object(self.room.api, 'search') as search:
            self.room.round()
        search.assert_not_called()
        collector.enrich.assert_called_once()
        articles = json.loads((Path(self.temp.name) / 'published.json').read_text(encoding='utf-8'))['articles']
        self.assertEqual(len(articles), 1)
        self.assertEqual(articles[0]['kind'], 'discussion')
        self.assertEqual(len(articles[0]['images']), 2)
        self.assertNotIn('content', articles[0]['sources'][0])

    def test_optional_official_lookup_outage_does_not_block_discussion(self):
        self.queue('https://bilibili.com/video/BV1234567890')
        self.cfg['auto_publish'] = True
        with patch.object(self.room.api, 'search', side_effect=RuntimeError('offline')), patch.object(self.room.api, 'write', return_value={**self.draft, 'kind': 'discussion'}):
            self.room.process()
        self.assertEqual(self.room.db.execute("SELECT count(*) FROM articles WHERE status='published'").fetchone()[0], 1)

    def test_report_bvid_is_read_through_platform_collector(self):
        self.cfg['report_dir'] = self.temp.name
        (Path(self.temp.name) / 'daily.md').write_text('### BV1234567890\n线索', encoding='utf-8')
        self.room.import_reports()
        collector = unittest.mock.Mock()
        collector.enrich.return_value = {'content': '已读取的真实字幕。' * 30, 'title': '作品讨论', 'content_type': 'subtitle'}
        self.room.collectors['bilibili'] = collector
        row = self.room.db.execute('SELECT * FROM candidates').fetchone()
        with patch.object(self.room.api, 'search', return_value=[]):
            sources = self.room.evidence(row)
        collector.enrich.assert_called_once()
        self.assertEqual(sources[0]['content_type'], 'subtitle')

    def test_news_may_use_two_independent_authoritative_sources(self):
        sources = [{'id': i, 'is_official': False, 'authority': 3,
                    **self.source, 'url': f'https://publisher{i}.example/news'} for i in (1, 2)]
        value = self.room.validate({**self.draft, 'source_ids': [1, 2]}, sources)
        self.assertFalse(value['needs_review'])


if __name__ == '__main__':
    unittest.main()

