import { ElMessage } from 'element-plus'
import type { AcgnUserStateDto, AcgnLibraryItemDto } from '@/api/acgnUser'
import {
  fetchAcgnUserState,
  postAcgnLibraryVisit,
  postAcgnFavoriteToggle,
  postAcgnSubscribeToggle,
  postAcgnProgressSave,
  postAcgnImportLegacyRows,
} from '@/api/acgnUser'

export type AcgnMediaType = 'animation' | 'comic' | 'novel'

const K_ANON_CLIENT = 'acgn_anon_client_v1'

/** 已从旧版浏览器 localStorage 迁出后不再作为数据源（仅做一次导入） */
const LEGACY_BOOK = 'acgn_bookshelf_v1'
const LEGACY_HIST = 'acgn_history_v1'
const LEGACY_MERGED = 'acgn_bookshelf_merged_into_history_v1'
const LEGACY_SUB = 'acgn_subscribe_v1'
const LEGACY_COMIC = 'acgn_progress_comic_v1'
const LEGACY_NOVEL = 'acgn_progress_novel_v1'
const LEGACY_ANIM = 'acgn_progress_animation_ep_v1'
const SESS_LEGACY_MIGRATE = 'acgn_legacy_user_data_migrated_v1'

export interface BookshelfItem {
  type: AcgnMediaType
  id: number
  name: string
  pic?: string
  addedAt: number
}

export interface HistoryItem {
  type: AcgnMediaType
  id: number
  name: string
  pic?: string
  /** 最近一次访问 */
  at: number
  /** 已从「书架」迁入：收藏标记 */
  starred?: boolean
  /** 点开「收藏」的时间 */
  starredAt?: number
}

let historyMem: HistoryItem[] = []
let subsMem: SubscribeEntry[] = []
const comicProgMem: Record<string, ComicProgress> = {}
const novelProgMem: Record<string, { chapterno: number }> = {}
const animEpMem: Record<string, number> = {}

let hydrated = false
let userDataInitPromise: Promise<void> | null = null

/** 初始化或刷新与后端的足迹 / 订阅 / 进度 */
export async function ensureAcgnUserHydrated(): Promise<void> {
  if (hydrated) return
  if (!userDataInitPromise) {
    userDataInitPromise = doHydrate()
  }
  await userDataInitPromise
}

function readLsJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

function applyServerState(raw: AcgnUserStateDto) {
  const lib = raw.library || []
  historyMem = lib.map((r) => mapLibraryRow(r))
  subsMem = (raw.subscriptions || []).map((s) => ({
    type: s.type as AcgnMediaType,
    id: s.id,
    name: s.name,
    pic: s.pic,
    subscribedAt: s.subscribedAt,
  }))
  const p = raw.progress || {}
  Object.keys(comicProgMem).forEach((k) => delete comicProgMem[k])
  Object.keys(novelProgMem).forEach((k) => delete novelProgMem[k])
  Object.keys(animEpMem).forEach((k) => delete animEpMem[k])
  const an = p.animation || {}
  for (const k of Object.keys(an)) {
    const v = (an as Record<string, number>)[k]
    if (typeof v === 'number' && Number.isFinite(v)) animEpMem[k] = v
  }
  const co = p.comic || {}
  for (const k of Object.keys(co)) {
    comicProgMem[k] = coerceComicProg((co as Record<string, unknown>)[k])
  }
  const nv = p.novel || {}
  for (const k of Object.keys(nv)) {
    const row = (nv as Record<string, { chapterno: number }>)[k]
    if (row && typeof row.chapterno === 'number') novelProgMem[k] = { chapterno: row.chapterno }
  }
}

function coerceComicProg(x: unknown): ComicProgress {
  if (!x || typeof x !== 'object') return { chapterId: '', chapterno: 1, imagePage: 1 }
  const o = x as Record<string, unknown>
  return {
    chapterId: typeof o.chapterId === 'string' ? o.chapterId : String(o.chapterId ?? ''),
    chapterno: typeof o.chapterno === 'number' ? o.chapterno : Number(o.chapterno) || 1,
    imagePage: typeof o.imagePage === 'number' ? o.imagePage : Number(o.imagePage) || 1,
  }
}

