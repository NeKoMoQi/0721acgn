import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus, { ElMessage, ElNotification } from 'element-plus'
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElMessage: { warning: vi.fn(), info: vi.fn() }, ElNotification: vi.fn() }
})
vi.mock('@/api/user', () => ({ reqLogin: vi.fn(), reqUserInfo: vi.fn(), reqLogout: vi.fn() }))
import { reqLogin, reqUserInfo } from '@/api/user'
import LoginDialog from '../LoginDialog.vue'
import useUserStore from '@/stores/modules/users'

let wrapper: VueWrapper | undefined
beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
})
afterEach(() => { wrapper?.unmount(); wrapper = undefined })

async function openLogin() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes: ['/', '/search', '/user/center'].map(path => ({ path, component: { template: '<div />' } })) })
  await router.push('/search?q=test')
  const store = useUserStore()
  store.triggerLoginWall('/user/center?tab=uploads', '登录后即可访问个人中心')
  wrapper = mount(LoginDialog, { global: { plugins: [pinia, router, ElementPlus] } })
  await flushPromises()
  return { store, router, view: wrapper }
}

describe('保留原有登录界面的交互', () => {
  it('保留原有二维码、人物图片、手机登录和注册入口，空密码不发请求', async () => {
    const { view } = await openLogin()
    expect(view.find('.loading_page').exists()).toBe(true)
    expect(view.find('img[src="/qer.jpg"]').exists()).toBe(true)
    expect(view.find('img[src="/tantou.png"]').exists()).toBe(true)
    expect(view.get('.zhuce').text()).toBe('注册')
    await view.get('.denglu').trigger('click')
    expect(ElMessage.warning).toHaveBeenCalledWith('请填写账号和密码')
    expect(reqLogin).not.toHaveBeenCalled()
    await view.findAll('div').find(item => item.text() === '手机登录')!.trigger('click')
    expect(view.find('input[placeholder="请输入手机号"]').exists()).toBe(true)
    await view.get('.denglu').trigger('click')
    expect(ElMessage.info).toHaveBeenCalledWith('手机登录暂未开放，请先使用账号密码登录')
    expect(reqLogin).not.toHaveBeenCalled()
  })

  it('使用原有关闭按钮取消登录，保留搜索页面并清空待跳转地址', async () => {
    const { view, store, router } = await openLogin()
    await view.get('.load > div:first-child').trigger('click')
    expect(store.loginVisible).toBe(false)
    expect(store.loginRedirect).toBe('')
    expect(router.currentRoute.value.fullPath).toBe('/search?q=test')
  })

  it('密码错误使用原有通知提示，不关闭弹窗或跳转', async () => {
    const { view, store, router } = await openLogin()
    vi.mocked(reqLogin).mockResolvedValue({ code: 0, msg: '用户名或密码错误' })
    await view.get('input[autocomplete="username"]').setValue('reader')
    await view.get('input[autocomplete="current-password"]').setValue('wrong')
    await view.get('.denglu').trigger('click')
    await flushPromises()
    expect(ElNotification).toHaveBeenCalledWith(expect.objectContaining({ type: 'error', message: '用户名或密码错误' }))
    expect(store.loginVisible).toBe(true)
    expect(router.currentRoute.value.path).toBe('/search')
  })

  it('原有登录按钮成功后恢复完整目标地址，关闭后清空密码', async () => {
    const { view, store, router } = await openLogin()
    vi.mocked(reqLogin).mockResolvedValue({ code: 200, data: 'valid-session' })
    vi.mocked(reqUserInfo).mockResolvedValue({ code: 200, data: { username: 'reader', avatar: '', level: '1' } })
    await view.get('input[autocomplete="username"]').setValue('reader')
    await view.get('input[autocomplete="current-password"]').setValue('correct')
    await view.get('.denglu').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/user/center?tab=uploads')
    expect(store.loginVisible).toBe(false)
    expect(localStorage.getItem('TOKEN')).toBe('valid-session')
    store.triggerLoginWall()
    await flushPromises()
    expect((view.get('input[autocomplete="current-password"]').element as HTMLInputElement).value).toBe('')
  })
})
