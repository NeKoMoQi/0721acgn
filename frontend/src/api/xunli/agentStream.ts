export type AgentEvent = { event: string; data: any }

/** Incremental SSE parser: fetch chunks can split UTF-8 characters, JSON, or CRLF frames. */
export function createAgentSseParser(onEvent: (event: AgentEvent) => void) {
  let buffer = ''
  return (chunk: string) => {
    buffer += chunk
    if (buffer.length > 256_000) throw new Error('巡礼响应过长，请缩短行程')
    let match: RegExpExecArray | null
    while ((match = /\r?\n\r?\n/.exec(buffer))) {
      const frame = buffer.slice(0, match.index)
      buffer = buffer.slice(match.index + match[0].length)
      let event = 'message'
      const data: string[] = []
      for (const line of frame.split(/\r?\n/)) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
      }
      if (data.length) onEvent({ event, data: JSON.parse(data.join('\n')) })
    }
  }
}

export async function streamAgentChat(
  request: { query: string; mode: string; conversationId?: string | null; contextPlan?: any },
  onEvent: (event: AgentEvent) => void,
  signal: AbortSignal,
  identity: { token?: string; clientId?: string } = {},
) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
  if (identity.token) headers.token = identity.token
  if (identity.clientId) headers['X-Acgn-Client'] = identity.clientId
  const response = await fetch('/acgn/live2d/agent/stream', {
    method: 'POST',
    headers,
    credentials: 'same-origin',
    body: JSON.stringify(request),
    signal,
  })
  if (!response.ok) throw new Error(`巡礼服务请求失败（HTTP ${response.status}）`)
  if (!response.headers.get('content-type')?.includes('text/event-stream') || !response.body)
    throw new Error('巡礼流式接口未就绪，请检查服务器路由配置')
  let result: any = null
  const parse = createAgentSseParser(({ event, data }) => {
    if (event === 'error') throw new Error(data?.message || '巡礼规划失败')
    if (event === 'done') result = data
    onEvent({ event, data })
  })
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) { parse(decoder.decode()); break }
      parse(decoder.decode(value, { stream: true }))
      if (result) break
    }
    if (!result) throw new Error('连接在规划完成前中断，请重试')
    return result
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}
