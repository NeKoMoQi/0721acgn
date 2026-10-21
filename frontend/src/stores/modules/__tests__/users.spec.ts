import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/user', () => ({ reqLogin: vi.fn(), reqUserInfo: vi.fn(), reqLogout: vi.fn() }))
import { reqLogin, reqLogout } from '@/api/user'

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

describe('登录凭证生命周期', () => {
  it('重新加载用户模块时不删除已有 token，并恢复到新仓库', async () => {
    localStorage.setItem('TOKEN', 'persisted-session')
    vi.resetModules()
    const { default: useUserStore } = await import('../users')
    expect(localStorage.getItem('TOKEN')).toBe('persisted-session')
    expect(useUserStore().token).toBe('persisted-session')
  })

  it('登录失败保留空凭证并显示后端错误信息', async () => {
    const { default: useUserStore } = await import('../users')
    vi.mocked(reqLogin).mockResolvedValue({ code: 0, msg: '用户名或密码错误' })
    await expect(useUserStore().userLogin({ username: 'bad', password: 'bad' })).rejects.toThrow('用户名或密码错误')
    expect(localStorage.getItem('TOKEN')).toBeNull()
  })

  it('登录成功后刷新仓库仍可恢复凭证', async () => {
    const { default: useUserStore } = await import('../users')
    vi.mocked(reqLogin).mockResolvedValue({ code: 200, data: 'new-session' })
    await useUserStore().userLogin({ username: 'reader', password: 'test-password' })
    setActivePinia(createPinia())
    expect(useUserStore().token).toBe('new-session')
  })

  it('退出接口断网时也清理本机登录态和待跳转地址', async () => {
    const { default: useUserStore } = await import('../users')
    localStorage.setItem('TOKEN', 'old-session')
    const store = useUserStore()
    store.triggerLoginWall('/user/center')
    vi.mocked(reqLogout).mockRejectedValue(new Error('offline'))
    await expect(store.userLogout()).rejects.toThrow('offline')
    expect(store.token).toBe('')
    expect(localStorage.getItem('TOKEN')).toBeNull()
    expect(store.loginVisible).toBe(false)
    expect(store.loginRedirect).toBe('')
  })
})
