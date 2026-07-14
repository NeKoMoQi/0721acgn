import request from '@/utils/request'

export interface RelatedWorkVo {
  type: 'animation' | 'comic' | 'novel'
  id: number
  name: string
  picurl: string
}

export const getRelated = (srcType: string, srcId: number) =>
  request.get<any, any>(
    `/related?srcType=${encodeURIComponent(srcType)}&srcId=${encodeURIComponent(String(srcId))}`,
  )
