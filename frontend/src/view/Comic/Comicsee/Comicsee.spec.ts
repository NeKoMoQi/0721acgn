import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ComicReader from './index.vue'
import { getcartoonbyid, getcartoonchapter, getcartoonpage } from '@/api/cartoon'

const { push, saveComicProgress } = vi.hoisted(() => ({
  push: vi.fn(),
  saveComicProgress: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { comicid: '8', chapterid: '0018042', chapterno: '4' } }),
  useRouter: () => ({ push }),
}))
vi.mock('@/stores/modules/users', () => ({ default: () => ({}) }))
vi.mock('@/utils/acgnLocal', () => ({ saveComicProgress, pushHistory: vi.fn() }))
vi.mock('@/api/cartoon', () => ({
  getcartoonbyid: vi.fn(),
  getcartoonchapter: vi.fn(),
  getcartoonpage: vi.fn(),
}))

const pages = [1, 2, 3, 4].map((page) => ({ fileurl: `/page-${page}.jpg` }))

function mountReader() {
  return mount(ComicReader, { global: { stubs: { Headpage: true, Behind: true } } })
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  vi.mocked(getcartoonbyid).mockResolvedValue({ code: 200, data: { name: '漫画' } })
  vi.mocked(getcartoonchapter).mockResolvedValue({ code: 200, data: [
    { chapterid: '0018012', chapterno: 2 },
    { chapterid: '0018042', chapterno: 4 },
    { chapterid: '0030871', chapterno: 7 },
  ] })
})

describe('漫画阅读器', () => {
  it('正文独立于目录与作品资料，首批失败后可重试', async () => {
    vi.mocked(getcartoonbyid).mockRejectedValueOnce(new Error('作品资料离线'))
    vi.mocked(getcartoonchapter).mockRejectedValueOnce(new Error('目录离线'))
    vi.mocked(getcartoonpage)
      .mockRejectedValueOnce(new Error('首批失败'))
      .mockResolvedValueOnce({ code: 200, data: pages })

    const wrapper = mountReader()
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('正文加载失败')
    await wrapper.get('.retry-pages').trigger('click')
    await flushPromises()
    expect(vi.mocked(getcartoonpage).mock.calls).toEqual([['0018042', 1], ['0018042', 1]])
    expect(wrapper.findAll('.acgn_container img')).toHaveLength(4)
    wrapper.unmount()
  })

  it('后续批次失败后保留图片和进度，重试相同页码', async () => {
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages })
      .mockResolvedValueOnce({ code: 500, data: [] })
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/page-5.jpg' }] })

    const wrapper = mountReader()
    await flushPromises()
    await wrapper.get('.more-pages').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.acgn_container img')).toHaveLength(4)
    expect(saveComicProgress).toHaveBeenCalledTimes(1)
    await wrapper.get('.retry-pages').trigger('click')
    await flushPromises()
    expect(vi.mocked(getcartoonpage).mock.calls).toEqual([['0018042', 1], ['0018042', 2], ['0018042', 2]])
    expect(wrapper.findAll('.acgn_container img')).toHaveLength(5)
    expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, imagePage: 2 })
    wrapper.unmount()
  })

  it('从目录选择真实下一章 ID 和章号', async () => {
    vi.mocked(getcartoonpage).mockResolvedValue({ code: 200, data: pages })
    const wrapper = mountReader()
    await flushPromises()
    await wrapper.get('.next').trigger('click')
    expect(push).toHaveBeenCalledWith({
      name: 'Comicsee',
      query: { comicid: 8, chapterid: '0030871', chapterno: 7 },
    })
    wrapper.unmount()
  })
})
