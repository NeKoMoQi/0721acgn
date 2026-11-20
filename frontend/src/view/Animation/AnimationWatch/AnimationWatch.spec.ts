import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import Watch from './index.vue'
import { getAnimationById, getAnimationEpisodeById, getAnimationEpisodes } from '@/api/animation'

const query = vi.hoisted(() => ({ animationid: '3', episodeid: undefined as string | undefined }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query }), useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/api/animation', () => ({
  getAnimationById: vi.fn(), getAnimationEpisodeById: vi.fn(), getAnimationEpisodes: vi.fn(),
}))
vi.mock('@/utils/acgnLocal', () => ({
  ensureAcgnUserHydrated: vi.fn(), isInBookshelf: vi.fn(), pushHistory: vi.fn(),
  saveAnimationEpisode: vi.fn(), toggleBookshelfAsync: vi.fn(),
}))
vi.mock('@/components/AcgnCommunityPanel.vue', () => ({ default: { template: '<div />' } }))

const episode = { id: 8, animationId: 3, episodeNo: 1, videoUrl: '/episode.mp4' }
function mountWatch() {
  return mount(Watch, { global: { stubs: { Headpage: true, Behind: true, RouterLink: true } } })
}
beforeEach(() => {
  vi.resetAllMocks()
  query.episodeid = undefined
  vi.mocked(getAnimationById).mockResolvedValue({ code: 200, data: { name: '测试番剧' } })
  vi.mocked(getAnimationEpisodes).mockResolvedValue({ code: 200, data: [episode] })
})

it('等待请求时显示加载，分集请求失败后重试原分集，再支持播放器重试', async () => {
  query.episodeid = '8'
  vi.mocked(getAnimationEpisodeById)
    .mockRejectedValueOnce(new Error('offline'))
    .mockResolvedValueOnce({ code: 200, data: episode })
  const view = mountWatch()
  expect(view.get('[role="status"]').text()).toContain('正在加载')
  await flushPromises()
  expect(view.get('[role="alert"]').text()).toBe('番剧加载失败')
  await view.get('.empty-state button').trigger('click')
  await flushPromises()
  expect(vi.mocked(getAnimationEpisodeById).mock.calls).toEqual([[8], [8]])
  expect(view.get('video').attributes('src')).toBe('/episode.mp4')
  await view.get('video').trigger('error')
  expect(view.get('[role="alert"]').text()).toContain('视频加载失败')
  await view.get('.playback-error button').trigger('click')
  expect(view.get('video').attributes('src')).toBe('/episode.mp4')
  view.unmount()
})

it('成功返回空分集列表时显示缺资源，隐藏重试', async () => {
  vi.mocked(getAnimationEpisodes).mockResolvedValue({ code: 200, data: [] })
  const view = mountWatch()
  await flushPromises()
  expect(view.get('.empty-state h1').text()).toBe('暂无视频资源')
  expect(view.find('.empty-state button').exists()).toBe(false)
  view.unmount()
})
