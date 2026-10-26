import guideData from './galgameGuides.json'

export interface GalgameGuide {
  slug: string
  tagline: string
  story: string[]
  highlights: { title: string; text: string }[]
  playStyle: string
  fitFor: string
  versionNote: string
  readingNote: string
  ratingVerified?: boolean
  sources: {
    name: string
    url: string
    checkedAt: string
    status: string
    supports: string[]
  }[]
}

const guides: GalgameGuide[] = guideData
const guideBySlug = new Map(guides.map(guide => [guide.slug, guide]))

export function guideFor(slug?: string) {
  return slug ? guideBySlug.get(slug) : undefined
}

export function ratingAllowed(slug?: string) {
  return guideFor(slug)?.ratingVerified !== false
}
