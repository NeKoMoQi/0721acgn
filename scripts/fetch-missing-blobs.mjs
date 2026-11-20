#!/usr/bin/env node
/**
 * GitHub 拉取很慢/卡住时的兜底：用 jsDelivr 把缺失的 git 对象补齐。
 *
 * 背景：这台服务器到 GitHub 的带宽经常只有几十 KB/s（大文件包会让 git fetch
 * 卡在 unpack-objects 十几分钟），而 jsDelivr 的 gh 镜像能跑到 300KB/s 左右。
 *
 * 用法：
 *   git fetch --filter=blob:none origin main     # 先只拿 commit/tree（几秒）
 *   node scripts/fetch-missing-blobs.mjs         # 再从 jsDelivr 补 blob
 *   git merge --ff-only origin/main              # 对象齐了，纯本地操作
 *
 * 说明：只处理 origin/<ref> 与 HEAD 之间新增/修改的文件；已存在的对象会跳过。
 */
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const REPO = path.resolve(import.meta.dirname, '..')
const REF = process.env.REF || 'origin/main'
const CONCURRENCY = Number(process.env.CONCURRENCY || 8)

const git = (...args) => execFileSync('git', ['-C', REPO, ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })

const remoteUrl = git('remote', 'get-url', 'origin').trim()
const match = /github\.com[:/]+([^/]+)\/([^/.]+)(?:\.git)?/.exec(remoteUrl)
if (!match) {
  console.error('无法从 origin 地址解析 GitHub owner/repo:', remoteUrl)
  process.exit(1)
}
const [, owner, repo] = match
const sha = git('rev-parse', REF).trim()
console.log(`repo=${owner}/${repo} ref=${REF} sha=${sha.slice(0, 8)}`)

const tree = new Map()
for (const line of git('ls-tree', '-r', REF).split('\n')) {
  const m = /^\d+ blob ([0-9a-f]{40})\t(.+)$/.exec(line.trim())
  if (m) tree.set(m[2], m[1])
}

const changed = git('diff', '--name-only', 'HEAD', REF).split('\n').map(s => s.trim()).filter(Boolean)
console.log('待检查文件:', changed.length)

const queue = [...changed]
const failures = []
let written = 0
let skipped = 0

const has = sha => {
  try { git('cat-file', '-e', sha); return true } catch { return false }
}

async function worker() {
  while (queue.length) {
    const rel = queue.shift()
    const expected = tree.get(rel)
    if (!expected) { failures.push(`${rel}: 不在 ${REF} 的树里`); continue }
    if (has(expected)) { skipped++; continue }
    const url = `https://cdn.jsdelivr.net/gh/${owner}/${repo}@${sha}/${rel.split('/').map(encodeURIComponent).join('/')}`
    try {
      const res = await fetch(url, { headers: { 'User-Agent': 'dsh-deploy' } })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const buf = Buffer.from(await res.arrayBuffer())
      const tmp = path.join(REPO, '.git', `blob-${expected}.tmp`)
      fs.writeFileSync(tmp, buf)
      const actual = git('hash-object', '-w', tmp).trim()
      fs.unlinkSync(tmp)
      if (actual !== expected) throw new Error(`sha 不匹配: ${actual}`)
      written++
    } catch (e) {
      failures.push(`${rel}: ${e.message}`)
    }
  }
}

await Promise.all(Array.from({ length: CONCURRENCY }, worker))
console.log(`补齐 ${written} 个，跳过 ${skipped} 个，失败 ${failures.length} 个`)
if (failures.length) {
  console.error(failures.slice(0, 20).join('\n'))
  process.exit(1)
}
