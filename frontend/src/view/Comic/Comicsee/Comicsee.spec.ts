import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ComicReader from './index.vue'
import { getcartoonbyid, getcartoonchapter, getcartoonpage } from '@/api/cartoon'

const { push, saveComicProgress, getComicProgress, routeQuery } = vi.hoisted(() => ({
  push: vi.fn(),
  saveComicProgress: vi.fn(),
  getComicProgress: vi.fn(),
  routeQuery: {} as Record<string, string>,
}))

vi.mock('@/utils/request', () => ({ default: { post: vi.fn() } }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: routeQuery }),
  useRouter: () => ({ push }),
}))
vi.mock('@/stores/modules/users', () => ({ default: () => ({}) }))
vi.mock('@/utils/acgnLocal', () => ({ saveComicProgress, getComicProgress, ensureAcgnUserHydrated: vi.fn(), pushHistory: vi.fn() }))
vi.mock('@/api/cartoon', () => ({
  getcartoonbyid: vi.fn(),
  getcartoonchapter: vi.fn(),
  getcartoonpage: vi.fn(),
}))

const pages = [1, 2, 3, 4].map((page) => ({ fileurl: `/page-${page}.jpg` }))

function mountReader() {
  return mount(ComicReader, { global: { stubs: { Headpage: true, Behind: true } } })
}

function stubDialog(dialog: HTMLDialogElement) {
  Object.assign(dialog, {
    showModal() { dialog.open = true },
    close() {
      dialog.open = false
      dialog.dispatchEvent(new Event('close'))
    },
  })
}

beforeEach(() => {
  vi.resetAllMocks()
  delete routeQuery.immersive
  delete routeQuery.pageNumber
  Object.assign(routeQuery, { comicid: '8', chapterid: '0018042', chapterno: '4' })
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  vi.mocked(getcartoonbyid).mockResolvedValue({ code: 200, data: { name: '漫画' } })
  vi.mocked(getcartoonchapter).mockResolvedValue({ code: 200, data: [
    { chapterid: '0018012', chapterno: 2 },
    { chapterid: '0018042', chapterno: 4 },
    { chapterid: '0030871', chapterno: 7 },
  ] })
})