function mapLibraryRow(r: AcgnLibraryItemDto): HistoryItem {
  return {
    type: r.type as AcgnMediaType,
    id: r.id,
    name: r.name,
    pic: r.pic,
    at: typeof r.at === 'number' ? r.at : Date.now(),
    starred: !!r.starred,
    starredAt:
      r.starredAt != null && typeof r.starredAt === 'number' ? r.starredAt : undefined,
  }
}

async function doHydrate() {
  try {
    try {
      const res = await fetchAcgnUserState()
      if (res?.code === 200 && res.data) {
        applyServerState(res.data)
      } else {
        applyServerState({
          library: [],
          subscriptions: [],
          progress: {},
        })
      }
    } catch {
      applyServerState({
        library: [],
        subscriptions: [],
        progress: {},
      })
    }
    await maybeMigrateLegacyUserDataOnce()
  } finally {
    hydrated = true
  }
}

function collectLegacyMergedHistory(): HistoryItem[] {
  if (typeof localStorage === 'undefined') return []
  try {
    if (localStorage.getItem(LEGACY_MERGED)) {
      const h = readLsJson<HistoryItem[]>(LEGACY_HIST, [])
      return Array.isArray(h) ? [...h].slice(0, 320) : []
    }
    const books = readLsJson<BookshelfItem[]>(LEGACY_BOOK, [])
    let hist = readLsJson<HistoryItem[]>(LEGACY_HIST, [])
    const now = Date.now()
    if (books.length === 0) return Array.isArray(hist) ? [...hist].slice(0, 320) : []
    for (const b of books) {
      const i = hist.findIndex((x) => x.type === b.type && x.id === b.id)
      const tAdded = typeof b.addedAt === 'number' ? b.addedAt : now
      if (i >= 0) {
        const row = hist[i]
        row.starred = true
        row.starredAt = tAdded
        row.name = row.name || b.name
        row.pic = row.pic || b.pic
      } else {
        hist.unshift({
          ...b,
          at: tAdded,
          starred: true,
          starredAt: tAdded,
        })
      }
    }
    hist = hist.slice(0, 320)
    return hist
  } catch {
    return []
  }
}

async function postLegacyProgress() {
  const comicAll = readLsJson<Record<string, ComicProgress>>(LEGACY_COMIC, {})
  for (const k of Object.keys(comicAll)) {
    const id = Number(k)
    if (!Number.isFinite(id)) continue
    await postAcgnProgressSave('comic', id, { ...comicAll[k] }).catch(() => {})
  }
  const novelAll = readLsJson<Record<string, { chapterno: number }>>(LEGACY_NOVEL, {})
  for (const k of Object.keys(novelAll)) {
    const id = Number(k)
    if (!Number.isFinite(id)) continue
    await postAcgnProgressSave('novel', id, novelAll[k]).catch(() => {})
  }
  const animAll = readLsJson<Record<string, number>>(LEGACY_ANIM, {})
  for (const k of Object.keys(animAll)) {
    const id = Number(k)
    if (!Number.isFinite(id)) continue
    const episode = animAll[k]
    if (typeof episode === 'number' && episode > 0) {
      await postAcgnProgressSave('animation', id, { episode }).catch(() => {})
    }
  }
}

