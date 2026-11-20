import { afterEach, describe, expect, it } from 'vitest'
import { CDN_BASE, optimizeCoverUrl, optimizeImageUrl, rewriteImgElement, toCdn } from '../imageOptimizer'

const OSS = 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com'

afterEach(() => {
  window.location.hash = ''
})

describe('图片走 CDN', () => {
  it('OSS 直链换成 CDN 域名，路径和缩图参数保留', () => {
    expect(toCdn(`${OSS}/comic/1.jpg`)).toBe(`${CDN_BASE}/comic/1.jpg`)
    expect(toCdn(`${OSS}/comic/1.jpg?x-oss-process=image/resize,w_700`)).toBe(
      `${CDN_BASE}/comic/1.jpg?x-oss-process=image/resize,w_700`,
    )
  })

  it('给没有参数的 OSS 图片补缩图参数', () => {
    const url = optimizeImageUrl(`${OSS}/comic/cover/7.png`)
    expect(url.startsWith(`${CDN_BASE}/comic/cover/7.png?x-oss-process=image/resize,m_lfit,w_`)).toBe(true)
  })

  it('列表封面在设置 src 前已是 WebP 缩略图，动画图保留格式', () => {
    expect(optimizeCoverUrl(`${OSS}/comic/cover/7.png`)).toContain('w_360/quality,q_82/format,webp')
    expect(optimizeCoverUrl(`${OSS}/comic/cover/7.gif`)).not.toContain('format,webp')
  })

  it('已有 x-oss-process 时只换域名，不重复追加', () => {
    const src = `${OSS}/comic/chapter/0000001/001.jpg?x-oss-process=image/resize,m_lfit,w_1600`
    const url = optimizeImageUrl(src)
    expect(url).toBe(`${CDN_BASE}/comic/chapter/0000001/001.jpg?x-oss-process=image/resize,m_lfit,w_1600`)
    expect(url.match(/x-oss-process=/g)).toHaveLength(1)
  })

  it('阅读页取大图宽度', () => {
    window.location.hash = '#/Watch?animationid=1'
    expect(optimizeImageUrl(`${OSS}/a.jpg`)).toContain('w_1600')
    window.location.hash = '#/Comicsee?chapterid=0000001&chapterno=1'
    expect(optimizeImageUrl(`${OSS}/cartoons/cbwn/001/001.jpg`)).toContain('w_1600')
    expect(optimizeImageUrl(`${OSS}/cartoons/cbwn/001/001.jpg`)).not.toContain('format,webp')
  })

  it('签名链接、外链、非图片原样返回', () => {
    const signed = `${OSS}/private/a.jpg?OSSAccessKeyId=k&Expires=1&Signature=s`
    expect(optimizeImageUrl(signed)).toBe(signed)
    const anitabi = 'https://image.anitabi.cn/points/x.jpg?plan=h360'
    expect(optimizeImageUrl(anitabi)).toBe(anitabi)
    const epub = `${OSS}/book/slzs1.epub`
    expect(optimizeImageUrl(epub)).toBe(epub)
  })

  it('已经是 CDN 的地址不变', () => {
    const cdn = `${CDN_BASE}/comic/1.jpg?x-oss-process=image/resize,w_700`
    expect(toCdn(cdn)).toBe(cdn)
    expect(optimizeImageUrl(cdn)).toBe(cdn)
  })

  it('<img> 换域名并记住原始地址；切章节换新地址时不会回退到旧图', () => {
    const img = document.createElement('img')
    img.src = `${OSS}/cartoons/cbwn/001/001.jpg`
    rewriteImgElement(img)
    expect(img.getAttribute('src')?.startsWith(`${CDN_BASE}/cartoons/cbwn/001/001.jpg?x-oss-process=`)).toBe(true)
    expect(img.getAttribute('data-raw-src')).toBe(`${OSS}/cartoons/cbwn/001/001.jpg`)

    // 重复处理不会叠加参数
    const once = img.getAttribute('src')
    rewriteImgElement(img)
    expect(img.getAttribute('src')).toBe(once)

    // Vue 换成第二章的图
    img.src = `${OSS}/cartoons/cbwn/002/001.jpg`
    rewriteImgElement(img)
    expect(img.getAttribute('src')?.startsWith(`${CDN_BASE}/cartoons/cbwn/002/001.jpg?`)).toBe(true)
    expect(img.getAttribute('data-raw-src')).toBe(`${OSS}/cartoons/cbwn/002/001.jpg`)
  })
})
