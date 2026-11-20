import { optimizeCoverUrl } from '@/utils/imageOptimizer'

const thumbnails = import.meta.glob<string>('/src/assets/galgame-covers/*.webp', {
  eager: true,
  query: '?url',
  import: 'default',
})

export function galgameThumbnail(src: string): string {
  const filename = /^\/galgame\/([a-z0-9-]+)\.jpg$/i.exec(src)?.[1]
  return thumbnails[`/src/assets/galgame-covers/${filename}.webp`] || optimizeCoverUrl(src)
}
