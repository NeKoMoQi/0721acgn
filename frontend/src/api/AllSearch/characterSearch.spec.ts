import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAllSearch } from './index'
import request from '@/utils/request'
import { searchResultLocation } from '@/utils/searchResultLocation'
import { castForWork, worksForCharacter, workLocation } from '@/data/characterAssociations'

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(request.get).mockResolvedValue({ code: 200, data: [] })
})

describe('角色搜索与作品关联', () => {
  it('保留数据库人物并按 ID 合并已有的登场作品，不生成第二条人物记录', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ code: 200, data: [{ id: 76, name: '轻井泽惠', sourcetype: '角色' }] })
    const res = await getAllSearch({ search: '轻井泽惠', mediaType: 'character' })
    expect(request.get).toHaveBeenCalledWith('/searchresult?search=%E8%BD%BB%E4%BA%95%E6%B3%BD%E6%83%A0&mediaType=character')
    expect(res.data).toHaveLength(1)
    expect(res.data[0].workTitle).toBe('欢迎来到实力至上主义的教室')
    expect(searchResultLocation(res.data[0])).toEqual({ name: 'Character', params: { id: 76 } })
  })

  it('从顶栏关键字命中 Galgame 人物，并用作品 slug 打开独立角色页', async () => {
    const res = await getAllSearch('亚托莉')
    expect(res.data[0]).toMatchObject({ name: '亚托莉', sourcetype: '角色', galgameSlug: 'atri' })
    expect(searchResultLocation(res.data[0])).toEqual({
      name: 'GalgameCharacter', params: { slug: 'atri', person: '亚托莉' },
    })
  })

  it('漫画与动漫指向同一人物页，人物页能返回两个版本', async () => {
    const res = await getAllSearch('フリーレン')
    const row = res.data[0]
    expect(row).toMatchObject({ id: 'frieren', name: '芙莉莲', sourcetype: '角色' })
    expect(castForWork('comic', 24)).toEqual(castForWork('animation', 10941))
    expect(worksForCharacter(row.id).map(workLocation)).toEqual([
      { name: 'ComicIntroduce', query: { comicid: 24 } },
      { name: 'Animationintroduce', query: { animationid: 10941 } },
    ])
    expect(castForWork('novel', 1)[0].id).toBe(76)
  })

  it('作品筛选不把同名人物混入结果', async () => {
    const res = await getAllSearch({ search: '芙莉莲', mediaType: 'animation', genre: '奇幻' })
    expect(res.data).toEqual([])
    expect(request.get).toHaveBeenCalledTimes(1)
  })
})
