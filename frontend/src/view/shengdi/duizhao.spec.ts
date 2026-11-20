import { mount } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import Compare from './duizhao.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { spot: '123' } }), useRouter: () => ({ push: vi.fn() }),
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
    await vi.waitFor(() => expect(view.get('img[alt="现实照片"]').attributes('src')).toMatch(/^data:image\/png/))
    expect(fetch).not.toHaveBeenCalled()
    await view.get('img[alt="现实照片"]').trigger('error')
    expect(view.get('[role="alert"]').text()).toContain('照片无法预览')
    expect(view.find('.score-box, .history-item').exists()).toBe(false)
  } finally {
    view.unmount()
    fetch.mockRestore()
  }
})
