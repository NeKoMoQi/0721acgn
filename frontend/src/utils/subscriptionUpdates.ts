import { getAnimationEpisodes } from '@/api/animation'
import { getcartoonchapter } from '@/api/cartoon'
import { getnovelchapter } from '@/api/novel'
import { ensureAcgnUserHydrated, listSubscriptions, type SubscribeEntry } from './acgnLocal'
import { pushNotification, type AcgnNotice } from './acgnNotifications'

interface Release {
  id: string
  label: string
  to: NonNullable<AcgnNotice['to']>
}

async function loadReleases(entry: SubscribeEntry): Promise<Release[]> {
  const id = entry.id
  const response = await (entry.type === 'animation' ? getAnimationEpisodes(id)
    : entry.type === 'comic' ? getcartoonchapter(id) : getnovelchapter(id))
  if (response?.code !== 200 || !Array.isArray(response.data)) throw new Error('目录暂不可用')
  const releases: Release[] = []
  for (const row of response.data) {
    if (entry.type === 'animation' && row.id && row.videoUrl?.trim()) {
      releases.push({ id: String(row.id), label: row.title || `第 ${row.episodeNo} 集`,
        to: { name: 'AnimationWatch', query: { animationid: id, episodeid: row.id } } })
    } else if (entry.type === 'comic' && row.chapterid) {
      releases.push({ id: String(row.chapterid), label: row.title || `第 ${row.chapterno} 章`,
        to: { name: 'Comicsee', query: { comicid: id, chapterid: row.chapterid, chapterno: row.chapterno } } })
    } else if (entry.type === 'novel' && row.novelurl?.trim()) {
      releases.push({ id: String(row.chapterno), label: row.title || `第 ${row.chapterno} 卷`,
        to: { name: 'Novelsee', query: { novelid: id, chapterno: row.chapterno } } })
    }
  }
  return releases
}

const CHECK_INTERVAL = 5 * 60 * 1000
const checkedAt = new Map<string, number>()
const pending = new Map<string, Promise<void>>()

export function checkSubscription(entry: SubscribeEntry): Promise<void> {
  const key = `${entry.type}:${entry.id}:${entry.subscribedAt}`
  const existing = pending.get(key)
  if (existing) return existing
  if (Date.now() - (checkedAt.get(key) ?? 0) < CHECK_INTERVAL) return Promise.resolve()
  const result = (async () => {
    const releases = await loadReleases(entry)
    if (!listSubscriptions().some(item => item.type === entry.type && item.id === entry.id
      && item.subscribedAt === entry.subscribedAt)) return
    const storageKey = `acgn_subscription_releases_v1:${entry.type}:${entry.id}`
    const raw = localStorage.getItem(storageKey)
    const previous = new Set<string>(raw ? JSON.parse(raw) : [])
    for (const release of releases) {
      if (previous.has(release.id)) continue
      previous.add(release.id)
      if (raw != null) {
        pushNotification({
          id: `release:${entry.type}:${entry.id}:${release.id}`,
          kind: 'subscribe_update',
          title: `《${entry.name}》有更新`,
          body: release.label,
          to: release.to,
          at: Date.now(),
        })
      }
    }
    localStorage.setItem(storageKey, JSON.stringify([...previous]))
    checkedAt.set(key, Date.now())
  })().catch(() => {
    // 目录或本地存储暂不可用时保留上一次记录，留待下次检查。
  }).finally(() => pending.delete(key))
  pending.set(key, result)
  return result
}

export async function refreshSubscriptionUpdates() {
  await ensureAcgnUserHydrated()
  const entries = listSubscriptions()
  for (let i = 0; i < entries.length; i += 6) {
    await Promise.all(entries.slice(i, i + 6).map(checkSubscription))
  }
}
