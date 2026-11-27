import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { afterAll, beforeAll, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { archiveCanonicalProfile, archiveCast, archiveCharacterLocation, archiveProfile, archiveSupplement, archiveWorkLocation, searchArchiveCharacters } from '@/data/characterArchive'
import Character from '@/view/Character/index.vue'
import WorkCharacters from '@/components/WorkCharacters.vue'
import { constantRoute } from '@/router/routes'
import { getAllSearch } from '@/api/AllSearch'
import request from '@/utils/request'

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))
vi.mock('@/components/Headpage/index.vue', () => ({ default: { template: '<header />' } }))

beforeAll(() => {
  vi.stubGlobal('fetch', async (url: string) => {
    const name = url.split('/').pop()!
    const bytes = await readFile(resolve('public/data/characters', name), 'utf-8')
    return { ok: true, json: async () => JSON.parse(bytes) }
  })
})
afterAll(() => vi.unstubAllGlobals())

it('opens an illustrated archive profile with inline prose and sources after the work links', async () => {
  await archiveProfile(89181)
  const router = createRouter({ history: createMemoryHistory(), routes: constantRoute })
  await router.push('/character/bgm-89181')
  const view = mount(Character, { global: { plugins: [router] } })
  try {
    await flushPromises()
    expect(view.get('h1').text()).toBe('阿乌拉')
    expect(view.get('.portrait img').attributes('src')).toContain('frieren-anime.jp')
    expect(view.get('.intro').text()).toContain('七崩贤')
    expect(view.get('.character-works a').attributes('href')).toMatch(/^\/(ComicIntroduce|Animationintroduce)\?/)
    expect(view.get('.sources a').attributes('href')).toBe('https://frieren-anime.jp/news/646/')
    expect(view.find('.intro a').exists()).toBe(false)
  } finally {
    view.unmount()
  }
})

it('shows the archive introduction on the site with contributor and license attribution', async () => {
  await archiveProfile(99541)
  const paragraphs = (await archiveSupplement(99541))?.paragraphs
  const router = createRouter({ history: createMemoryHistory(), routes: constantRoute })
  await router.push('/character/bgm-99541')
  const view = mount(Character, { global: { plugins: [router] } })
  try {
    await flushPromises()
    expect(paragraphs?.length).toBeGreaterThan(0)
    expect(view.get('.intro p').text()).toBe(paragraphs![0])
    expect(view.get('.synopsis-credit a').attributes('href')).toBe('https://creativecommons.org/licenses/by-sa/3.0/')
    expect(view.findAll('.profile-sources a').map(link => link.attributes('href'))).toContain('https://bgm.tv/character/99541')
    expect((await archiveSupplement(86246))?.paragraphs).toBeUndefined()
  } finally {
    view.unmount()
  }
})

it('links non-spoiler relationships to the canonical local character pages', async () => {
  await archiveCanonicalProfile('clannad:古河渚')
  const details = await archiveSupplement(4)
  const router = createRouter({ history: createMemoryHistory(), routes: constantRoute })
  await router.push('/character/galgame/clannad/' + encodeURIComponent('古河渚'))
  const view = mount(Character, { global: { plugins: [router] } })
  try {
    await flushPromises()
    expect(details?.relations?.some(([label, person]) => label === '亲属' && person.id === 1019)).toBe(true)
    const link = view.findAll('.character-relations a').find(row => row.text() === '古河秋生')
    expect(link?.attributes('href')).toContain('/character/galgame/clannad/')
  } finally {
    view.unmount()
  }
})

it('shows a single Galgame roster with portraits and keeps the original character routes', async () => {
  await archiveCast('galgame', 'clannad')
  const router = createRouter({ history: createMemoryHistory(), routes: constantRoute })
  await router.push('/')
  const view = mount(WorkCharacters, { props: { type: 'galgame', id: 'clannad' }, global: { plugins: [router] } })
  try {
    await flushPromises()
    const cards = view.findAll('.cast-card')
    expect(cards).toHaveLength(12)
    const nagisa = cards.filter(card => card.text() === '古河渚')
    expect(nagisa).toHaveLength(1)
    expect(nagisa[0].get('img').attributes('src')).toContain('key.visualarts.gr.jp')
    expect(nagisa[0].attributes('href')).toContain('/character/galgame/clannad/')
    await view.get('button').trigger('click')
    expect(view.findAll('.cast-card').length).toBeGreaterThan(12)
  } finally {
    view.unmount()
  }
})

it('keeps the apothecary and the similarly named cat on separate character routes', async () => {
  expect((await archiveCanonicalProfile('maomao'))?.person.id).toBe(61330)
  expect(archiveCharacterLocation((await archiveProfile(143350))!.person)).toEqual({
    name: 'Character', params: { id: 'bgm-143350' },
  })
})

it('opens new comic characters, keeps curated links and returns to the correct 0721 work', async () => {
  const cast = await archiveCast('comic', 24)
  expect(cast.length).toBeGreaterThan(20)
  const frieren = cast.find(({ person }) => person.siteId === 'frieren')!
  expect(archiveCharacterLocation(frieren.person)).toEqual({ name: 'Character', params: { id: 'frieren' } })
  const aura = cast.find(({ person }) => person.name === '阿乌拉')!
  const profile = await archiveProfile(aura.person.id)
  expect(profile?.person.name).toBe('阿乌拉')
  const book = profile?.works.find(work => work.type === 'comic' && work.id === 24)!
  expect(book.sources.length).toBeGreaterThan(0)
  expect(archiveWorkLocation(book)).toEqual({ name: 'ComicIntroduce', query: { comicid: 24 } })
  expect(await searchArchiveCharacters('阿乌拉', 6)).toContainEqual(expect.objectContaining({ id: `bgm-${aura.person.id}` }))
})

it('paginates local cast matches after database results without skipping a partial page', async () => {
  const known = { id: 91, name: '已收录人物', sourcetype: '角色' }
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: { items: [known], total: 1, page: 1, pageSize: 4 } })
  const first = await getAllSearch({ mediaType: 'character', page: 1, limit: 4 })
  const second = await getAllSearch({ mediaType: 'character', page: 2, limit: 4 })
  expect(first.data?.[0]).toEqual(known)
  expect(first.data).toHaveLength(4)
  expect(second.data).toHaveLength(4)
  expect(second.page).toBe(2)
  expect(second.total).toBeGreaterThan(20000)
  expect(second.data?.some(row => first.data?.some(other => row.id === other.id))).toBe(false)
})
