import data from './galgameDiscovery.json'
import { getGalgame } from '@/api/galgame'

export const discovery = data
export type DiscoveryEntry = typeof data.entries[number]
export function relatedFor(slug?: string) {
  const current = data.entries.find(entry => entry.slug === slug)
  if (!current) return []
  const result: { entry: DiscoveryEntry; reason: string }[] = []
  for (const relation of data.series.filter(relation => relation.from === slug)) {
    const entry = data.entries.find(entry => entry.slug === relation.to)
    if (entry) result.push({ entry, reason: relation.label })
  }
  for (const entry of data.entries) {
    if (entry.slug === slug || result.some(item => item.entry.slug === entry.slug)) continue
    const studio = entry.studioKeys.find(key => current.studioKeys.includes(key))
    if (studio) result.push({ entry, reason: `同开发商 · ${studio}` })
  }
  for (const entry of data.entries) {
    if (entry.slug === slug || result.some(item => item.entry.slug === entry.slug)) continue
    const topic = data.topics.find(topic => topic.id !== 'chinese' && entry.topics.includes(topic.id) && current.topics.includes(topic.id))
    if (topic) result.push({ entry, reason: `同专题 · ${topic.title}` })
  }
  return result.slice(0, 3)
}

// Resolve stable slugs through the real paginated API; database IDs are never seeded into links.
const ids = new Map<string, number>()
let resolving: Promise<void> | undefined
async function readCatalogIds() {
  let page = 1
  const seen = new Set<string>()
  while (true) {
    const response = await getGalgame(page++)
    if (!Array.isArray(response?.data)) throw new Error('作品列表返回异常，请稍后重试。')
    const rows = response.data as { id: number; slug: string }[]
    const signature = rows.map(row => row.id).join(',')
    if (rows.length && seen.has(signature)) throw new Error('作品分页返回重复，请从列表进入作品。')
    seen.add(signature)
    for (const row of rows) if (Number.isSafeInteger(row.id) && row.id > 0) ids.set(row.slug, row.id)
    if (rows.length < 20) break
  }
}
export async function idForSlug(slug: string) {
  if (ids.has(slug)) return ids.get(slug)
  if (!resolving) resolving = readCatalogIds().finally(() => { resolving = undefined })
  await resolving
  return ids.get(slug)
}
