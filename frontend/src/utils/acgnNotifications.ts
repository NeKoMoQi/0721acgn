import type { LocationQueryRaw } from 'vue-router'

export type NotifyKind = 'subscribe_update' | 'comment_reply' | 'review_result' | 'system'

export interface AcgnNotice {
  id: string
  kind: NotifyKind
  title: string
  body: string
  at: number
  read: boolean
  to?: { name: string; query: LocationQueryRaw }
}

const K = 'acgn_notifications_v1'
const demoIds = new Set(['sys_welcome', 'demo_review', 'demo_reply', 'demo_sub'])

function readAll(): AcgnNotice[] {
  try {
    const raw = localStorage.getItem(K)
    if (!raw) return []
    const arr = JSON.parse(raw) as AcgnNotice[]
    return Array.isArray(arr) ? arr.filter(item => item && !demoIds.has(item.id) && item.title !== '投稿已收到（演示）') : []
  } catch {
    return []
  }
}

function writeAll(rows: AcgnNotice[]) {
  try {
    localStorage.setItem(K, JSON.stringify(rows.slice(0, 500)))
  } catch {
    //
  }
}

export function listNotifications(): AcgnNotice[] {
  return readAll().sort((a, b) => b.at - a.at)
}

export function unreadNotificationCount(): number {
  return listNotifications().filter((x) => !x.read).length
}

export function markNotificationRead(id: string) {
  const rows = readAll().map((x) => (x.id === id ? { ...x, read: true } : x))
  writeAll(rows)
}

export function markAllNotificationsRead() {
  writeAll(readAll().map((x) => ({ ...x, read: true })))
}

/** 追加一条提醒，保留已读状态 */
export function pushNotification(row: Omit<AcgnNotice, 'read'> & { read?: boolean }) {
  const notice: AcgnNotice = {
    read: false,
    ...row,
  }
  const next = readAll()
  if (next.some(item => item.id === notice.id)) return
  next.unshift(notice)
  writeAll(next)
}
