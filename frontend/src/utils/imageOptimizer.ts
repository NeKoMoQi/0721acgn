/**
 * 给阿里云 OSS / 图片 CDN 上的图片自动追加缩放参数，并把 OSS 直链换成图片 CDN 域名。
 * 只作用于 <img>，不会碰视频（PV 预览）、epub 等其他资源。
 *
 * 背景：数据库里存的封面、阅读页图片都是 OSS 直链，直接 <img src> 会走 OSS 公网流出。
 * 这里在渲染前把主机名换成 CDN 加速域名，路径和 x-oss-process 参数原样保留，
 * 这样阅读页大图、列表封面都走 CDN，OSS 只剩回源流量。
 */
const OSS_HOST = 'acgn0721.oss-cn-hangzhou.aliyuncs.com';
const IMAGE_HOSTS = [OSS_HOST, 'img.0721acgn.com'];
const MARK = 'x-oss-process=';

/** CDN 域名；构建时可用 VITE_IMG_CDN 覆盖（空字符串表示只加缩图参数，不换域名） */
export const CDN_BASE = ((import.meta.env?.VITE_IMG_CDN as string | undefined) ?? 'https://img.0721acgn.com')
  .trim()
  .replace(/\/+$/, '');

/** 阅读/观看页需要大图，其余页面用缩略图 */
function targetWidth(): number {
  const h = (typeof location !== 'undefined' ? location.hash : '') || '';
  // Comicsee 是漫画阅读页，用的是整页大图，不能按列表缩略图取宽
  const bigPage = /(Watch|Read|read|watch|detail|Detail|Comicsee)/.test(h);
  const dpr = typeof window !== 'undefined' ? Math.min(window.devicePixelRatio || 1, 2) : 1;
  return bigPage ? 1600 : Math.round(700 * dpr);
}

/** 只认完整主机名：OSS 原站或已配置的图片 CDN */
function isOwnImage(src: string): boolean {
  if (!src) return false;
  try {
    const url = new URL(src, typeof location !== 'undefined' ? location.href : 'https://0721acgn.com/');
    return IMAGE_HOSTS.includes(url.hostname);
  } catch {
    return false;
  }
}

/** 签名链接换域名或加参数都会失效 */
function isSigned(src: string): boolean {
  return /[?&](OSSAccessKeyId|Signature|security-token|X-Oss-Signature)=/i.test(src);
}

/** OSS 直链 → CDN 直链；已经是 CDN 或不是我们自己的图就原样返回 */
export function toCdn(src: string): string {
  if (!src || !CDN_BASE) return src;
  const m = /^https?:\/\/acgn0721\.oss-cn-hangzhou\.aliyuncs\.com(?=[/?#]|$)/i.exec(src);
  if (!m) return src;
  return CDN_BASE + src.slice(m[0].length);
}

/** 换域名 + 补缩图参数；非图片、签名链接、外链都不动 */
export function optimizeImageUrl(src: string): string {
  if (!isOwnImage(src)) return src;
  // 只处理图片扩展名，避免给 mp4/epub 等加图片处理参数
  if (!/\.(jpe?g|png|webp|gif|bmp)$/i.test(src.split(/[?#]/)[0])) return src;
  if (isSigned(src)) return src;
  const cdn = toCdn(src);
  if (cdn.includes(MARK)) return cdn;
  const sep = cdn.includes('?') ? '&' : '?';
  const w = targetWidth();
  return `${cdn}${sep}${MARK}image/resize,m_lfit,w_${w}/quality,q_82`;
}

/**
 * 处理单个 <img>：src 换成 CDN 版本，原始地址记在 data-raw-src。
 * Vue 改 src（切章节、换列表）时旧的 data-raw-src 会过期，这里以新 src 为准重算。
 */
export function rewriteImgElement(img: HTMLImageElement) {
  const current = img.getAttribute('src') || '';
  if (!current) return;
  const prevRaw = img.getAttribute('data-raw-src') || '';
  const raw = prevRaw && optimizeImageUrl(prevRaw) === current ? prevRaw : current;
  const next = optimizeImageUrl(raw);
  if (raw !== prevRaw) img.setAttribute('data-raw-src', raw);
  if (next !== current) img.setAttribute('src', next);
}

function scan(root: ParentNode) {
  root.querySelectorAll?.('img').forEach((el) => rewriteImgElement(el as HTMLImageElement));
}

export function installImageOptimizer() {
  if (typeof window === 'undefined' || typeof MutationObserver === 'undefined') return;
  const start = () => {
    scan(document);
    const mo = new MutationObserver((records) => {
      for (const r of records) {
        if (r.type === 'attributes' && r.target instanceof HTMLImageElement) rewriteImgElement(r.target);
        r.addedNodes.forEach((n) => {
          if (n instanceof HTMLImageElement) rewriteImgElement(n);
          else if (n instanceof HTMLElement) scan(n);
        });
      }
    });
    mo.observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });
    window.addEventListener('hashchange', () => scan(document));
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
}
