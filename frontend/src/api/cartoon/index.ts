import request from '@/utils/request'
enum API {
    cartoon_URL = '/cartoon',
    CARTOON_HOT_URL = '/cartoon/hot',
    CARTOON_CHAPTER_URL = '/cartoon/chapter',
    CARTOON_BYID_URL = '/cartoon/byid',
    CARTOON_PAGE_URL = '/cartoonpage',
}
export const getcartoon = (page: number, params: Record<string, string | number | undefined> = {}) => {
  const q = new URLSearchParams({ page: String(page) })
  if (params.genre) q.set('genre', String(params.genre))
  if (params.status) q.set('status', String(params.status))
  if (params.yearMin != null && params.yearMin !== '') q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null && params.yearMax !== '') q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', String(params.sortBy))
  return request.get<any, any>(`${API.cartoon_URL}?${q.toString()}`)
}
export const getcartoonhot = () => request.get<any, any>(API.CARTOON_HOT_URL)
export const getcartoonchapter = (data) => request.get<any, any>(API.CARTOON_CHAPTER_URL+`?id=${data}`)
export const getcartoonbyid = (data) => request.get<any, any>(API.CARTOON_BYID_URL + `?id=${data}`)
export const getcartoonpage = (data,datas) => request.get<any, any>(API.CARTOON_PAGE_URL+`?chapter=${data}&page=${datas}`)

