import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError } from 'axios'
vi.mock('@/router', () => ({ default: { currentRoute: { value: { fullPath: '/search?q=test', meta: {} } }, replace: vi.fn() } }))
vi.mock('@/utils/acgnLocal', () => ({ getOrCreateAcgnClientId: () => 'guest-browser' }))
vi.mock('element-plus', () => ({ ElMessage: vi.fn() }))
import request from '../request'
import useUserStore from '@/stores/modules/users'
import router from '@/router'

beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('TOKEN', 'expired-session')
  setActivePinia(createPinia())
  router.currentRoute.value = { fullPath: '/search?q=test', meta: {} } as any
  vi.clearAllMocks()
})

describe('失效凭证与网络故障', () => {
  it.each(['legacy', 'http'])('%s 未登录响应清理凭证，在公开页原地提示登录', async (kind) => {
    request.defaults.adapter = async config => {
      const response = { data: { code: 0, msg: 'NOT_LOGIN' }, status: kind === 'http' ? 401 : 200, statusText: '', headers: {}, config }
      if (kind === 'http') throw new AxiosError('unauthorized', 'ERR_BAD_REQUEST', config, null, response)
      return response
    }
    await expect(request.get('/admin/acl/index/info')).rejects.toThrow('登录状态已失效')
    expect(localStorage.getItem('TOKEN')).toBeNull()
    expect(useUserStore().token).toBe('')
    expect(useUserStore().loginVisible).toBe(true)
    expect(router.replace).not.toHaveBeenCalled()
  })

  it('受保护页面凭证失效后回到首页，保留重新登录的目标', async () => {
    router.currentRoute.value = { fullPath: '/user/center?tab=uploads', meta: { requiresAuth: true } } as any
    request.defaults.adapter = async config => ({ data: { msg: 'NOT_LOGIN' }, status: 200, statusText: '', headers: {}, config })
    await expect(request.get('/admin/acl/index/info')).rejects.toThrow()
    expect(useUserStore().loginRedirect).toBe('/user/center?tab=uploads')
    expect(router.replace).toHaveBeenCalledWith('/')
  })

  it('网络故障不清除登录凭证，也不弹出登录框', async () => {
    request.defaults.adapter = async () => { throw new Error('offline') }
    await expect(request.get('/admin/acl/index/info')).rejects.toThrow('网络连接失败')
    expect(localStorage.getItem('TOKEN')).toBe('expired-session')
    expect(useUserStore().loginVisible).toBe(false)
  })

  it('旧凭证请求延迟返回 401 时，不清除刚登录的新凭证', async () => {
    let rejectOldRequest: (() => void) | undefined
    request.defaults.adapter = config => new Promise((_, reject) => {
      rejectOldRequest = () => reject(new AxiosError('unauthorized', 'ERR_BAD_REQUEST', config, null, {
        data: { msg: 'NOT_LOGIN' }, status: 401, statusText: '', headers: {}, config,
      }))
    })
    const pending = request.get('/admin/acl/index/info')
    await vi.waitFor(() => expect(rejectOldRequest).toBeDefined())
    useUserStore().token = 'fresh-session'
    localStorage.setItem('TOKEN', 'fresh-session')
    rejectOldRequest!()
    await expect(pending).rejects.toThrow()
    expect(useUserStore().token).toBe('fresh-session')
    expect(localStorage.getItem('TOKEN')).toBe('fresh-session')
    expect(useUserStore().loginVisible).toBe(false)
  })
})
