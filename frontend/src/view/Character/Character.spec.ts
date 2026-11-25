import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { expect, it, vi } from 'vitest'
import Character from './index.vue'
import { constantRoute } from '@/router/routes'

vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<header />' } }))
vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))

async function open(url: string) {
  const router = createRouter({ history: createMemoryHistory(), routes: constantRoute })
  await router.push(url)
  const view = mount(Character, { global: { plugins: [router] } })
  await flushPromises()
  return view
}

it('直接访问漫画与动漫共用的人物介绍，可返回两个版本', async () => {
  const view = await open('/character/frieren')
  try {
    expect(view.get('h1').text()).toBe('芙莉莲')
    const links = view.findAll('.character-works a').map(link => link.attributes('href'))
    expect(links).toEqual(['/ComicIntroduce?comicid=24', '/Animationintroduce?animationid=10941'])
  } finally {
    view.unmount()
  }
})

it('直接访问 Galgame 角色地址，可以看到原有资料并返回作品', async () => {
  const view = await open('/character/galgame/atri/' + encodeURIComponent('亚托莉'))
  try {
    expect(view.get('h1').text()).toBe('亚托莉')
    expect(view.get('.intro').text()).toContain('机器人少女')
    expect(view.get('.character-works a').attributes('href')).toBe('/GalgameIntroduce?slug=atri')
  } finally {
    view.unmount()
  }
})
