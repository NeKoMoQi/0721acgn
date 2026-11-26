import { flushPromises, mount } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import Compare from './duizhao.vue'
import { getXunliPointById } from '@/api/xunli'

const query = vi.hoisted(() => ({ spot: '123', work: undefined as string | undefined }))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query }), useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}))
vi.mock('@/stores/modules/users', () => ({
  default: () => ({ xunlilist: { id: 123, gaoqingpic: '/scene.jpg' } }),
}))
vi.mock('@/api/xunli', () => ({ getXunliPointById: vi.fn() }))
vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<div />' } }))

it('对比不可用时只在本机预览，图片失败也不会生成比分或历史', async () => {
  const fetch = vi.spyOn(window, 'fetch')
  const view = mount(Compare, { global: { stubs: { ElIcon: true } } })
  try {
    expect(view.text()).toContain('自动比对服务暂不可用')
    expect(view.text()).not.toMatch(/巡礼榜单|最近对比|收藏结果|可分享巡礼备注/)
    const input = view.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['image data'], 'photo.png', { type: 'image/png' })],
    })
    await input.trigger('change')
    await vi.waitFor(() => expect(view.get('img[alt="本机照片"]').attributes('src')).toMatch(/^data:image\/png/))
    expect(fetch).not.toHaveBeenCalled()
    await view.get('img[alt="本机照片"]').trigger('error')
    expect(view.get('[role="alert"]').text()).toContain('照片无法预览')
    expect(view.find('.score-box, .history-item').exists()).toBe(false)
  } finally {
    view.unmount()
    fetch.mockRestore()
  }
})

it('宇治专题并排显示动画截图与默认实景参考，且保留原来源', async () => {
  query.spot = '9690'
  query.work = '424'
  vi.mocked(getXunliPointById).mockResolvedValue({ code: 200, data: [
    { id: 9690, xunliname: '大吉山展望台', gaoqingpic: '/eupho-8.jpg' },
  ] })
  const view = mount(Compare, { global: { stubs: { ElIcon: true } } })
  try {
    await flushPromises()
    expect(view.get('img[alt="动画截图"]').attributes('src')).toBe('/eupho-8.jpg')
    expect(view.get('img[alt="大吉山展望台的实景参考照片"]').attributes('src')).toBe('/xunli/uji/daikichiyama.webp')
    expect(view.get('.photo-credit a').attributes('href')).toContain('commons.wikimedia.org/wiki/File:')
    expect(view.text()).toContain('第一季第 8 集')
  } finally {
    query.spot = '123'
    query.work = undefined
    view.unmount()
  }
})
