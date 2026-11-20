import request from '@/utils/request'

enum API {
    NOVEL_URL = '/novel',
    NOVEL_CHAPTER_URL = '/novel/chapter',
    NOVEL_BYID_URL = '/novel/byid',
    NOVEL_COUNT_URL = '/novel/count',
    NOVEL_CHAPTER_BYID_URL = "/novel/chapter/byid",
    NOVEL_CONTENT_URL = "/novel/content",
    NOVEL_HOT_URL="/novel/hot"
}
export const getnovel = (page: number, params: Record<string, string | number | undefined> = {}) => {
  const q = new URLSearchParams({ page: String(page) })
  if (params.genre) q.set('genre', String(params.genre))
  if (params.status) q.set('status', String(params.status))
  if (params.yearMin != null && params.yearMin !== '') q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null && params.yearMax !== '') q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', String(params.sortBy))
  return request.get<any, any>(`${API.NOVEL_URL}?${q.toString()}`)
}
export const getnovelchapter = (data) => request.get<any, any>(API.NOVEL_CHAPTER_URL+`?id=${data}`)
export const getnovelbyid = (data) => request.get<any, any>(API.NOVEL_BYID_URL+`?id=${data}`)
export const getnovelcount = (data) => request.get<any, any>(API.NOVEL_COUNT_URL+`?id=${data}`)
export const getnovelchapterbyid = (data,datas) => request.get<any, any>(API.NOVEL_CHAPTER_BYID_URL+`?id=${data}&no=${datas}`)
// 正文走站点同源接口取字节：浏览器直连 OSS 会被 CORS 拦，epub.js 拿不到文件。
export const getnovelcontent = (data, datas) =>
  request.get<any, any>(API.NOVEL_CONTENT_URL + `?id=${data}&no=${datas}`, {
    responseType: 'arraybuffer',
    timeout: 60000,
  })
export const getnovelhot = () => request.get<any, any>(API.NOVEL_HOT_URL)