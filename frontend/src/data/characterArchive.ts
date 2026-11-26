import type { RouteLocationRaw } from 'vue-router'
import { galgameCharacterLocation } from '@/utils/searchResultLocation'

export type ArchiveWorkType = 'animation' | 'comic' | 'novel' | 'galgame'

export interface ArchiveWork {
  type: ArchiveWorkType
  id: number | string
  title: string
  cast: [id: number, role: number, voices?: string[]][]
  sources: number[]
}

export interface ArchivePerson {
  id: number
  name: string
  originalName?: string
  aliases?: string[]
  birthday?: string
  facts?: [label: string, value: string][]
  siteId?: number | string
  game?: [slug: string, name: string]
}

const normalize = (name: string) => name.normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '')
const workKey = (type: ArchiveWorkType, id: number | string) => `${type}:${id}`
const archiveId = (id: number) => `bgm-${id}`

interface ArchiveProfile {
  person: ArchivePerson
  works: ArchiveWork[]
}

type SearchEntry = [id: number, name: string, title: string, ...aliases: string[]]
const cache = new Map<string, Promise<unknown>>()

function readShard<T>(name: string): Promise<T> {
  let pending = cache.get(name)
  if (!pending) {
    pending = Promise.resolve().then(async () => {
      const response = await fetch(`${import.meta.env.BASE_URL}data/characters/${name}.json`)
      if (!response.ok) throw new Error('角色资料暂时无法读取')
      return response.json()
    }).catch(error => {
      cache.delete(name)
      throw error
    })
    cache.set(name, pending)
  }
  return pending as Promise<T>
}

export async function archiveCast(type: ArchiveWorkType, id: string | number) {
  const key = workKey(type, id)
  const bucket = [...key].reduce((sum, char) => sum + char.codePointAt(0)!, 0) % 16
  const works = await readShard<Record<string, [ArchivePerson, number][]>>(`work-${bucket}`)
  return (works[key] ?? []).map(([person, role]) => ({ person, role }))
}

export async function archiveProfile(id: number) {
  if (!Number.isSafeInteger(id) || id < 1) return undefined
  const profiles = await readShard<Record<string, ArchiveProfile>>(`profile-${id % 16}`)
  return profiles[String(id)]
}

export async function archiveWorksForSiteCharacter(id: number | string) {
  const links = await readShard<Record<string, ArchiveWork[]>>('links')
  return Object.hasOwn(links, id) ? links[String(id)] : []
}

export async function searchArchiveCharacters(keyword: string, limit?: number) {
  const query = normalize(keyword)
  if (keyword && !query) return []
  const searchable = await readShard<SearchEntry[]>('search')
  const exact: SearchEntry[] = [], prefix: SearchEntry[] = [], substring: SearchEntry[] = []
  for (const entry of searchable) {
    const names = [entry[1], ...entry.slice(3) as string[]].map(normalize)
    if (query && names.includes(query)) exact.push(entry)
    else if (!query || names.some(name => name.startsWith(query))) prefix.push(entry)
    else if (names.some(name => name.includes(query))) substring.push(entry)
  }
  return [...exact, ...prefix, ...substring].slice(0, limit).map(([id, name, workTitle]) => ({
    id: archiveId(id), name, sourcetype: '角色', workTitle,
  }))
}

export function archiveCharacterLocation(person: ArchivePerson): RouteLocationRaw {
  if (person.game) return galgameCharacterLocation(...person.game)
  return { name: 'Character', params: { id: person.siteId ?? archiveId(person.id) } }
}

export function archiveWorkLocation(work: ArchiveWork): RouteLocationRaw {
  if (work.type === 'comic') return { name: 'ComicIntroduce', query: { comicid: work.id } }
  if (work.type === 'novel') return { name: 'NovelIntroduce', query: { novelid: work.id } }
  if (work.type === 'galgame') return { name: 'GalgameIntroduce', query: { slug: work.id } }
  return { name: 'Animationintroduce', query: { animationid: work.id } }
}

export function archiveRoleLabel(role: number) {
  return { 1: '主角', 2: '配角', 3: '客串' }[role] ?? ''
}
