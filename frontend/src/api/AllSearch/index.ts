import request from '@/utils/request'

export interface GlobalSearchParams {
  search?: string
  genre?: string
  status?: string
  yearMin?: number
  yearMax?: number
  /** default | hot | update */
  sortBy?: string
  /** comic | animation | novel | 不传为全站合并 */
  mediaType?: string
  limit?: number
}

enum API {
  ALL_SEARCH_API = '/searchresult',
}

export const getAllSearch = (params: GlobalSearchParams | string) => {
  if (typeof params === 'string') {
    return request.get<any, any>(`${API.ALL_SEARCH_API}?${new URLSearchParams({ search: params }).toString()}`)
  }
  const q = new URLSearchParams()
  if (params.search != null && params.search !== '') q.set('search', String(params.search))
  if (params.genre) q.set('genre', params.genre)
  if (params.status) q.set('status', params.status)
  if (params.yearMin != null) q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null) q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', params.sortBy)
  if (params.mediaType) q.set('mediaType', params.mediaType)
  if (params.limit != null) q.set('limit', String(params.limit))
  const qs = q.toString()
  return request.get<any, any>(qs ? `${API.ALL_SEARCH_API}?${qs}` : API.ALL_SEARCH_API)
}