async function maybeMigrateLegacyUserDataOnce() {
  if (typeof sessionStorage === 'undefined') return
  if (sessionStorage.getItem(SESS_LEGACY_MIGRATE) === '1') return

  const rows = collectLegacyMergedHistory()
  const hasLegacyRows = rows.length > 0
  const hasLegacyProgress =
    typeof localStorage !== 'undefined' &&
    !!(localStorage.getItem(LEGACY_COMIC) || localStorage.getItem(LEGACY_NOVEL) || localStorage.getItem(LEGACY_ANIM))
  const hasLegacy = hasLegacyRows || hasLegacyProgress

  const serverHadLibraryOrSubs =
    historyMem.length > 0 || subsMem.length > 0

  if (!hasLegacy) {
    sessionStorage.setItem(SESS_LEGACY_MIGRATE, '1')
    purgeLegacyLsKeysQuiet()
    return
  }

  let libraryMigrateOk = true
  if (!serverHadLibraryOrSubs && hasLegacyRows) {
    const res = await postAcgnImportLegacyRows(
      rows.map((r): AcgnLibraryItemDto => {
        return {
          type: r.type,
          id: r.id,
          name: r.name,
          pic: r.pic,
          at: typeof r.at === 'number' ? r.at : Date.now(),
          starred: !!r.starred,
          starredAt:
            r.starredAt != null && typeof r.starredAt === 'number' ? r.starredAt : undefined,
        }
      }),
    ).catch(() => ({ code: 0 }))
    libraryMigrateOk = (res as { code?: number })?.code === 200
    if (!libraryMigrateOk) return
  } else if (hasLegacyRows && serverHadLibraryOrSubs) {
    /* 服务端已有足迹则不再覆盖导入，仅在下方同步进度 */
  }

  await postLegacyProgress()

  try {
    const res = await fetchAcgnUserState()
    if (res?.code === 200 && res.data) {
      applyServerState(res.data)
    }
  } catch {
    //
  }

  sessionStorage.setItem(SESS_LEGACY_MIGRATE, '1')
  purgeLegacyLsKeysQuiet()
}

function purgeLegacyLsKeysQuiet() {
  if (typeof localStorage === 'undefined') return
  const keys = [
    LEGACY_BOOK,
    LEGACY_HIST,
    LEGACY_MERGED,
    LEGACY_SUB,
    LEGACY_COMIC,
    LEGACY_NOVEL,
    LEGACY_ANIM,
    'acgn_engage_v1',
    'acgn_reviews_v1',
    'acgn_visitor_nick_v1',
  ]
  keys.forEach((k) => {
    try {
      localStorage.removeItem(k)
    } catch {
      //
    }
  })
}

/** 旧版占位：不再需要本地合并书架，已由服务端接管 */
export function migrateBookshelfIntoHistoryOnce(): void {}

/** HTTP 匿名设备 ID；仅存此键于 localStorage */
export function getOrCreateAcgnClientId(): string {
  let v = localStorage.getItem(K_ANON_CLIENT)?.trim()
  if (v && v.length >= 8) return v.length > 64 ? v.slice(0, 64) : v
  const next =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID().replace(/-/g, '').slice(0, 22)
      : `d_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`
  localStorage.setItem(K_ANON_CLIENT, next)
  return next
}

export function listBookshelf(): BookshelfItem[] {
  return listHistory()
    .filter((x) => x.starred)
    .map((x) => ({
      type: x.type,
      id: x.id,
      name: x.name,
      pic: x.pic,
      addedAt: x.starredAt ?? x.at,
    }))
}

export function isInBookshelf(type: AcgnMediaType, id: number): boolean {
  const row = historyMem.find((x) => x.type === type && x.id === id)
  return !!row?.starred
}

function spliceHistoryVisited(item: Omit<HistoryItem, 'at' | 'starred' | 'starredAt'>): HistoryItem[] {
  const now = Date.now()
  let list = [...historyMem]
  const i = list.findIndex((x) => x.type === item.type && x.id === item.id)
  if (i >= 0) {
    const prev = list[i]
    list.splice(i, 1)
    list.unshift({
      ...prev,
      name: item.name ?? prev.name,
      pic: item.pic ?? prev.pic,
      at: now,
      starred: prev.starred,
      starredAt: prev.starredAt,
    })
  } else {
    list.unshift({
      ...item,
      at: now,
      starred: false,
    })
  }
  historyMem = list.slice(0, 200)
  return list
}

export function pushHistory(item: Omit<HistoryItem, 'at' | 'starred' | 'starredAt'>) {
  void innerPushHistory(item)
}

