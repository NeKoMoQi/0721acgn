import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Home from '@/view/main/index.vue'
import Search from '@/view/Search/index.vue'
import Animation from '@/view/Animation/index.vue'
import { getAllActivity, getpageActivity } from '@/api/activity'
import { getAllSearch } from '@/api/AllSearch'
import { getAnimation, getAnimationhot } from '@/api/animation'

vi.mock('vue-router', async (importOriginal) => ({
  ...await importOriginal<typeof import('vue-router')>(),
  useRoute: () => ({ fullPath: '/search?q=作品', query: { q: '作品' } }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))
vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<header />' } }))
vi.mock('@/components/behind/index.vue', () => ({ default: { template: '<footer />' } }))
vi.mock('@/utils/request', () => ({ default: { get: vi.fn().mockResolvedValue({ code: 200, data: { items: [] } }) } }))
vi.mock('@/api/activity', () => ({
  getAllActivity: vi.fn(),
  getpageActivity: vi.fn(),
  getActivityHot: vi.fn(),
  getActivityLeibie: vi.fn(),
}))
vi.mock('@/api/peoplecount', () => ({ getPeopleCount: vi.fn() }))
vi.mock('@/api/acgnuser/index', () => ({ getAcgnUserBirth: vi.fn(), getAcgnUserRandom: vi.fn() }))
vi.mock('@/api/shengdi/index', () => ({ getShengDiHot: vi.fn() }))
vi.mock('@/api/AllSearch', () => ({ getAllSearch: vi.fn() }))
vi.mock('@/api/animation', () => ({ getAnimation: vi.fn(), getAnimationhot: vi.fn() }))
vi.mock('@/api/cartoon', () => ({ getcartoonhot: vi.fn().mockResolvedValue({ code: 200, data: [] }) }))
vi.mock('@/api/novel', () => ({ getnovelhot: vi.fn().mockResolvedValue({ code: 200, data: [] }) }))

const global = {
  stubs: {
    Headpage: true,
    RouterLink: { template: '<a><slot /></a>' },
    Behind: true,
    SvgIcon: true,
    Right: true,
    AcgnFilterBar: true,
    ElIcon: true,
    ElInput: true,
    ElSelect: true,
    ElOption: true,
    ElCarousel: { template: '<div><slot /></div>' },
    ElCarouselItem: { template: '<div><slot /></div>' },
    ElButton: { template: '<button><slot /></button>' },
  },
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(getAnimationhot).mockResolvedValue({ code: 200, data: [] })
})

describe('公开列表', () => {
  it('请求失败后从同一页重试，并保留首页已加载的活动', async () => {
    const first = Array.from({ length: 9 }, (_, i) => ({ activityname: `活动 ${i + 1}` }))
    vi.mocked(getAllActivity).mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ code: 200, data: first })
    vi.mocked(getpageActivity).mockResolvedValueOnce({ code: 0, data: null })
      .mockResolvedValueOnce({ code: 200, data: [{ activityname: '活动 10' }] })
    const wrapper = mount(Home, { global })
    try {
      await flushPromises()
      expect(wrapper.get('[role="status"]').text()).toContain('活动加载失败')
      await wrapper.get('[role="status"] button').trigger('click')
      await flushPromises()
      expect(wrapper.findAll('.zi1')).toHaveLength(9)
      await wrapper.get('.zixun').trigger('scroll')
      await flushPromises()
      expect(wrapper.findAll('.zi1')).toHaveLength(9)
      await wrapper.get('[role="status"] button').trigger('click')
      await flushPromises()
      expect(vi.mocked(getpageActivity).mock.calls).toEqual([[2], [2]])
      expect(wrapper.findAll('.zi1')).toHaveLength(10)
    } finally {
      wrapper.unmount()
    }
  })

  it('搜索错误显示重试，成功但无匹配时才展示空结果', async () => {
    vi.mocked(getAllSearch).mockResolvedValueOnce({ code: 0, data: null })
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ code: 200, data: [] })
    const wrapper = mount(Search, { global })
    try {
      await flushPromises()
      expect(wrapper.text()).not.toContain('暂无匹配结果')
      await wrapper.get('[role="status"] button').trigger('click')
      await flushPromises()
      expect(wrapper.get('[role="status"]').text()).toContain('搜索暂时无法完成')
      await wrapper.get('[role="status"] button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('暂无匹配结果')
      expect(getAllSearch).toHaveBeenLastCalledWith(expect.objectContaining({ search: '作品' }))
    } finally {
      wrapper.unmount()
    }
  })

  it('番剧榜单使用后端分集数，旧接口缺字段时省略状态', async () => {
    const available = { id: 1, name: '已有分集', episodeCount: 3 }
    vi.mocked(getAnimationhot).mockResolvedValue({ code: 200, data: [available] })
    vi.mocked(getAnimation).mockResolvedValue({ code: 200, data: [
      available, { id: 2, name: '仅资料', episodeCount: 0 }, { id: 3, name: '旧接口' },
    ] })
    const wrapper = mount(Animation, { global })
    try {
      await flushPromises()
      expect(wrapper.get('.episode-status--carousel').text()).toBe('已收录 3 集')
      expect(wrapper.findAll('.card-container').map(card => card.findAll('.episode-status')[0]?.text()))
        .toEqual(['已收录 3 集', '暂无正片', undefined])
    } finally {
      wrapper.unmount()
    }
  })
})
