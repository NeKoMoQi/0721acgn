import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { expect, it, vi } from 'vitest'
import Header from '../Headpage/index.vue'
import { getAllSearch } from '@/api/AllSearch'

vi.mock('@/api/AllSearch', () => ({ getAllSearch: vi.fn() }))
vi.mock('@/stores/modules/users', () => ({ default: () => ({ token: '', triggerLoginWall: vi.fn() }) }))
vi.mock('@/utils/acgnNotifications', () => ({ unreadNotificationCount: () => 0 }))

it('快速搜索不会显示过期结果，并明确显示当前请求失败', async () => {
  let finishOld: (value: unknown) => void = () => {}
  vi.mocked(getAllSearch)
    .mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve }))
    .mockResolvedValueOnce({ code: 200, data: [{ id: 2, name: '新版结果', sourcetype: '漫画' }] })
    .mockRejectedValueOnce(new Error('请求失败'))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:rest(.*)*', component: { template: '<div />' } }],
  })
  await router.push('/')
  const view = mount(Header, { global: { plugins: [router], stubs: {
    ElInput: {
      props: ['modelValue'],
      emits: ['update:modelValue', 'input'],
      template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value); $emit(\'input\', $event.target.value)">',
    },
    ElButton: true,
    ElDialog: true,
    ElIcon: true,
    ElTooltip: true,
  } } })
  try {
    await view.get('input').setValue('旧词')
    await view.get('input').setValue('新词')
    await flushPromises()
    finishOld({ code: 200, data: [{ id: 1, name: '过期结果', sourcetype: '漫画' }] })
    await flushPromises()
    expect(view.get('.result-item').text()).toContain('新版结果')
    expect(view.text()).not.toContain('过期结果')
    await view.get('input').setValue('出错的查询')
    await flushPromises()
    expect(view.get('[role="status"]').text()).toContain('快速搜索失败')
    expect(view.find('.result-item').exists()).toBe(false)
  } finally {
    view.unmount()
  }
})
