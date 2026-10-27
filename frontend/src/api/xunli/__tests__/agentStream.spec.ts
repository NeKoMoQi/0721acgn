import { afterEach, describe, expect, it, vi } from 'vitest'
import { createAgentSseParser, streamAgentChat } from '../agentStream'

afterEach(() => vi.unstubAllGlobals())

function mockResponse(text: string, byteByByte = false) {
  const bytes = new TextEncoder().encode(text)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(new ReadableStream({
    start(controller) {
      if (byteByByte) for (const byte of bytes) controller.enqueue(new Uint8Array([byte]))
      else controller.enqueue(bytes)
      controller.close()
    },
  }), { headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } })))
}

describe('巡礼 SSE 接口', () => {
  it('跨分段解析 CRLF、多行数据，并忽略心跳', () => {
    const events: any[] = []
    const parse = createAgentSseParser(event => events.push(event))
    const text = ': heartbeat\r\n\r\nevent: status\r\ndata: {\r\ndata: "text":"正在检索"}\r\n\r\n'
    for (const character of text) parse(character)
    expect(events).toEqual([{ event: 'status', data: { text: '正在检索' } }])
  })

  it('UTF-8 每字节拆包也不会损坏中文，完成后返回结构化路线', async () => {
    mockResponse('event: delta\ndata: {"text":"京都巡礼"}\n\nevent: done\ndata: {"reply":"完成","routePlan":{"version":2}}\n\n', true)
    const events: any[] = []
    const result = await streamAgentChat({ query: '京都', mode: 'route' }, e => events.push(e), new AbortController().signal)
    expect(events[0].data.text).toBe('京都巡礼')
    expect(result.routePlan.version).toBe(2)
    expect(vi.mocked(fetch).mock.calls[0][1]?.body).not.toContain('systemHint')
  })

  it('流中错误不能冒充成功', async () => {
    mockResponse('event: error\ndata: {"message":"地点不存在"}\n\n')
    await expect(streamAgentChat({ query: '京都', mode: 'route' }, () => {}, new AbortController().signal)).rejects.toThrow('地点不存在')
  })

  it('截断连接不能生成半条路线', async () => {
    mockResponse('event: delta\ndata: {"text":"半条路线"}\n\n')
    await expect(streamAgentChat({ query: '京都', mode: 'route' }, () => {}, new AbortController().signal)).rejects.toThrow('中断')
  })

  it('拒绝代理误返回 HTML', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html/>', { headers: { 'Content-Type': 'text/html' } })))
    await expect(streamAgentChat({ query: '京都', mode: 'route' }, () => {}, new AbortController().signal)).rejects.toThrow('接口未就绪')
  })

  it('中止信号传递给 fetch', async () => {
    const controller = new AbortController()
    controller.abort()
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new DOMException('aborted', 'AbortError')))
    await expect(streamAgentChat({ query: '京都', mode: 'route' }, () => {}, controller.signal)).rejects.toThrow('aborted')
    expect(vi.mocked(fetch).mock.calls[0][1]?.signal).toBe(controller.signal)
  })

  it('账号与设备身份通过请求头传递，不放入模型问题', async () => {
    mockResponse('event: done\ndata: {"reply":"完成"}\n\n')
    await streamAgentChat({ query: '京都', mode: 'route' }, () => {}, new AbortController().signal,
      { token: 'test-token', clientId: 'test-device-123' })
    const options = vi.mocked(fetch).mock.calls[0][1]!
    expect(options.headers).toMatchObject({ token: 'test-token', 'X-Acgn-Client': 'test-device-123' })
    expect(options.body).not.toContain('test-token')
  })
})
