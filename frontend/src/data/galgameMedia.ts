import data from './galgameMedia.json'
import discovery from './galgameDiscovery.json'
import type { CharacterWork } from './characterAssociations'

export interface GalgameCharacter {
  name: string
  originalName?: string
  summary: string
  image?: string
  imagePosition?: string
  imageOriginalUrl?: string
  sourceUrl: string
}
export interface GalgameMedia {
  slug: string
  checkedAt: string
  characters: GalgameCharacter[]
  screenshots: { src: string; alt: string; category: string; sourceUrl: string; originalUrl: string }[]
  galleryNote?: string
  pv?: { title: string; originalTitle: string; poster: string; watchUrl: string; originalPosterUrl: string; checkedAt: string; movieId: number; videoUrl?: string }
  offers: { label: string; platform: string; url: string; priceLabel: string; currency?: string }[]
  sources: { name: string; url: string; checkedAt: string; supports: string[] }[]
  durationLabel?: string
  officialGalleryUrl?: string
}
const mediaBySlug = new Map((data as GalgameMedia[]).map(item => [item.slug, item]))
export function mediaFor(slug?: string) {
  return slug ? mediaBySlug.get(slug) : undefined
}

export function galgameCharacter(slug: string, name: string) {
  const person = mediaFor(slug)?.characters.find(entry => entry.name === name)
  if (!person) return undefined
  return { person, title: discovery.entries.find(entry => entry.slug === slug)?.name || '' }
}

export function searchGalgameCharacters(keyword: string) {
  const query = keyword.normalize('NFKC').toLowerCase()
  return [...mediaBySlug.values()].flatMap(({ slug, characters }) =>
    characters.filter(person => [person.name, person.originalName || ''].some(name =>
      name.normalize('NFKC').toLowerCase().includes(query),
    )).map(person => ({
      slug,
      person,
      title: discovery.entries.find(entry => entry.slug === slug)?.name || '',
    })),
  )
}
const animeAdaptations: Record<string, CharacterWork[]> = {
  atri: [{ type: 'animation', id: 11791, title: '亚托莉 -我挚爱的时光-' }],
  clannad: [
    { type: 'animation', id: 10949, title: 'CLANNAD' },
    { type: 'animation', id: 11206, title: 'CLANNAD ～AFTER STORY～' },
  ],
}

export function animationWorksForGalgame(slug: string) {
  return Object.hasOwn(animeAdaptations, slug) ? animeAdaptations[slug] : []
}

export function galgameForAnimation(id: number) {
  return Object.entries(animeAdaptations).find(([, works]) => works.some(work => work.id === id))?.[0]
}
