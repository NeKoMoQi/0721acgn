import type { SearchRow } from '@/api/AllSearch'

export function galgameCharacterLocation(slug: string, person: string) {
  return { name: 'GalgameCharacter', params: { slug, person } }
}

export function searchResultLocation(item: SearchRow) {
  if (item.sourcetype === '角色') {
    return item.galgameSlug
      ? galgameCharacterLocation(item.galgameSlug, item.name)
      : { name: 'Character', params: { id: item.id } }
  }
  if (item.sourcetype === '轻小说') return { name: 'NovelIntroduce', query: { novelid: item.id } }
  if (item.sourcetype === '漫画') return { name: 'ComicIntroduce', query: { comicid: item.id } }
  return { name: 'Animationintroduce', query: { animationid: item.id } }
}
