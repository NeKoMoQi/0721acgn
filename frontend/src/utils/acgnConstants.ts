/** 列表筛选：题材（需与库中 genre 字段一致，可先 UPDATE 几条演示数据） */
export const ACGN_GENRES = ['奇幻', '恋爱', '战斗', '日常', '科幻', '悬疑', '治愈', '校园']

/** Galgame 题材/标签，与 tb_galgame.genres 中的逗号分隔值一致 */
export const GALGAME_GENRES = ['恋爱', '校园', '剧情', '治愈', '奇幻', '悬疑', '科幻', '视觉小说', '冒险', '日常']

export const GALGAME_SORT_OPTIONS = [
  { label: '默认', value: 'default' },
  { label: '评分最高', value: 'rating' },
  { label: '发行时间', value: 'release' },
]


export function decadeRange(decade: string): { yearMin?: number; yearMax?: number } {
  const map: Record<string, [number, number]> = {
    '2020s': [2020, 2029],
    '2010s': [2010, 2019],
    '2000s': [2000, 2009],
    '1990s': [1990, 1999],
    earlier: [1900, 1989],
  }
  if (!decade || !map[decade]) return {}
  const [a, b] = map[decade]
  return { yearMin: a, yearMax: b }
}
