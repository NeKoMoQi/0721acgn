import request from '@/utils/request'

/** 后端互动：点赞计数 + 短评持久化 */

export interface AcgnEngagementDto {
  likes: number
  dislikes: number
  myVote: null | 'up' | 'down'
}

export interface AcgnCommentDto {
  nickname: string
  avatar?: string
  body: string
  at: number
}

export const fetchAcgnEngagement = (mediaType: string, mediaId: number) =>
  request.get<any, { code: number; data: AcgnEngagementDto }>(
    `/acgn/social/engagement?${new URLSearchParams({ mediaType, mediaId: String(mediaId) }).toString()}`,
  )

export const postAcgnVote = (mediaType: string, mediaId: number, side: 'up' | 'down') =>
  request.post<any, { code: number; msg?: string; data: AcgnEngagementDto }>('/acgn/social/vote', {
    mediaType,
    mediaId,
    side,
  })

export const fetchAcgnComments = (mediaType: string, mediaId: number) =>
  request.get<any, { code: number; data: AcgnCommentDto[] }>(
    `/acgn/social/comments?${new URLSearchParams({ mediaType, mediaId: String(mediaId) }).toString()}`,
  )

export const postAcgnComment = (
  mediaType: string,
  mediaId: number,
  nickname: string | undefined,
  avatar: string | undefined,
  body: string,
) =>
  request.post<any, { code: number; msg?: string }>('/acgn/social/comments', {
    mediaType,
    mediaId,
    avatar: avatar || '',
    nickname: nickname || '访客',
    body,
  })
