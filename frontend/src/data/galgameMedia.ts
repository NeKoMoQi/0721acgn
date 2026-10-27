import data from './galgameMedia.json'

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
  screenshots: { src: string; alt: string; sourceUrl: string; originalUrl: string }[]
  offers: { label: string; platform: string; url: string; priceLabel: string; currency?: string }[]
  sources: { name: string; url: string; checkedAt: string; supports: string[] }[]
  durationLabel?: string
  officialGalleryUrl?: string
}
const mediaBySlug = new Map((data as GalgameMedia[]).map(item => [item.slug, item]))
export function mediaFor(slug?: string) {
  return slug ? mediaBySlug.get(slug) : undefined
}
