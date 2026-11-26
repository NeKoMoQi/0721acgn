import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/stores/modules/users', () => ({ default: () => ({ username: 'reader', avatar: '', level: '1' }) }))
vi.mock('@/utils/acgnLocal', () => ({ ensureAcgnUserHydrated: vi.fn(), listHistory: () => [], listSubscriptions: () => [], listContinueItems: vi.fn() }))
vi.mock('@/utils/acgnPersonalFeed', () => ({ listMyPublishedComments: () => [], listUserUploadRecords: () => [] }))
import { listContinueItems, type ContinueItem } from '@/utils/acgnLocal'
import Center from '../Center.vue'

const items: ContinueItem[] = [
  { type: 'animation', id: 1, name: '番剧', at: 30, position: 5 },
  { type: 'comic', id: 2, name: '漫画', at: 20, position: 3, chapterId: '0000203', pageNumber: 9 },
  { type: 'novel', id: 3, name: '小说', at: 10, position: 2 },
]

beforeEach(() => { vi.mocked(listContinueItems).mockReturnValue(items) })

async function setup() {
  const names = ['UserCenter', 'AnimationWatch', 'Comicsee', 'Novelsee', 'Bookshelf', 'UserUpload']
  const router = createRouter({ history: createMemoryHistory(), routes: names.map(name => ({ path: '/' + name, name, component: { template: '<div />' } })) })
  await router.push({ name: 'UserCenter' })
  const view = mount(Center, { global: { plugins: [router], stubs: {
    ElTabs: { template: '<div><slot /></div>' }, ElTabPane: { template: '<div><slot /></div>' },
    ElButton: { template: '<button><slot /></button>' }, Behind: true,
  } } })
  await flushPromises()
  return { view, router }
}

describe('个人中心续看入口', () => {
  it.each([
    [0, 'AnimationWatch', { animationid: '1', episode: '5' }],
    [1, 'Comicsee', { comicid: '2', chapterid: '0000203', chapterno: '3', pageNumber: '9' }],
    [2, 'Novelsee', { novelid: '3', chapterno: '2' }],
  ] as const)('第 %s 个入口直接打开对应观看 / 阅读页', async (index, name, query) => {
    const { view, router } = await setup()
    expect(view.text()).toContain('上次观看至第 5 集')
    expect(view.text()).toContain('上次阅读至第 3 章 · 第 9 页')
    expect(view.text()).toContain('上次阅读至第 2 卷')
    await view.findAll('.resume-row button')[index].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe(name)
    expect(router.currentRoute.value.query).toEqual(query)
    view.unmount()
  })

  it('新用户显示空状态，不出现虚构的续看作品', async () => {
    vi.mocked(listContinueItems).mockReturnValue([])
    const { view } = await setup()
    expect(view.text()).toContain('还没有观看或阅读进度')
    expect(view.findAll('.resume-row')).toHaveLength(0)
    view.unmount()
  })
})
