/** 将关键词分段用于结果高亮（避免 v-html XSS） */

export function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function keywordSegments(full: string, kwRaw: string): { text: string; hl: boolean }[] {
  const kw = kwRaw.trim()
  if (!kw || !full) return [{ text: full || '', hl: false }]
  const out: { text: string; hl: boolean }[] = []
  const r = new RegExp(escapeRegExp(kw), 'gi')
  let last = 0
  let m: RegExpExecArray | null
  while ((m = r.exec(full))) {
    if (m.index > last) out.push({ text: full.slice(last, m.index), hl: false })
    out.push({ text: m[0], hl: true })
    last = m.index + m[0].length
  }
  if (last < full.length) out.push({ text: full.slice(last), hl: false })
  return out.length ? out : [{ text: full, hl: false }]
}
