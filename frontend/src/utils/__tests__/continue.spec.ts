import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AcgnUserStateDto } from '@/api/acgnUser'
vi.mock('@/api/acgnUser', () => ({
  fetchAcgnUserState: vi.fn(), postAcgnLibraryVisit: vi.fn(), postAcgnFavoriteToggle: vi.fn(),
  postAcgnSubscribeToggle: vi.fn(), postAcgnProgressSave: vi.fn(), postAcgnImportLegacyRows: vi.fn(),
}))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn() } }))
import { fetchAcgnUserState, postAcgnProgressSave, postAcgnLibraryVisit } from '@/api/acgnUser'

const fixture: AcgnUserStateDto = {
  library: [
    { type: 'animation', id: 1, name: '番剧', at: 10 },
    { type: 'comic', id: 2, name: '漫画', at: 30 },
    { type: 'novel', id: 3, name: '小说', at: 20 },
    { type: 'animation', id: 4, name: '只看过简介', at: 99 },
  ],
  subscriptions: [],
  progress: { animation: { 1: 5 }, comic: { 2: { chapterId: '0000203', chapterno: 3 } }, novel: { 3: { chapterno: 2 } } },
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  vi.resetModules()
  vi.clearAllMocks()
  vi.mocked(fetchAcgnUserState).mockResolvedValue({ code: 200, data: structuredClone(fixture) })
  vi.mocked(postAcgnProgressSave).mockResolvedValue({ code: 200 })
  vi.mocked(postAcgnLibraryVisit).mockResolvedValue({ code: 200 })
})

describe('继续观看 / 阅读数据', () => {
  it('恢复三类服务端进度，按最近时间排列并排除只有浏览足迹的作品', async () => {
    const data = await import('../acgnLocal')
    await data.ensureAcgnUserHydrated()
    expect(data.listContinueItems().map(item => [item.type, item.id, item.position, item.chapterId])).toEqual([
      ['comic', 2, 3, '0000203'], ['novel', 3, 2, undefined], ['animation', 1, 5, undefined],
    ])
    expect(data.listHistory()).toHaveLength(4)
  })

  it('没有进度或进度损坏时不生成假的续看入口', async () => {
    vi.mocked(fetchAcgnUserState).mockResolvedValue({ code: 200, data: {
      ...fixture, progress: { animation: { 1: -1 }, comic: { 2: { chapterId: '', chapterno: 1 } }, novel: { 3: { chapterno: 0 } } },
    } })
    const data = await import('../acgnLocal')
    await data.ensureAcgnUserHydrated()
    expect(data.listContinueItems()).toEqual([])
  })

  it('刷新后优先续读最新的确切页码，且不把旧版接口批次当页码', async () => {
    const data = await import('../acgnLocal')
    await data.ensureAcgnUserHydrated()
    expect(data.listContinueItems()[0].pageNumber).toBeUndefined()
    data.saveComicProgress(2, { chapterId: '0000204', chapterno: 4, pageNumber: 13 })
    vi.resetModules()
    const reloaded = await import('../acgnLocal')
    await reloaded.ensureAcgnUserHydrated()
    expect(reloaded.listContinueItems()[0]).toMatchObject({ chapterId: '0000204', position: 4, pageNumber: 13 })
  })

  it.each(['animation', 'comic', 'novel'] as const)('保存 %s 进度后，该作品成为最近观看 / 阅读', async (type) => {
    const data = await import('../acgnLocal')
    await data.ensureAcgnUserHydrated()
    const id = type === 'animation' ? 1 : type === 'comic' ? 2 : 3
    if (type === 'animation') data.saveAnimationEpisode(id, 6)
    else if (type === 'comic') data.saveComicProgress(id, { chapterId: '0000204', chapterno: 4 })
    else data.saveNovelProgress(id, 3)
    await vi.waitFor(() => expect(postAcgnLibraryVisit).toHaveBeenCalled())
    expect(data.listContinueItems()[0].id).toBe(id)
  })
})


it('routes precise bookmarks to the saved page and labels old chapter-only records explicitly', async () => {
  const { resumeLocation, resumeLabel } = await import('../continueReading')
  const bookmark = { id: 25, name: '漫画', at: 10, type: 'comic' as const, position: 7, chapterId: '917' }
  expect(resumeLabel(bookmark)).toBe('第 7 章 · 页码未记录')
  expect(resumeLabel({ ...bookmark, pageNumber: 13 })).toBe('第 7 章 · 第 13 页')
  expect(resumeLocation({ ...bookmark, pageNumber: 13 }).query).toMatchObject({ chapterno: 7, pageNumber: 13 })
})
