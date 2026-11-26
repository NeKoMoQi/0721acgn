import request from '@/utils/request'
import { searchArchiveCharacters } from '@/data/characterArchive'

export interface GlobalSearchParams {
  search?: string
  genre?: string
  status?: string
  yearMin?: number
  yearMax?: number
  /** default | hot | update */
  sortBy?: string
  /** comic | animation | novel | character | 不传为全站合并 */
  mediaType?: string
  limit?: number
  page?: number
}

enum API {
  ALL_SEARCH_API = '/searchresult',
}

export interface SearchRow {
  id: number | string
  name: string
  picurl?: string
  sourcetype: string
  resourceEnd?: number | null
  workTitle?: string
  galgameSlug?: string
  matchRank?: number | null
}

interface SearchResponse {
  code: number
  data: SearchRow[] | null | undefined
  total?: number
  page?: number
  pageSize?: number
}

function fromDatabase(q: URLSearchParams, page?: number): Promise<SearchResponse> {
  const params = new URLSearchParams(q)
  if (page != null) params.set('page', String(page))
  const qs = params.toString()
  return request.get<unknown, { code: number; data: SearchRow[] | {
    items: SearchRow[]; total: number; page: number; pageSize: number
  } }>(qs ? `${API.ALL_SEARCH_API}?${qs}` : API.ALL_SEARCH_API).then(response => {
    if (Array.isArray(response.data)) return { ...response, data: response.data }
    const payload = response.data
    return { ...response, data: payload?.items, total: payload?.total, page: payload?.page, pageSize: payload?.pageSize }
  })
}

async function characterPage(q: URLSearchParams, requested: number, local: SearchRow[]): Promise<SearchResponse> {
  const base = await fromDatabase(q, 1)
  if (base.code !== 200 || !Array.isArray(base.data)) return base
  const pageSize = base.pageSize ?? Math.max(1, Number(q.get('limit')) || 24)
  const serverTotal = base.total ?? base.data.length
  const total = serverTotal + local.length
  const page = Math.max(1, Math.min(requested, Math.ceil(total / pageSize)))
  const start = (page - 1) * pageSize
  const end = start + pageSize
  const localRows = end > serverTotal ? local.slice(Math.max(0, start - serverTotal), end - serverTotal) : []
  const window = start >= serverTotal ? undefined : page === 1 ? base : await fromDatabase(q, page)
  if (window && (window.code !== 200 || !Array.isArray(window.data))) return window
  return { code: 200, data: [...(window?.data ?? []), ...localRows].slice(0, pageSize), total, page, pageSize }
}

export async function getAllSearch(input: GlobalSearchParams | string): Promise<SearchResponse> {
  const params: GlobalSearchParams = typeof input === 'string' ? { search: input } : input
  const search = params.search?.trim() || ''
  const normalized = search.normalize('NFKC').toLowerCase()
  const q = new URLSearchParams()
  if (search) q.set('search', search)
  if (params.genre) q.set('genre', params.genre)
  if (params.status) q.set('status', params.status)
  if (params.yearMin != null) q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null) q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', params.sortBy)
  if (params.mediaType) q.set('mediaType', params.mediaType)
  if (params.limit != null) q.set('limit', String(params.limit))
  const includeLocal = params.mediaType === 'character'
    || (search && !params.mediaType && !params.genre && !params.status && params.yearMin == null && params.yearMax == null)
  const pagedCharacters = params.mediaType === 'character' && params.page != null
  if (!includeLocal || (!pagedCharacters && (params.page ?? 1) > 1)) return fromDatabase(q, params.page)
  const loadingServer = pagedCharacters ? null : fromDatabase(q, params.page)
  const [response, { matchingCast }, { searchGalgameCharacters }, archive] = await Promise.all([
    loadingServer,
    import('@/data/characterAssociations'),
    import('@/data/galgameMedia'),
    searchArchiveCharacters(search, pagedCharacters ? undefined : 8).catch(() => []),
  ])
  const editorial: SearchRow[] = matchingCast(search).filter(person => !params.page || typeof person.id === 'string'
    || (normalized && (person.id === 1 || !person.name.normalize('NFKC').toLowerCase().includes(normalized)))).map(person => ({
    id: person.id, name: person.name, sourcetype: '角色', workTitle: person.title,
  }))
  const galgame: SearchRow[] = searchGalgameCharacters(search).map(({ slug, person, title }) => ({
    id: `${slug}:${person.name}`, name: person.name, picurl: person.image,
    sourcetype: '角色', workTitle: title, galgameSlug: slug,
  }))
  const added = [...editorial, ...galgame, ...archive]
  if (pagedCharacters) {
    return characterPage(q, params.page!, added)
  }
  if (!response || response.code !== 200 || !Array.isArray(response.data)) return response ?? { code: 500, data: null }
  const byId = new Map(added.map(row => [String(row.id), row]))
  const serverCharacters = new Set(response.data.filter(row => row.sourcetype === '角色').map(row => String(row.id)))
  const server = response.data.map(row => row.sourcetype === '角色' && byId.has(String(row.id))
    ? { ...byId.get(String(row.id)), ...row } : row)
  const local = added.filter(row => !serverCharacters.has(String(row.id)))
  const data = params.mediaType === 'character' && !search ? [...local, ...server] : [...server, ...local]
  if (!params.page && search && (!params.sortBy || params.sortBy === 'default' || params.mediaType === 'character')) {
    const rank = (name: string) => {
      const value = name.normalize('NFKC').toLowerCase()
      return value === normalized ? 0 : value.startsWith(normalized) ? 1 : 2
    }
    data.sort((a, b) => (a.matchRank ?? rank(a.name)) - (b.matchRank ?? rank(b.name)))
  }
  return { ...response, data: params.page ? data : params.limit ? data.slice(0, params.limit) : data }
}
