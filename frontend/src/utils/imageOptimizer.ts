/**
 * 给阿里云 OSS / 图片 CDN 上的图片自动追加缩放参数，避免列表页直接下载原图。
 * 只作用于 <img>，不会碰视频（PV 预览）等其他资源。
 */
const IMAGE_HOSTS = ['acgn0721.oss-cn-hangzhou.aliyuncs.com', 'img.0721acgn.com'];
const MARK = 'x-oss-process=';

/** 阅读/观看页需要大图，其余页面用缩略图 */
function targetWidth(): number {
  const h = (typeof location !== 'undefined' ? location.hash : '') || '';
  const bigPage = /(Watch|Read|read|watch|detail|Detail)/.test(h);
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

function withProcess(src: string): string {
  if (!isOwnImage(src) || src.includes(MARK)) return src;
  // 只处理图片扩展名，避免给 mp4/epub 等加图片处理参数
  if (!/\.(jpe?g|png|webp|gif|bmp)$/i.test(src.split('?')[0])) return src;
  // 签名链接加任何参数都会失效
  if (/[?&](OSSAccessKeyId|Signature|security-token|X-Oss-Signature)=/i.test(src)) return src;
  const sep = src.includes('?') ? '&' : '?';
  const w = targetWidth();
  return `${src}${sep}${MARK}image/resize,m_lfit,w_${w}/quality,q_82`;
}

function rewriteImg(img: HTMLImageElement) {
  const raw = img.getAttribute('data-raw-src') || img.getAttribute('src') || '';
  const next = withProcess(raw);
  if (next !== raw && img.getAttribute('src') !== next) {
    img.setAttribute('data-raw-src', raw);
    img.setAttribute('src', next);
  }
}

function scan(root: ParentNode) {
  root.querySelectorAll?.('img').forEach((el) => rewriteImg(el as HTMLImageElement));
}

export function installImageOptimizer() {
  if (typeof window === 'undefined' || typeof MutationObserver === 'undefined') return;
  const start = () => {
    scan(document);
    const mo = new MutationObserver((records) => {
      for (const r of records) {
        if (r.type === 'attributes' && r.target instanceof HTMLImageElement) rewriteImg(r.target);
        r.addedNodes.forEach((n) => {
          if (n instanceof HTMLImageElement) rewriteImg(n);
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
