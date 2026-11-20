export function newsCoverUrl(
  images: { role: string; url: string }[] | undefined,
  thumbnail = false,
): string {
  const url = images?.find(image => image.role === 'cover')?.url || ''
  if (!/^\/api\/news\/image\?id=[a-f0-9]{24}&role=cover$/.test(url)) return ''
  return thumbnail ? `${url}&variant=thumb` : url
}
