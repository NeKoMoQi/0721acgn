import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { afterAll, beforeAll, expect, it, vi } from 'vitest'
import { archiveCast, archiveCharacterLocation, archiveProfile, archiveWorkLocation, searchArchiveCharacters } from '@/data/characterArchive'
import { getAllSearch } from '@/api/AllSearch'
import request from '@/utils/request'

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))

beforeAll(() => {
  vi.stubGlobal('fetch', async (url: string) => {
    const name = url.split('/').pop()!
    const bytes = await readFile(resolve('public/data/characters', name), 'utf-8')
    return { ok: true, json: async () => JSON.parse(bytes) }
  })
})
afterAll(() => vi.unstubAllGlobals())

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
