export type NotifyKind = 'subscribe_update' | 'comment_reply' | 'review_result' | 'system'

export interface AcgnNotice {
  id: string
  kind: NotifyKind
  title: string
  body: string
  at: number
  read: boolean
}

const K = 'acgn_notifications_v1'
const INIT = 'acgn_notifications_initialized_v1'

function readAll(): AcgnNotice[] {
  try {
    const raw = localStorage.getItem(K)
    if (!raw) return []
    const arr = JSON.parse(raw) as AcgnNotice[]
    return Array.isArray(arr) ? arr : []
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

function seedBootstrap(): AcgnNotice[] {
  const now = Date.now()
  return [
    {
      id: 'sys_welcome',
      kind: 'system',
      title: '系统公告',
      body: '0721 ACGN 演示站：欢迎来到个人消息中心。本站收藏、足迹与订阅会与设备匿名 ID 绑定同步。',
      at: now - 86400000 * 3,
      read: false,
    },
    {
      id: 'demo_review',
      kind: 'review_result',
      title: '投稿审核',
      body: '示例：你提交的 PV 已通过自动入库（演示数据）。',
      at: now - 86400000,
      read: false,
    },
    {
      id: 'demo_reply',
      kind: 'comment_reply',
      title: '评论互动',
      body: '腐竹 回复了你的评论：感谢追更～。',
      at: now - 3600000,
      read: false,
    },
    {
      id: 'demo_sub',
      kind: 'subscribe_update',
      title: '订阅更新',
      body: '你订阅的某部作品刚刚更新了预告 / 剧集信息（演示文案）。',
      at: now - 120000,
      read: false,
    },
  ]
}

export function ensureNotificationsBootstrapped() {
  if (typeof localStorage === 'undefined') return
  try {
    if (localStorage.getItem(INIT) === '1') return
    const cur = readAll()
    if (cur.length === 0) {
      writeAll(seedBootstrap())
    }
    localStorage.setItem(INIT, '1')
  } catch {
    //
  }
}

export function listNotifications(): AcgnNotice[] {
  ensureNotificationsBootstrapped()
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

/** 程序化追加一条提醒（例如在「投稿」表单提交后） */
export function pushNotification(row: Omit<AcgnNotice, 'read'> & { read?: boolean }) {
  ensureNotificationsBootstrapped()
  const notice: AcgnNotice = {
    read: false,
    ...row,
  }
  const next = readAll().filter((x) => x.id !== notice.id)
  next.unshift(notice)
  writeAll(next)
}

/** 当有订阅条目时可用于生成文案（占位：由消息页按需调用） */
export function notifySubscribeDummy(workName: string) {
  pushNotification({
    id: `sub_${Date.now()}`,
    kind: 'subscribe_update',
    title: '订阅作品更新',
    body: `${workName}：可能有新话 / 新卷（演示占位通知）。`,
    at: Date.now(),
  })
}
