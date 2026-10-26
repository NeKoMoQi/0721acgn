import request from '@/utils/request'

enum API {
  GALGAME_URL = '/galgame',
  GALGAME_HOT_URL = '/galgame/hot',
  GALGAME_BYID_URL = '/galgame/byid',
}

export interface GalgameListParams {
  genre?: string
  yearMin?: number
  yearMax?: number
  sortBy?: 'default' | 'rating' | 'release'
}

export const getGalgame = (page: number, params: GalgameListParams = {}) => {
  const q = new URLSearchParams({ page: String(page) })
  if (params.genre) q.set('genre', params.genre)
  if (params.yearMin != null) q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null) q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', params.sortBy)
  return request.get<any, any>(`${API.GALGAME_URL}?${q.toString()}`)
}

export const getGalgameHot = () => request.get<any, any>(API.GALGAME_HOT_URL)

export const getGalgameById = (id: number | string) =>
  request.get<any, any>(`${API.GALGAME_BYID_URL}?id=${id}`)
