import request from '@/utils/request'
enum API {
    ANIMATION_URL = '/animation',
    ANIMATION_HOT_URL = '/animation/hot',
    ANIMATION_BYID_URL = '/animation/byid',
}
export const getAnimation = (page: number, params: Record<string, string | number | undefined> = {}) => {
  const q = new URLSearchParams({ page: String(page) })
  if (params.genre) q.set('genre', String(params.genre))
  if (params.status) q.set('status', String(params.status))
  if (params.yearMin != null && params.yearMin !== '') q.set('yearMin', String(params.yearMin))
  if (params.yearMax != null && params.yearMax !== '') q.set('yearMax', String(params.yearMax))
  if (params.sortBy) q.set('sortBy', String(params.sortBy))
  return request.get<any, any>(`${API.ANIMATION_URL}?${q.toString()}`)
}
export const getAnimationhot = () => request.get<any, any>(API.ANIMATION_HOT_URL)
export const getAnimationById = (data) => request.get<any, any>(API.ANIMATION_BYID_URL + `?id=${data}`)
export const getAnimationEpisodes = (animationId: number) =>
  request.get<any, any>(`/animation/episodes?animationId=${animationId}`)
export const getAnimationEpisodeById = (id: number) =>
  request.get<any, any>(`/animation/episode?id=${id}`)

/** 演示：PV 上传到 OSS 并写入番剧预览地址 */
export const uploadAnimationPreviewVideo = (animationId: number, file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<any, any>(`/animation/preview-video?animationId=${animationId}`, fd, {
    timeout: 300000,
  })
}

export const importAnimationPreviewVideoFromPath = (animationId: number, sourcePath: string) => {
  const q = new URLSearchParams({
    animationId: String(animationId),
    sourcePath,
  })
  return request.post<any, any>(`/animation/preview-video/local?${q.toString()}`, undefined, {
    timeout: 300000,
  })
}

/** 正版公开 PV/先导片（YouTube 嵌入元数据），非正片爬取 */
export const getAnimationPublicClips = (animationId: number) =>
  request.get<any, any>(`/animation/public-clips?animationId=${animationId}`)
