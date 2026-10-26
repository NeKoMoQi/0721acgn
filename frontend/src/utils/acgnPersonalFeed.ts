import type { AcgnMediaType } from '@/utils/acgnLocal'

export interface MyPublishedCommentRow {
  type: AcgnMediaType
  mediaId: number
  workName: string
  text: string
  at: number
  channel: 'server' | 'local'
}

const K = 'acgn_my_comments_v1'

function readRows(): MyPublishedCommentRow[] {
  try {
    const raw = localStorage.getItem(K)
    if (!raw) return []
    const arr = JSON.parse(raw) as MyPublishedCommentRow[]
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

/** 记录在「个人中心 · 我的评论」中展示的本地列表（服务端成功与离线回退共用） */
export function appendMyPublishedComment(row: Omit<MyPublishedCommentRow, 'at'> & { at?: number }) {
  const at = typeof row.at === 'number' ? row.at : Date.now()
  const next: MyPublishedCommentRow = { ...row, at }
  try {
    const prev = readRows().filter(
      (x) =>
        !(
          x.type === next.type &&
          x.mediaId === next.mediaId &&
          x.at === next.at &&
          x.text === next.text
        ),
    )
    prev.unshift(next)
    localStorage.setItem(K, JSON.stringify(prev.slice(0, 200)))
  } catch {
    //
  }
}

export function listMyPublishedComments(): MyPublishedCommentRow[] {
  return readRows().sort((a, b) => b.at - a.at)
}

export interface UserUploadRecord {
  id: string
  kind: 'preview_video' | 'resource_submit'
  /** 简述 */
  label: string
  detail?: string
  /** 动漫 / 漫画 / 小说 id */
  relatedId?: number
  relatedName?: string
  at: number
  /** 前端演示：已通过 / 待审核 */
  auditStatus?: 'pending' | 'approved' | 'rejected'
}

const K_UPLOAD = 'acgn_user_uploads_v1'

export function appendUserUploadRecord(row: Omit<UserUploadRecord, 'id' | 'at'> & { id?: string; at?: number }) {
  const id = row.id ?? `u_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  const at = row.at ?? Date.now()
  const full: UserUploadRecord = {
    auditStatus: 'approved',
    ...row,
    id,
    at,
  }
  try {
    const raw = localStorage.getItem(K_UPLOAD)
    const prev: UserUploadRecord[] = raw ? JSON.parse(raw) : []
    const list = Array.isArray(prev) ? prev : []
    list.unshift(full)
    localStorage.setItem(K_UPLOAD, JSON.stringify(list.slice(0, 120)))
    return full
  } catch {
    return full
  }
}

export function listUserUploadRecords(): UserUploadRecord[] {
  try {
    const raw = localStorage.getItem(K_UPLOAD)
    if (!raw) return []
    const arr = JSON.parse(raw) as UserUploadRecord[]
    return Array.isArray(arr) ? arr.sort((a, b) => b.at - a.at) : []
  } catch {
    return []
  }
}