describe('漫画阅读器', () => {
  it('输入第 5 页，加载第二批并保存实际页码', async () => {
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages })
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/page-5.jpg' }] })
    const wrapper = mountReader()
    await flushPromises()
    await wrapper.get('input[aria-label="目标页码"]').setValue('5')
    await wrapper.get('form.page-jump').trigger('submit')
    await flushPromises()
    expect(getcartoonpage).toHaveBeenLastCalledWith('0018042', 2)
    expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 5 })
    wrapper.unmount()
  })

  it('刷新时同一章节用新保存的页码覆盖续读链接里的旧页码', async () => {
    routeQuery.pageNumber = '2'
    const navigation = vi.spyOn(performance, 'getEntriesByType').mockReturnValue([{ type: 'reload' }] as any)
    getComicProgress.mockReturnValue({ chapterId: '0018042', chapterno: 4, pageNumber: 3 })
    vi.mocked(getcartoonpage).mockResolvedValueOnce({ code: 200, data: pages })
    const wrapper = mountReader()
    try {
      await flushPromises()
      expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 3 })
    } finally {
      wrapper.unmount()
      navigation.mockRestore()
    }
  })

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

  it('后续批次失败后保留图片和进度，预载下一批不推进实际页码', async () => {
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
    expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 1 })
    wrapper.unmount()
  })

  it('续读等待目标页成功加载，从第二批的第五张实际图页恢复', async () => {
    routeQuery.pageNumber = '5'
    const scrollIntoView = vi.fn()
    const original = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollIntoView')
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { value: scrollIntoView, configurable: true })
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages })
      .mockRejectedValueOnce(new Error('暂时离线'))
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/page-5.jpg' }] })
    const wrapper = mountReader()
    try {
      await flushPromises()
      expect(saveComicProgress).not.toHaveBeenCalled()
      await wrapper.get('.retry-pages').trigger('click')
      await flushPromises()
      expect(vi.mocked(getcartoonpage).mock.calls).toEqual([['0018042', 1], ['0018042', 2], ['0018042', 2]])
      expect(scrollIntoView.mock.contexts[0]).toBe(wrapper.findAll('.comic-page')[4].element)
      expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 5 })
    } finally {
      wrapper.unmount()
      if (original) Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', original)
      else Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView')
    }
  })

  it('普通滚动记录进入阅读位置的实际图页', async () => {
    vi.mocked(getcartoonpage).mockResolvedValueOnce({ code: 200, data: pages })
    const wrapper = mountReader()
    await flushPromises()
    wrapper.findAll('.comic-page').forEach(({ element }, index) => {
      const top = 1000 * (index - 2)
      vi.spyOn(element, 'getBoundingClientRect').mockReturnValue({ top, bottom: top + 1000 } as DOMRect)
    })
    const container = wrapper.get('.acgn_container').element
    vi.spyOn(container, 'getBoundingClientRect').mockReturnValue({ top: 0, bottom: 700 } as DOMRect)
    document.body.append(wrapper.element)
    container.dispatchEvent(new Event('scroll'))
    await vi.waitFor(() => {
      expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 3 })
    })
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

  it('沉浸模式切换到真实章节，正文可用后从该章第 1 页继续沉浸阅读', async () => {
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages.slice(0, 1) })
      .mockRejectedValueOnce(new Error('新章节暂时离线'))
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/chapter-7-page-1.jpg' }] })

    const reader = mountReader()
    stubDialog(reader.get<HTMLDialogElement>('dialog').element)
    await flushPromises()
    await reader.get('.comic-page').trigger('click')
    const buttons = reader.get('.immersive-chapters').findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    expect(push.mock.calls.map(([target]) => target.query)).toEqual([
      { comicid: 8, chapterid: '0018012', chapterno: 2, immersive: '1' },
      { comicid: 8, chapterid: '0030871', chapterno: 7, immersive: '1' },
    ])
    reader.unmount()

    Object.assign(routeQuery, push.mock.lastCall![0].query)
    const destination = mountReader()
    const dialog = destination.get<HTMLDialogElement>('dialog').element
    stubDialog(dialog)
    await flushPromises()
    expect(dialog.open).toBe(false)
    await destination.get('.retry-pages').trigger('click')
    await flushPromises()
    expect(dialog.open).toBe(true)
    expect(destination.get('.immersive-chapters').text()).toContain('第 7 章')
    expect(destination.get('.immersive-chapters button:last-child').attributes('disabled')).toBeDefined()
    expect(destination.get('.immersive-stage img').attributes('src')).toBe('/chapter-7-page-1.jpg')
    expect(destination.get('.immersive-stage img').attributes('alt')).toBe('漫画第 1 页')
    destination.unmount()
  })

  it('到达当前批次最后一张图前开始加载下一批，重复通知不会重复请求', async () => {
    const observe = vi.fn()
    let intersect: IntersectionObserverCallback
    vi.stubGlobal('IntersectionObserver', class {
      constructor(callback: IntersectionObserverCallback) { intersect = callback }
      observe = observe
      disconnect = vi.fn()
    })
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages })
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/page-5.jpg' }] })

    const wrapper = mountReader()
    try {
      await flushPromises()
      const lastImage = wrapper.findAll('.comic-page')[3].element
      expect(observe).toHaveBeenCalledWith(lastImage)
      expect(wrapper.find('.more-pages').exists()).toBe(false)
      const notify = (isIntersecting: boolean) =>
        intersect([{ target: lastImage, isIntersecting } as IntersectionObserverEntry], {} as IntersectionObserver)
      notify(false)
      expect(getcartoonpage).toHaveBeenCalledTimes(1)
      notify(true)
      notify(true)
      await flushPromises()
      expect(vi.mocked(getcartoonpage).mock.calls).toEqual([['0018042', 1], ['0018042', 2]])
      expect(wrapper.findAll('.comic-page')).toHaveLength(5)
      expect(wrapper.findAll('.comic-page img').map(img => img.attributes('loading')))
        .toEqual(['eager', 'lazy', 'lazy', 'lazy', 'eager'])
      expect(wrapper.text()).toContain('已经读完本章')
    } finally {
      wrapper.unmount()
      vi.unstubAllGlobals()
    }
  })

  it('从点开的图片进入沉浸模式，滑动跨过分页后回到当前阅读位置', async () => {
    vi.mocked(getcartoonpage)
      .mockResolvedValueOnce({ code: 200, data: pages })
      .mockResolvedValueOnce({ code: 200, data: [{ fileurl: '/page-5.jpg' }] })

    const wrapper = mountReader()
    await flushPromises()
    const dialog = wrapper.get<HTMLDialogElement>('dialog').element
    stubDialog(dialog)
    await wrapper.findAll('.comic-page')[0].trigger('click')
    expect(dialog.open).toBe(true)
    expect(wrapper.findAll('dialog img[fetchpriority="low"]').map(img => img.attributes('src')))
      .toEqual(['/page-2.jpg', '/page-3.jpg'])
    expect(getcartoonpage).toHaveBeenCalledTimes(1)

    const stage = wrapper.get('.immersive-stage')
    const swipe = async (from: number, to: number) => {
      const pointer = { isPrimary: true, pointerId: 1, button: 0, clientY: 150 }
      await stage.trigger('pointerdown', { ...pointer, clientX: from })
      await stage.trigger('pointerup', { ...pointer, clientX: to })
      await flushPromises()
    }
    await swipe(280, 80)
    await swipe(280, 80)
    expect(wrapper.get('.immersive-stage img').attributes('src')).toBe('/page-3.jpg')
    expect(getcartoonpage).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('dialog img[fetchpriority="low"]').map(img => img.attributes('src')))
      .toEqual(['/page-4.jpg', '/page-5.jpg'])
    await swipe(280, 80)
    await swipe(80, 280)
    expect(wrapper.get('.immersive-stage img').attributes('src')).toBe('/page-3.jpg')
    await swipe(280, 80)
    await swipe(280, 80)
    expect(wrapper.get('.immersive-stage img').attributes('src')).toBe('/page-5.jpg')
    expect(vi.mocked(getcartoonpage).mock.calls).toEqual([['0018042', 1], ['0018042', 2]])
    expect(wrapper.get('.immersive-page-controls button:last-child').attributes('disabled')).toBeDefined()

    expect(saveComicProgress).toHaveBeenLastCalledWith(8, { chapterId: '0018042', chapterno: 4, pageNumber: 5 })

    const scrollIntoView = vi.fn()
    const lastPage = wrapper.findAll('.comic-page')[4].element
    Object.assign(lastPage, { scrollIntoView })
    await wrapper.get('.immersive-bar button').trigger('click')
    expect(dialog.open).toBe(false)
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'start' })
    wrapper.unmount()
  })
})