async function innerPushHistory(item: Omit<HistoryItem, 'at' | 'starred' | 'starredAt'>) {
  await ensureAcgnUserHydrated()
  const before = [...historyMem]
  spliceHistoryVisited(item)
  try {
    const res = await postAcgnLibraryVisit({
      mediaType: item.type,
      mediaId: item.id,
      name: item.name,
      pic: item.pic,
    })
    if (res?.code !== 200) {
      historyMem = before
      ElMessage.warning(res?.msg || '同步浏览记录失败')
    }
  } catch {
    historyMem = before
  }
}

export function toggleBookshelf(item: Omit<BookshelfItem, 'addedAt'>): boolean {
  const i = historyMem.findIndex((x) => x.type === item.type && x.id === item.id)
  const nextStarred = i < 0 ? true : !historyMem[i].starred
  void innerToggleBookshelf(item)
  return nextStarred
}

async function innerToggleBookshelf(item: Omit<BookshelfItem, 'addedAt'>) {
  await ensureAcgnUserHydrated()
  const before = [...historyMem]
  const now = Date.now()
  let list = [...historyMem]
  const i = list.findIndex((x) => x.type === item.type && x.id === item.id)
  let nextStarred: boolean

  if (i >= 0) {
    const row = list[i]
    nextStarred = !row.starred
    row.starred = nextStarred
    row.starredAt = nextStarred ? now : undefined
    row.name = item.name ?? row.name
    row.pic = item.pic ?? row.pic
    row.at = now
    list.splice(i, 1)
    list.unshift(row)
  } else {
    nextStarred = true
    list.unshift({
      ...item,
      at: now,
      starred: true,
      starredAt: now,
    })
  }
  historyMem = list.slice(0, 200)

  try {
    const res = await postAcgnFavoriteToggle({
      mediaType: item.type,
      mediaId: item.id,
      name: item.name,
      pic: item.pic,
    })
    if (res?.code !== 200) {
      historyMem = before
      ElMessage.warning(res?.msg || '同步收藏失败')
      return
    }
    const srv = res.data?.starred
    if (typeof srv === 'boolean' && srv !== nextStarred) {
      innerApplyStarFromServer(item.type, item.id, srv, now)
    }
  } catch {
    historyMem = before
    ElMessage.error('网络异常，收藏未保存')
  }
}

function innerApplyStarFromServer(t: AcgnMediaType, id: number, starred: boolean, now: number) {
  let list = [...historyMem]
  const i = list.findIndex((x) => x.type === t && x.id === id)
  if (i < 0) return
  const row = { ...list[i] }
  row.starred = starred
  row.starredAt = starred ? now : undefined
  list[i] = row
  historyMem = list
}

export function removeFromBookshelf(type: AcgnMediaType, id: number) {
  const row = historyMem.find((x) => x.type === type && x.id === id)
  if (!row) return
  void innerToggleBookshelf({
    type,
    id,
    name: row.name,
    pic: row.pic,
  })
}

export function listHistory(): HistoryItem[] {
  return [...historyMem]
}

export interface ComicProgress {
  chapterId: string
  chapterno: number
  imagePage?: number
}

export function saveComicProgress(comicId: number, p: ComicProgress) {
  void innerSaveComicProgress(comicId, p)
}

async function innerSaveComicProgress(comicId: number, p: ComicProgress) {
  await ensureAcgnUserHydrated()
  const before = { ...comicProgMem[String(comicId)] }
  comicProgMem[String(comicId)] = { ...p, imagePage: p.imagePage ?? 1 }
  try {
    const res = await postAcgnProgressSave(
      'comic',
      comicId,
      comicProgMem[String(comicId)] as unknown as Record<string, unknown>,
    )
    if (res?.code !== 200) {
      if (before && Object.keys(before).length) comicProgMem[String(comicId)] = before
      else delete comicProgMem[String(comicId)]
    }
  } catch {
    if (before && Object.keys(before).length) comicProgMem[String(comicId)] = before
    else delete comicProgMem[String(comicId)]
  }
}

