import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { expect, it, vi } from 'vitest'
import Character from './index.vue'
import { constantRoute } from '@/router/routes'
import { castForWork, matchingCast } from '@/data/characterAssociations'
import { getCharacterProfile } from '@/data/characterProfiles'
import catalogProfiles from '@/data/catalogCharacterProfiles.json'
import { mediaFor } from '@/data/galgameMedia'
import request from '@/utils/request'

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
    const links = view.findAll('.character-works a').map(link => link.attributes('href'))
    expect(links).toEqual(['/Animationintroduce?animationid=11791', '/GalgameIntroduce?slug=atri'])
  } finally {
    view.unmount()
  }
})

it('新增角色可通过别名搜索，且对应作品的每个角色都有资料来源', async () => {
  expect(matchingCast('廣井きくり')).toEqual([expect.objectContaining({ id: 'kikuri', title: '孤独摇滚！' })])
  for (const comicId of [24, 31]) {
    const cast = castForWork('comic', comicId)
    expect(cast).toHaveLength(8)
    for (const person of cast) {
      const profile = getCharacterProfile(person.name)
      expect(profile?.paragraphs.length).toBeGreaterThanOrEqual(2)
      expect(profile?.sources.some(source => source.url.startsWith('https://moegirl.uk/'))).toBe(true)
    }
  }
  const view = await open('/character/kikuri')
  try {
    expect(view.get('h1').text()).toBe('广井菊里')
    expect(view.get('.intro').text()).toContain('SICK HACK')
    expect(view.get('.profile-sources a').attributes('href')).toBe('https://moegirl.uk/广井菊里')
  } finally {
    view.unmount()
  }
})

it('数字人物仍使用原有地址，修复旧名字并显示来源与作品', async () => {
  vi.mocked(request.get).mockResolvedValueOnce({
    code: 200,
    data: { id: '1', name: '11月9日', intro: '' },
  })
  const view = await open('/character/1')
  try {
    expect(view.get('h1').text()).toBe('木下秀吉')
    expect(view.get('.intro').text()).toContain('演剧部')
    expect(view.get('.character-works a').attributes('href')).toBe('/Animationintroduce?animationid=11231')
  } finally {
    view.unmount()
  }
})

it('补录数据保留人物身份与可回查的来源，且不同频道的新增阵容可查', () => {
  expect(Object.keys(catalogProfiles)).toHaveLength(77)
  for (const [id, item] of Object.entries(catalogProfiles)) {
    const profile = getCharacterProfile(item.catalogName, id)
    expect(profile?.paragraphs.join('').length).toBeGreaterThanOrEqual(90)
    expect(profile?.sources[0].url).toMatch(/^https:\/\//)
  }
  expect(castForWork('comic', 6)).toHaveLength(6)
  expect(castForWork('animation', 10933)).toEqual(castForWork('comic', 6))
  expect(castForWork('novel', 18)).toHaveLength(4)
  expect(mediaFor('clannad')?.characters).toHaveLength(10)
})
