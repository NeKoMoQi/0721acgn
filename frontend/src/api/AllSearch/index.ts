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

export async function getAllSearch(input: GlobalSearchParams | string) {
  const params: GlobalSearchParams = typeof input === 'string' ? { search: input } : input
  const search = params.search?.trim() || ''
  const q = new URLSearchParams()
  if (search) q.set('search', search)
  if (params.genre) q.set('genre', params.genre)
  if (params.status) q.set('status', params.status)
  if (params.yearMin != null) q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null) q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', params.sortBy)
  if (params.mediaType) q.set('mediaType', params.mediaType)
  if (params.limit != null) q.set('limit', String(params.limit))
  const qs = q.toString()
  const fromServer = request.get<unknown, { code: number; data: SearchRow[] }>(qs ? `${API.ALL_SEARCH_API}?${qs}` : API.ALL_SEARCH_API)
  const includeLocal = params.mediaType === 'character'
    || (search && !params.mediaType && !params.genre && !params.status && params.yearMin == null && params.yearMax == null)
  if (!includeLocal) return fromServer
  const [response, { matchingCast }, { searchGalgameCharacters }] = await Promise.all([
    fromServer,
    import('@/data/characterAssociations'),
    import('@/data/galgameMedia'),
  ])
  if (response?.code !== 200 || !Array.isArray(response.data)) return response
  const editorial: SearchRow[] = matchingCast(search).map(person => ({
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
  if (search && (!params.sortBy || params.sortBy === 'default' || params.mediaType === 'character')) {
    const normalized = search.normalize('NFKC').toLowerCase()
    const rank = (name: string) => {
      const value = name.normalize('NFKC').toLowerCase()
      return value === normalized ? 0 : value.startsWith(normalized) ? 1 : 2
    }
    data.sort((a, b) => (a.matchRank ?? rank(a.name)) - (b.matchRank ?? rank(b.name)))
  }
  return { ...response, data: params.limit ? data.slice(0, params.limit) : data }
}