export function getComicProgress(comicId: number): ComicProgress | null {
  return comicProgMem[String(comicId)] ?? null
}

export function saveNovelProgress(novelId: number, chapterno: number) {
  void innerSaveNovelProg(novelId, chapterno)
}

async function innerSaveNovelProg(novelId: number, chapterno: number) {
  await ensureAcgnUserHydrated()
  const k = String(novelId)
  const before = novelProgMem[k]
  novelProgMem[k] = { chapterno }
  try {
    const res = await postAcgnProgressSave('novel', novelId, { chapterno })
    if (res?.code !== 200) {
      if (before) novelProgMem[k] = before
      else delete novelProgMem[k]
    }
  } catch {
    if (before) novelProgMem[k] = before
    else delete novelProgMem[k]
  }
}

export function getNovelProgress(novelId: number): { chapterno: number } | null {
  return novelProgMem[String(novelId)] ?? null
}

export function saveAnimationEpisode(animationId: number, episode: number) {
  void innerSaveAnimEp(animationId, episode)
}

async function innerSaveAnimEp(animationId: number, episode: number) {
  await ensureAcgnUserHydrated()
  const k = String(animationId)
  const prev = animEpMem[k]
  animEpMem[k] = episode
  try {
    const res = await postAcgnProgressSave('animation', animationId, { episode })
    if (res?.code !== 200) {
      if (typeof prev === 'number') animEpMem[k] = prev
      else delete animEpMem[k]
    }
  } catch {
    if (typeof prev === 'number') animEpMem[k] = prev
    else delete animEpMem[k]
  }
}

export function getAnimationEpisode(animationId: number): number | null {
  const v = animEpMem[String(animationId)]
  return typeof v === 'number' ? v : null
}

// --- 订阅 ---
export interface SubscribeEntry {
  type: AcgnMediaType
  id: number
  name: string
  pic?: string
  subscribedAt: number
}

export function listSubscriptions(): SubscribeEntry[] {
  return [...subsMem]
}

export function isSubscribed(type: AcgnMediaType, id: number): boolean {
  return subsMem.some((x) => x.type === type && x.id === id)
}

export async function toggleSubscribeAsync(
  item: Omit<SubscribeEntry, 'subscribedAt'>,
): Promise<boolean> {
  await ensureAcgnUserHydrated()
  const before = [...subsMem]
  const now = Date.now()
  try {
    const res = await postAcgnSubscribeToggle({
      mediaType: item.type,
      mediaId: item.id,
      name: item.name,
      pic: item.pic,
    })
    if (res?.code !== 200 || res.data?.subscribed == null) {
      subsMem = before
      ElMessage.warning(res?.msg || '同步订阅失败')
      return !!before.find((s) => s.type === item.type && s.id === item.id)
    }
    const on = res.data.subscribed
    if (on) {
      subsMem = [{ ...item, subscribedAt: now }, ...subsMem.filter((s) => !(s.type === item.type && s.id === item.id))].slice(
        0,
        320,
      )
    } else {
      subsMem = subsMem.filter((s) => !(s.type === item.type && s.id === item.id))
    }
    return on
  } catch {
    subsMem = before
    return !!before.find((s) => s.type === item.type && s.id === item.id)
  }
}

/** @deprecated 仅兼容旧调用；改为 await toggleSubscribeAsync */
export function toggleSubscribe(item: Omit<SubscribeEntry, 'subscribedAt'>): boolean {
  void toggleSubscribeAsync(item)
  const had = subsMem.some((x) => x.type === item.type && x.id === item.id)
  return !had
}

// --- 互动（离线仅内存占位，不写 localStorage）---
const engageMem: Record<string, EngageState> = {}

interface EngageState {
  likes: number
  dislikes: number
  myVote: null | 'up' | 'down'
}

interface ReviewEntry {
  nick: string
  avatar?: string
  text: string
  at: number
}

