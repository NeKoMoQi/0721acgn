// Generate WebP copies of large local images.
// The web server (C:\apps\0721acgn-server\server.js) serves the .webp sibling when the
// browser sends "Accept: image/webp", so URLs in the app do not need to change.
//
// Usage: node scripts/convert-webp.mjs
// Requires sharp: npm i -D sharp
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..', 'public');
const MIN_BYTES = 120 * 1024;
const SKIP_DIR = /[\\/](models)[\\/]/i;

async function walk(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (!SKIP_DIR.test(p + path.sep)) await walk(p, out);
      continue;
    }
    if (!/\.(png|jpe?g)$/i.test(e.name)) continue;
    const st = fs.statSync(p);
    if (st.size >= MIN_BYTES) out.push({ p, size: st.size });
  }
}

const files = [];
await walk(root, files);
let before = 0;
let after = 0;
for (const f of files) {
  const dst = f.p.replace(/\.(png|jpe?g)$/i, '.webp');
  const meta = await sharp(f.p, { failOn: 'none' }).metadata();
  const out = await sharp(f.p, { failOn: 'none' })
    .resize({ width: Math.min(meta.width || 1920, 1920), withoutEnlargement: true })
    .webp({ quality: 80, effort: 4 })
    .toFile(dst);
  before += f.size;
  after += out.size;
  console.log(`${path.relative(root, f.p)}  ${(f.size / 1024) | 0}KB -> ${(out.size / 1024) | 0}KB`);
}
console.log(`\n${files.length} images, ${(before / 1048576).toFixed(1)} MB -> ${(after / 1048576).toFixed(1)} MB`);
