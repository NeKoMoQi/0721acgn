import type { AcgnMediaType } from '@/utils/acgnLocal'

export function resourceLabel(type: AcgnMediaType, end?: number | null) {
  if (end == null) return '资源状态待确认'
  if (end <= 0) return '仅资料'
  const unit = type === 'animation' ? '集' : type === 'novel' ? '卷' : '章'
  return `${type === 'animation' ? '可播放' : '可阅读'} · 已收录至第 ${end} ${unit}`
}
