import json
import unittest
from platforms import bili_item, xhh_items, xhh_comments, number


class PlatformParsingTests(unittest.TestCase):
    def test_bili_search_and_detail_shapes(self):
        search = bili_item({'bvid': 'BV1234567890', 'title': '<em>新番</em>讨论',
                            'play': '1.2万', 'review': 42}, 'search:新番')
        self.assertEqual(search['title'], '新番 讨论')
        self.assertEqual(search['metrics'], {'views': 12000, 'comments': 42, 'likes': None})
        detail = bili_item({'bvid': 'BV1234567890', 'title': '新番讨论',
                            'stat': {'view': 20000, 'reply': 52, 'like': 300}}, 'popular')
        self.assertEqual(detail['metrics']['views'], 20000)
        self.assertIsNone(bili_item({'bvid': 'bad'}, 'popular'))

    def test_xhh_feed_and_detail_linkid_do_not_leak_account_fields(self):
        payload = {'result': {'links': [{'link': {'linkid': 123, 'title': '版本讨论',
            'text': '<p>新版本的玩法</p>', 'click': '2万', 'comment_num': 30, 'create_at': 1792700000,
            'user': {'token': 'SECRET', 'phone': 'SECRET'}}}], 'account': {'token': 'SECRET'}}}
        items = xhh_items(payload)
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]['metrics']['views'], 20000)
        self.assertNotIn('SECRET', json.dumps(items))
        self.assertNotIn('user', items[0])
        self.assertEqual(items[0]['content'], '新版本的玩法')

    def test_nested_comments_are_bounded_and_omit_usernames(self):
        payload = {'result': {'comments': [{'comment': {'commentid': i, 'text': '观点',
                   'user': {'username': 'SECRET'}, 'support_num': i}} for i in range(20)]}}
        rows = xhh_comments(payload, 3)
        self.assertEqual(len(rows), 3)
        self.assertNotIn('SECRET', json.dumps(rows))

    def test_metric_unknown_is_distinct_from_real_zero(self):
        self.assertIsNone(number('未知'))
        self.assertIsNone(number(None))
        self.assertEqual(number('0'), 0)


if __name__ == '__main__':
    unittest.main()
