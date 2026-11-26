import request from '@/utils/request'

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
  if (params.page != null) q.set('page', String(params.page))
  if (params.limit != null) q.set('limit', String(params.limit))
  const qs = q.toString()
  const fromServer = request.get<unknown, { code: number; data: SearchRow[] | {
    items: SearchRow[]; total: number; page: number; pageSize: number
  } }>(qs ? `${API.ALL_SEARCH_API}?${qs}` : API.ALL_SEARCH_API).then(response => {
    if (Array.isArray(response.data)) return { ...response, data: response.data }
    const payload = response.data
    return { ...response, data: payload?.items, total: payload?.total, page: payload?.page, pageSize: payload?.pageSize }
  })
  const includeLocal = params.mediaType === 'character'
    || (search && !params.mediaType && !params.genre && !params.status && params.yearMin == null && params.yearMax == null)
  if (!includeLocal || (params.page ?? 1) > 1) return fromServer
  const [response, { matchingCast }, { searchGalgameCharacters }] = await Promise.all([
    fromServer,
    import('@/data/characterAssociations'),
    import('@/data/galgameMedia'),
  ])
  if (response?.code !== 200 || !Array.isArray(response.data)) return response
  const editorial: SearchRow[] = matchingCast(search).filter(person => !params.page || typeof person.id === 'string'
    || (normalized && (person.id === 1 || !person.name.normalize('NFKC').toLowerCase().includes(normalized)))).map(person => ({
    id: person.id, name: person.name, sourcetype: '角色', workTitle: person.title,
  }))
  const galgame: SearchRow[] = searchGalgameCharacters(search).map(({ slug, person, title }) => ({
    id: `${slug}:${person.name}`, name: person.name, picurl: person.image,
    sourcetype: '角色', workTitle: title, galgameSlug: slug,
  }))
  const added = [...editorial, ...galgame]
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
