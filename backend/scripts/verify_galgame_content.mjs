import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { execFileSync } from 'node:child_process'

const root = path.resolve(process.argv[2])
const readJSON = name => JSON.parse(fs.readFileSync(path.join(root, name), 'utf8'))
const guides = readJSON('frontend/src/data/galgameGuides.json')
const seeds = readJSON('backend/data/galgame_seed.json')
const catalog = readJSON('backend/scripts/galgame_catalog.json').items
assert.equal(guides.length, 20)
assert.equal(new Set(guides.map(g => g.slug)).size, 20)
assert.deepEqual(new Set(guides.map(g => g.slug)), new Set(seeds.map(g => g.slug)))
assert.deepEqual(new Set(guides.map(g => g.slug)), new Set(catalog.map(g => g.slug)))
for (const guide of guides) {
  assert.equal(guide.story.length, 2, guide.slug)
  assert.equal(guide.highlights.length, 3, guide.slug)
  for (const key of ['tagline', 'playStyle', 'fitFor', 'versionNote', 'readingNote']) assert.ok(guide[key]?.trim(), `${guide.slug}: ${key}`)
  assert.ok(guide.sources.some(s => s.status === 'verified'), `${guide.slug}: missing verified source`)
  for (const source of guide.sources) {
    assert.equal(new URL(source.url).protocol, 'https:')
    assert.ok(['verified', 'unavailable'].includes(source.status))
    assert.ok(source.checkedAt)
    if (source.status === 'verified') assert.ok(source.supports.length)
  }
}
assert.equal(guides.find(g => g.slug === 'white-album-2').ratingVerified, false)
assert.equal(catalog.find(g => g.slug === 'atri').developer, 'Frontwing / 枕')
assert.deepEqual(seeds.map(s => s.sort_order).sort((a,b) => a-b), Array.from({length:20}, (_,i) => i+1))
assert.ok(new Set(catalog.slice(0,6).map(g => g.developer)).size >= 4)

const require = createRequire(path.join(root, 'frontend/package.json'))
const { parse, compileScript, compileTemplate, compileStyle } = require('@vue/compiler-sfc')
for (const file of ['frontend/src/view/GalaGame/index.vue', 'frontend/src/view/GalaGame/GalgameIntroduce/index.vue', 'frontend/src/view/GalaGame/GalgameIntroduce/GalgameExtras.vue']) {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const { descriptor, errors } = parse(source, { filename:file })
  assert.deepEqual(errors, [], file)
  const script = compileScript(descriptor, { id:'galgame-check' })
  const result = compileTemplate({ source:descriptor.template.content, filename:file, id:'galgame-check', compilerOptions:{bindingMetadata:script.bindings} })
  assert.deepEqual(result.errors, [], file)
  for (const style of descriptor.styles) {
    const css = style.src ? fs.readFileSync(path.resolve(root, path.dirname(file), style.src), 'utf8') : style.content
    assert.deepEqual(compileStyle({source:css, filename:file, id:'data-v-galgame-check', scoped:true}).errors, [], file)
  }
}
const media = readJSON('frontend/src/data/galgameMedia.json')
assert.equal(media.length, 20)
assert.deepEqual(new Set(media.map(m=>m.slug)), new Set(seeds.map(m=>m.slug)))
let characterCount = 0, screenshotCount = 0
const imageExists = src => {
  assert.ok(src.startsWith('/galgame/media/'))
  const file = path.join(root, 'frontend/public', src)
  assert.ok(fs.existsSync(file) && fs.statSync(file).size > 100, src)
}
for (const item of media) {
  assert.ok(item.characters.length >= 2, item.slug)
  assert.ok(item.checkedAt)
  assert.ok(!item.collectionError)
  for (const character of item.characters) {
    assert.ok(character.name && character.summary && character.summary.length < 120)
    assert.equal(new URL(character.sourceUrl).protocol, 'https:')
    assert.ok(item.sources.some(s=>s.url===character.sourceUrl), character.name)
    if (character.image) { imageExists(character.image); assert.equal(new URL(character.imageOriginalUrl).protocol,'https:') }
    characterCount++
  }
  if (item.slug === 'white-album-2') {
    assert.equal(item.screenshots.length, 0)
    assert.ok(item.officialGalleryUrl)
  } else assert.equal(item.screenshots.length, 4, item.slug)
  for (const shot of item.screenshots) {
    imageExists(shot.src)
    assert.ok(shot.alt)
    assert.equal(new URL(shot.originalUrl).protocol, 'https:')
    assert.equal(new URL(shot.sourceUrl).hostname,'store.steampowered.com')
    screenshotCount++
  }
  for (const offer of item.offers) {
    assert.equal(new URL(offer.url).hostname, 'store.steampowered.com')
    assert.ok(offer.label && offer.priceLabel)
    if (offer.priceLabel.startsWith('¥')) assert.equal(offer.currency,'CNY')
  }
}
console.log(`PASS: ${characterCount} character introductions, ${screenshotCount} local official screenshots, provenance, price currency, gallery fallback, extra component compilation.`)
console.log(`PASS: 20 unique guides, coverage, source provenance, version-sensitive rating guard, curated ordering, Vue scripts/templates/scoped styles.`)
console.log(`Verified references: ${guides.flatMap(g=>g.sources).filter(s=>s.status==='verified').length}; unavailable: ${guides.flatMap(g=>g.sources).filter(s=>s.status!=='verified').length}.`)
if (process.argv.includes('--asset-paths')) {
  const tracked = new Set(execFileSync('git', ['ls-tree','-r','--name-only','HEAD'], {cwd:root,encoding:'utf8'}).split('\n'))
  const required = new Set()
  const walk = dir => {
    for (const item of fs.readdirSync(dir, {withFileTypes:true})) {
      const file = path.join(dir,item.name)
      if (item.isDirectory()) walk(file)
      else if (item.name.endsWith('.vue')) {
        const text = fs.readFileSync(file,'utf8')
        for (const match of text.matchAll(/\bsrc\s*=\s*["'](\/[^"']+)["']/g)) {
          const publicFile = 'frontend/public' + match[1]
          if (tracked.has(publicFile) && !fs.existsSync(path.join(root, publicFile))) required.add(publicFile)
        }
      }
    }
  }
  walk(path.join(root,'frontend/src'))
  fs.writeFileSync(path.resolve(root,'../../deliverables/galgame-required-assets.txt'), [...required].join('\n')+'\n')
  console.log(`Missing static public imports for full-site build: ${required.size}`)
}