const reviewsMem: Record<string, ReviewEntry[]> = {}

function mediaKey(type: AcgnMediaType, id: number) {
  return `${type}_${id}`
}

function seedCount(id: number, salt: number) {
  return 120 + Math.abs(((id ^ salt) * 1103515245 + 12345) >>> 0) % 2000
}

function readEngage(type: AcgnMediaType, id: number): EngageState {
  const k = mediaKey(type, id)
  if (!engageMem[k]) {
    engageMem[k] = { likes: seedCount(id, 1), dislikes: 6 + (id % 180), myVote: null }
  }
  return engageMem[k]
}

export function getEngagement(type: AcgnMediaType, id: number): EngageState {
  return readEngage(type, id)
}

export function setVote(type: AcgnMediaType, id: number, vote: 'up' | 'down'): EngageState {
  const cur = readEngage(type, id)
  if (vote === 'up') {
    if (cur.myVote === 'up') {
      cur.likes = Math.max(0, cur.likes - 1)
      cur.myVote = null
    } else if (cur.myVote === 'down') {
      cur.dislikes = Math.max(0, cur.dislikes - 1)
      cur.likes += 1
      cur.myVote = 'up'
    } else {
      cur.likes += 1
      cur.myVote = 'up'
    }
  } else if (vote === 'down') {
    if (cur.myVote === 'down') {
      cur.dislikes = Math.max(0, cur.dislikes - 1)
      cur.myVote = null
    } else if (cur.myVote === 'up') {
      cur.likes = Math.max(0, cur.likes - 1)
      cur.dislikes += 1
      cur.myVote = 'down'
    } else {
      cur.dislikes += 1
      cur.myVote = 'down'
    }
  }
  engageMem[mediaKey(type, id)] = cur
  return cur
}

export function listReviews(type: AcgnMediaType, id: number): ReviewEntry[] {
  const all = reviewsMem
  return [...(all[mediaKey(type, id)] ?? [])].sort((a, b) => b.at - a.at)
}

let sessionReviewerNick = ''

export function addReview(
  type: AcgnMediaType,
  id: number,
  textRaw: string,
  nickHint?: string,
  workName?: string,
  avatarHint?: string,
): boolean {
  const text = textRaw.trim()
  if (!text) return false
  if (text.length > 480) return false
  let nick =
    nickHint?.trim() ||
    (sessionReviewerNick ||
      (sessionReviewerNick = `访客_${String(Math.random()).slice(2, 6)}`))
  const k = mediaKey(type, id)
  const list = [...(reviewsMem[k] ?? [])]
  const at = Date.now()
  const avatar = avatarHint?.trim() || ''
  list.unshift({ nick, avatar, text, at })
  reviewsMem[k] = list.slice(0, 80)
  void import('@/utils/acgnPersonalFeed')
    .then(({ appendMyPublishedComment }) => {
      appendMyPublishedComment({
        type,
        mediaId: id,
        workName: (workName && workName.trim()) || `作品 #${id}`,
        text,
        channel: 'local',
        at,
      })
    })
    .catch(() => {})
  return true
}

/** 弹幕仍为演示用本地环形缓冲（未入库） */
export interface DanmuItem {
  text: string
  color: string
  at: number
}

export function listDanmu(streamKey: string): DanmuItem[] {
  try {
    const raw = localStorage.getItem(`acgn_dm_${streamKey}_v1`)
    if (!raw) return []
    const arr = JSON.parse(raw) as DanmuItem[]
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

export function pushDanmu(streamKey: string, textRaw: string): boolean {
  const text = textRaw.trim().slice(0, 72)
  if (!text) return false
  const colors = ['#fce8ea', '#a6d8ff', '#c8f7c8', '#fff3bf', '#e6d9ff']
  const prev = listDanmu(streamKey)
  const next = [...prev, { text, color: colors[prev.length % colors.length], at: Date.now() }]
  localStorage.setItem(`acgn_dm_${streamKey}_v1`, JSON.stringify(next.slice(-200)))
  return true
}
