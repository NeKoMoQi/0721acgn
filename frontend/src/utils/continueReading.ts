import type { ContinueItem } from '@/utils/acgnLocal'

export function resumeLocation(item: ContinueItem) {
  if (item.type === 'comic') {
    return { name: 'Comicsee', query: { comicid: item.id, chapterid: item.chapterId, chapterno: item.position, pageNumber: item.pageNumber } }
  }
  if (item.type === 'novel') {
    return { name: 'Novelsee', query: { novelid: item.id, chapterno: item.position } }
  }
  return { name: 'AnimationWatch', query: { animationid: item.id, episode: item.position } }
}

export function resumeLabel(item: ContinueItem) {
  if (item.type === 'animation') return `第 ${item.position} 集`
  const chapter = `第 ${item.position} ${item.type === 'novel' ? '卷' : '章'}`
  if (item.pageNumber) return `${chapter} · 第 ${item.pageNumber} 页`
  return item.type === 'comic' ? `${chapter} · 页码未记录` : chapter
}
