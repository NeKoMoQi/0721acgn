import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
vi.mock('@/api/user', () => ({ reqLogin: vi.fn(), reqUserInfo: vi.fn(), reqLogout: vi.fn() }))
import { constantRoute } from '../routes'
import { requireAccount } from '../index'
import useUserStore from '@/stores/modules/users'

function makeRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: constantRoute.map(route => ({ ...route, component: { template: '<div />' } })),
  })
  router.beforeEach(requireAccount)
  return router
}

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

describe('游客路由与登录返回', () => {
  it.each(['/search?q=test', '/animation', '/comic', '/novel', '/Animationintroduce?animationid=3', '/Comicsee?comicid=4', '/Novelsee?novelid=5', '/AnimationWatch?animationid=3', '/bookshelf', '/shengdi'])('游客可直接打开 %s', async (path) => {
    const router = makeRouter()
    await router.push(path)
    expect(router.currentRoute.value.fullPath).toBe(path)
    expect(useUserStore().loginVisible).toBe(false)
  })

  it.each(['/messages', '/user/center', '/user/upload', '/user/activity', '/shengdi/addnew', '/warflag', '/warflag/battle?side=german'])('受保护页面 %s 保留来源、记住目标，取消后仍在来源页', async (path) => {
    const router = makeRouter()
    await router.push('/search?q=test')
    await router.push(path)
    const store = useUserStore()
    expect(router.currentRoute.value.fullPath).toBe('/search?q=test')
    expect(store.loginVisible).toBe(true)
    expect(store.loginRedirect).toBe(path)
    store.closeLogin()
    expect(store.loginRedirect).toBe('')
    expect(router.currentRoute.value.fullPath).toBe('/search?q=test')
  })

  it('直接打开受保护链接时落到首页，登录后可恢复完整目标地址', async () => {
    const router = makeRouter()
    const target = '/user/center?tab=uploads'
    await router.push(target)
    const store = useUserStore()
    expect(router.currentRoute.value.path).toBe('/')
    expect(store.loginRedirect).toBe(target)
    localStorage.setItem('TOKEN', 'signed-in')
    await router.push(store.loginRedirect)
    expect(router.currentRoute.value.fullPath).toBe(target)
  })
})
