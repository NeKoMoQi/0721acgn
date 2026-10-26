import { beforeEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))
import request from '@/utils/request'
import News from '../index.vue'

const article = {
  id: '0123456789abcdef01234567', title: '新刊消息', summary: '新刊发售安排',
  paragraphs: ['<script>恶意正文</script>'], published_at: '2026-10-23T00:00:00Z',
  event_date: '2026-10-23', sources: [{ title: '官方网站', url: 'javascript:alert(1)' }],
}
beforeEach(() => vi.mocked(request.get).mockReset())
async function setup(path = '/news') {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/news/:id?', component: News }] })
  await router.push(path)
  const view = mount(News, { global: { plugins: [router] } })
  await flushPromises()
  return { view, router }
}
it('shows empty state without invented content', async () => {
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: { items: [], total: 0 } })
  const { view } = await setup()
  expect(view.text()).toContain('暂时还没有发布的资讯')
  expect(view.findAll('.news-card')).toHaveLength(0)
  view.unmount()
})
it('escapes model text and refuses unsafe source links', async () => {
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: article })
  const { view } = await setup('/news/' + article.id)
  expect(view.text()).toContain('<script>恶意正文</script>')
  expect(view.find('script').exists()).toBe(false)
  expect(view.find('footer a').attributes('href')).toBe('#')
  view.unmount()
})
it('paginates server results', async () => {
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: { items: [article], total: 13 } })
  const { view } = await setup()
  await view.findAll('.pagination button')[1].trigger('click')
  await flushPromises()
  expect(request.get).toHaveBeenLastCalledWith('/news', { params: { page: 2 } })
  view.unmount()
})
it('offers retry after a network failure', async () => {
  vi.mocked(request.get).mockRejectedValueOnce(new Error('offline'))
  const { view } = await setup()
  expect(view.find('[role="alert"]').exists()).toBe(true)
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: { items: [], total: 0 } })
  await view.find('button').trigger('click')
  await flushPromises()
  expect(view.text()).toContain('暂时还没有发布的资讯')
  view.unmount()
})
it('shows generated cover and statistics images with credits', async () => {
  const images = [{ role: 'cover', url: `/api/news/image?id=${article.id}&role=cover`, caption: '封面', credit: '原创插画' },
    { role: 'stats', url: `/api/news/image?id=${article.id}&role=stats`, caption: '统计图', credit: '站内制图' }]
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: { ...article, kind: 'discussion', images } })
  const { view } = await setup('/news/' + article.id)
  expect(view.find('.article-cover').attributes('src')).toBe(images[0].url)
  expect(view.find('.inline-image').attributes('src')).toBe(images[1].url)
  expect(view.text()).toContain('社区观察')
  expect(view.text()).toContain('原创插画')
  await view.find('.article-cover').trigger('error')
  expect(view.find('.detail-cover').exists()).toBe(true)
  view.unmount()
})
