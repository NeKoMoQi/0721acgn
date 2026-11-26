// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { getcartoonchapter } from '@/api/cartoon'
import { listNotifications, unreadNotificationCount } from '@/utils/acgnNotifications'
import { checkSubscription } from '@/utils/subscriptionUpdates'

const { entry } = vi.hoisted(() => ({
  entry: { type: 'comic' as const, id: 24, name: '葬送的芙莉莲', subscribedAt: 1 },
}))
vi.mock('@/api/animation', () => ({ getAnimationEpisodes: vi.fn() }))
vi.mock('@/api/cartoon', () => ({ getcartoonchapter: vi.fn() }))
vi.mock('@/api/novel', () => ({ getnovelchapter: vi.fn() }))
vi.mock('@/utils/acgnLocal', () => ({
  listSubscriptions: () => [entry],
  ensureAcgnUserHydrated: vi.fn(),
}))

beforeEach(() => localStorage.clear())
afterEach(() => vi.restoreAllMocks())

it('空浏览器没有未读红点，旧演示消息也不再计入', () => {
  expect(unreadNotificationCount()).toBe(0)
  localStorage.setItem('acgn_notifications_v1', JSON.stringify([
    { id: 'demo_reply', kind: 'comment_reply', title: '评论回复', read: false },
    { id: 'demo_sub', kind: 'subscribe_update', title: '订阅更新', read: false },
  ]))
  expect(listNotifications()).toEqual([])
  expect(unreadNotificationCount()).toBe(0)
})

it('只有实际开放正文的漫画新话才出现通知，并保留目标章号', async () => {
  let now = 1_000_000
  vi.spyOn(Date, 'now').mockImplementation(() => now)
  const past = { chapterid: 'before', chapterno: 151, available: true }
  const next = { chapterid: 'next', chapterno: 152, available: false }
  vi.mocked(getcartoonchapter)
    .mockResolvedValueOnce({ code: 200, data: [past, next] })
    .mockResolvedValueOnce({ code: 200, data: [past, { ...next, available: true }] })

  await checkSubscription(entry)
  expect(listNotifications()).toEqual([])
  now += 301_000
  await checkSubscription(entry)
  expect(listNotifications()).toEqual([expect.objectContaining({
    kind: 'subscribe_update',
    to: { name: 'Comicsee', query: { comicid: 24, chapterid: 'next', chapterno: 152 } },
    read: false,
  })])
})
