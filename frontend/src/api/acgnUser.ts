import request from '@/utils/request'

/** 后端用户态：足迹 / 收藏 / 订阅 / 阅读进度（按 X-Acgn-Client） */

export interface AcgnLibraryItemDto {
  type: string
  id: number
  name: string
  pic?: string
  at: number
  starred?: boolean
  starredAt?: number | null
}

export interface AcgnSubscribeItemDto {
  type: string
  id: number
  name: string
  pic?: string
  subscribedAt: number
}

export interface AcgnUserStateDto {
  library: AcgnLibraryItemDto[]
  subscriptions: AcgnSubscribeItemDto[]
  progress: {
    animation?: Record<string, number>
    comic?: Record<string, Record<string, unknown>>
    novel?: Record<string, { chapterno: number }>
  }
}

export const fetchAcgnUserState = () =>
  request.get<any, { code: number; data: AcgnUserStateDto }>('/acgn/user/state')

export const postAcgnLibraryVisit = (body: {
  mediaType: string
  mediaId: number
  name?: string
  pic?: string
}) =>
  request.post<any, { code: number; msg?: string }>('/acgn/user/library/visit', body)

export const postAcgnFavoriteToggle = (body: {
  mediaType: string
  mediaId: number
  name?: string
  pic?: string
}) =>
  request.post<any, { code: number; msg?: string; data?: { starred: boolean } }>(
    '/acgn/user/library/favorite-toggle',
    body,
  )

export const postAcgnSubscribeToggle = (body: {
  mediaType: string
  mediaId: number
  name?: string
  pic?: string
}) =>
  request.post<any, { code: number; msg?: string; data?: { subscribed: boolean } }>(
    '/acgn/user/subscribe/toggle',
    body,
  )

export const postAcgnProgressSave = (
  mediaType: string,
  mediaId: number,
  payload: Record<string, unknown>,
) =>
  request.post<any, { code: number; msg?: string }>('/acgn/user/progress', {
    mediaType,
    mediaId,
    payload,
  })

export const postAcgnImportLegacyRows = (rows: AcgnLibraryItemDto[]) =>
  request.post<any, { code: number; msg?: string }>('/acgn/user/import-legacy', rows)
