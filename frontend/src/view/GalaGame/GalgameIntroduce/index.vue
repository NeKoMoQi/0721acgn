<template>
  <div class="root-container">
    <Headpage class="header" />
    <div class="background-layer"></div>

    <!-- 加载骨架 -->
    <div v-if="loading" class="content-wrapper">
      <div class="info-container skeleton-panel">
        <div class="cover-skeleton"></div>
        <div class="detail-skeleton">
          <span class="sk-line w40"></span>
          <span class="sk-line w70 tall"></span>
          <span class="sk-line w50"></span>
          <span class="sk-line"></span>
          <span class="sk-line"></span>
          <span class="sk-line w80"></span>
        </div>
      </div>
    </div>

    <!-- 请求失败 -->
    <div v-else-if="error" class="content-wrapper">
      <div class="state-card">
        <div class="state-icon">!</div>
        <h2>作品详情暂时无法加载</h2>
        <p>{{ error }}</p>
        <el-button type="primary" round @click="loadDetail">重新加载</el-button>
        <el-button round @click="backToList">返回 Galgame 列表</el-button>
      </div>
    </div>

    <!-- 正常内容 -->
    <div v-else-if="game" class="content-wrapper">
      <header class="game-hero">
        <div class="hero-art" :style="{ backgroundImage: `url(${coverOf})` }" aria-hidden="true"></div>
        <nav class="breadcrumb" aria-label="当前位置"><router-link to="/Galagame">Galgame</router-link><span> / </span><span>{{ game.developer || '作品介绍' }}</span></nav>
        <div class="hero-content">
          <div class="cover-wrapper"><img :src="coverOf" class="cover-image" :alt="`${game.nameZh} 封面`" @error="onImgError"></div>
          <div class="detail-wrapper">
            <h1 class="novel-title">{{ game.nameZh }}</h1>
            <p v-if="game.nameOriginal && game.nameOriginal !== game.nameZh" class="original-title">{{ game.nameOriginal }}</p>
            <p v-if="game.aliases" class="alias-line">别名：{{ game.aliases }}</p>
            <div v-if="game.rating != null && ratingAllowed(game.slug)" class="hero-rating">
              <strong>{{ formatRating(game.rating) }}</strong>
              <span class="rating-stars" aria-hidden="true"><span>★★★★★</span><span class="stars-fill" :style="{ width: `${Math.max(0, Math.min(100, Number(game.rating) * 10))}%` }">★★★★★</span></span>
              <a v-if="game.ratingSourceUrl" :href="game.ratingSourceUrl" target="_blank" rel="noopener noreferrer">{{ game.ratingSource || '来源评分' }}<template v-if="game.ratingCount != null"> · {{ formatCount(game.ratingCount) }} 人评分</template> ↗</a>
              <span v-else>{{ game.ratingSource || '来源评分' }} / 10</span>
            </div>
            <p v-else class="rating-pending">{{ ratingAllowed(game.slug) ? '暂无来源评分' : '评分篇章核对中' }}</p>
            <dl class="hero-facts">
              <div><dt>发行</dt><dd>{{ game.releaseDate || '暂无资料' }}</dd></div>
              <div><dt>开发</dt><dd>{{ game.developer || '暂无资料' }}</dd></div>
              <div><dt>平台</dt><dd>{{ game.platforms || '暂无资料' }}</dd></div>
              <div v-if="media?.durationLabel"><dt>时长</dt><dd>{{ media.durationLabel }}</dd></div>
            </dl>
            <p v-if="game.releaseNote" class="release-note">{{ game.releaseNote }}</p>
            <div class="tag-row"><span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span></div>
            <p v-if="guide" class="guide-tagline">{{ guide.tagline }}</p>
            <p v-else class="guide-tagline">{{ game.intro || game.shortIntro || '暂无简介。' }}</p>
            <div class="button-group">
              <a v-if="officialLink" class="main-button" :href="officialLink" target="_blank" rel="noopener noreferrer">↗ 官方 / 商店页</a>
              <a v-if="game.vndbUrl" class="secondary-button" :href="game.vndbUrl" target="_blank" rel="noopener noreferrer">VNDB 条目</a>
              <button type="button" class="secondary-button" @click="backToList">← 返回列表</button>
            </div>
          </div>
        </div>
      </header>
      <GalgameExtras :media="media" :title="game.nameZh" :official-url="officialLink" />

      <GalgameExplore :slug="game.slug" />

      <article v-if="guide" class="reading-panel">
        <nav class="reading-nav" aria-label="作品导读目录">
          <button type="button" @click="scrollToSection('story')">剧情简介</button>
          <button type="button" @click="scrollToSection('features')">作品看点</button>
          <button type="button" @click="scrollToSection('reading')">游玩参考</button>
          <button type="button" @click="scrollToSection('versions')">版本说明</button>
        </nav>
        <section id="guide-story" class="reading-section">
          <span class="reading-eyebrow">STORY</span>
          <h2 class="section-title">剧情简介</h2>
          <p v-for="paragraph in guide.story" :key="paragraph">{{ paragraph }}</p>
        </section>
        <section id="guide-features" class="reading-section">
          <h2 class="section-title">作品看点</h2>
          <div class="feature-grid">
            <div v-for="(feature, index) in guide.highlights" :key="feature.title" class="feature-card">
              <span class="feature-number">0{{ index + 1 }}</span>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.text }}</p>
            </div>
          </div>
        </section>
        <section id="guide-reading" class="reading-section">
          <h2 class="section-title">游玩参考</h2>
          <div class="reading-pair">
            <div><h3>玩法</h3><p>{{ guide.playStyle }}</p></div>
            <div><h3>适合人群</h3><p>{{ guide.fitFor }}</p></div>
          </div>
          <p class="reading-note">{{ guide.readingNote }}</p>
        </section>
        <section id="guide-versions" class="reading-section">
          <h2 class="section-title">版本说明</h2>
          <p>{{ guide.versionNote }}</p>
        </section>
      </article>

      <details class="source-details">
          <summary>资料来源与更新时间</summary>
          <p class="source-note">简介根据以下资料整理，游玩建议为本站观点。版本与语言信息对应具体商品。</p>
          <h2 v-if="guide" class="sources-heading">介绍参考</h2>
          <ul v-if="guide" class="source-list guide-sources">
            <li v-for="source in guide.sources" :key="source.url" class="source-item">
              <a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.name }}</a>
              <span class="source-time">{{ source.checkedAt }} · {{ source.status === 'verified' ? '已核对' : '待核对' }}</span>
              <div class="source-fields">{{ source.supports.join('、') }}</div>
            </li>
          </ul>
          <h2 v-if="media" class="sources-heading">角色、画面与商店资料</h2>
          <ul v-if="media" class="source-list"><li v-for="source in media.sources" :key="source.url" class="source-item"><a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.name }}</a><span class="source-time">{{ source.checkedAt }}</span><div class="source-fields">{{ source.supports.join('、') }}</div></li></ul>
          <h2 class="sources-heading">基础资料来源</h2>
          <ul v-if="sources.length" class="source-list">
            <li v-for="(source, index) in sources" :key="'source-' + index" class="source-item">
              <a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.name }}</a>
              <span v-if="source.retrieved_at" class="source-time">{{ formatDate(source.retrieved_at) }}</span>
              <div v-if="source.fields?.length" class="source-fields">{{ source.fields.join('、') }}</div>
              <div v-if="source.license" class="source-license">{{ source.license }}</div>
            </li>
          </ul>
          <p v-else class="source-empty">暂无结构化来源记录。</p>
          <p v-if="game.sourceNote" class="source-note">{{ game.sourceNote }}</p>
          <p class="source-note">
            评分对应所链接的来源条目，支持平台可能涵盖多个历史发行版本，具体内容与语言以所选商品为准。
            Bangumi 在本次采集环境连接超时，未使用其评分或条目数据。
            封面版权归原权利人，原始资料遵循各来源许可；封面仅用于作品识别，如权利方有异议可替换为默认封面。
          </p>
      </details>

      <div class="collected-line">
        基础资料更新：{{ formatDate(game.fetchedAt) }}<template v-if="guide"> · 介绍更新：{{ guide.sources[0]?.checkedAt }}</template>
      </div>
    </div>

    <Behind class="footer" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import Behind from '@/components/behind/index.vue'
import { getGalgameById } from '@/api/galgame'
import { guideFor, ratingAllowed } from '@/data/galgameGuides'
import { mediaFor } from '@/data/galgameMedia'
import GalgameExtras from './GalgameExtras.vue'
import GalgameExplore from '../GalgameExplore.vue'
import { idForSlug } from '@/data/galgameDiscovery'

interface Galgame {
  id: number
  slug: string
  nameZh: string
  nameOriginal?: string
  aliases?: string
  shortIntro?: string
  intro?: string
  cover?: string
  developer?: string
  releaseDate?: string
  releaseYear?: number | null
  releaseNote?: string
  platforms?: string
  genres?: string
  rating?: number | string | null
  ratingCount?: number | null
  ratingSource?: string
  ratingSourceUrl?: string
  moegirlUrl?: string
  vndbUrl?: string
  officialUrl?: string
  steamUrl?: string
  sourcesJson?: string
  sourceNote?: string
  fetchedAt?: string
}

interface Source {
  name: string
  url: string
  retrieved_at?: string
  fields?: string[]
  license?: string
}

const PLACEHOLDER = '/galgame/placeholder.svg'
const route = useRoute()
const router = useRouter()
const game = ref<Galgame | null>(null)
const loading = ref(true)
const error = ref('')
let detailRequest = 0
const guide = computed(() => guideFor(game.value?.slug))
const media = computed(() => mediaFor(game.value?.slug))

function scrollToSection(section: string) {
  document.getElementById(`guide-${section}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const coverOf = computed(() => game.value?.cover || PLACEHOLDER)
const officialLink = computed(() => game.value?.officialUrl || game.value?.steamUrl || media.value?.officialGalleryUrl || '')
const tags = computed(() =>
  (game.value?.genres || '')
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean),
)
const sources = computed<Source[]>(() => {
  if (!game.value?.sourcesJson) return []
  try {
    const parsed = JSON.parse(game.value.sourcesJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  if (!img || img.src.endsWith(PLACEHOLDER)) return
  img.src = PLACEHOLDER
}

function formatCount(n: number | null | undefined) {
  if (n == null) return ''
  return Number(n).toLocaleString('zh-CN')
}

function formatRating(v: number | string | null | undefined) {
  if (v == null) return ''
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(1) : String(v)
}

function formatDate(value?: string) {
  if (!value) return '暂无'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (x: number) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function backToList() {
  router.push('/Galagame')
  window.scrollTo({ top: 0, behavior: 'auto' })
}

async function loadDetail() {
  const requestId = ++detailRequest
  loading.value = true
  error.value = ''
  try {
    let id = Number(route.query.galgameid)
    if (!Number.isSafeInteger(id) || id <= 0) {
      const slug = typeof route.query.slug === 'string' ? route.query.slug : ''
      if (!slug) throw new Error('链接中缺少作品编号，请从 Galgame 列表进入。')
      id = await idForSlug(slug) || 0
      if (requestId !== detailRequest) return
      if (!id) throw new Error('当前作品库暂未找到这部作品，请返回列表查看。')
    }
    const res = await getGalgameById(id)
    if (requestId !== detailRequest) return
    if (!res?.data) {
      error.value = '没有找到这部作品的资料，可能已被移除。'
      game.value = null
      return
    }
    game.value = res.data
  } catch (e: any) {
    if (requestId !== detailRequest) return
    error.value = e?.message || '网络连接失败，请稍后重试'
  } finally {
    if (requestId === detailRequest) loading.value = false
  }
}

onMounted(() => {
  window.scrollTo({ top: 0, behavior: 'auto' })
  loadDetail()
})

watch(() => [route.query.galgameid, route.query.slug], () => {
  window.scrollTo({ top: 0, behavior: 'auto' })
  loadDetail()
})
</script>

<style scoped>
.root-container{min-height:100vh;display:flex;flex-direction:column;color:#344e60}.background-layer{position:fixed;inset:0;background:#edf2f6;z-index:-1}.content-wrapper{box-sizing:border-box;width:min(1200px,100%);margin:28px auto 40px;padding:0 24px}
.game-hero{position:relative;isolation:isolate;overflow:hidden;padding:25px 30px 30px;border-radius:20px;background:linear-gradient(115deg,#213b53,#687e96 65%,#958897);color:white;box-shadow:0 12px 35px #203c531a}.hero-art{position:absolute;inset:-55px;background-size:cover;background-position:center;filter:blur(42px);opacity:.19;z-index:-1}.game-hero::after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,#172e48b5,#283e5f30);z-index:-1}
.breadcrumb{display:flex;flex-wrap:wrap;gap:8px;align-items:center;font-size:12px;color:#d8e2e8;margin-bottom:23px}.breadcrumb a{color:inherit;text-decoration:none}.hero-content{display:flex;gap:28px;align-items:center}.cover-wrapper{flex:0 0 180px;align-self:flex-start;border-radius:12px;overflow:hidden;box-shadow:0 10px 26px #081c4533;background:#d5e1ec}.cover-image{width:100%;aspect-ratio:3/4;object-fit:cover;display:block}.detail-wrapper{flex:1;min-width:0}.novel-title{font-size:30px;line-height:1.35;margin:0 0 7px;letter-spacing:-.5px;color:#fff}.original-title,.alias-line{font-size:12px;line-height:1.6;color:#d5dfe8;margin:4px 0;overflow-wrap:anywhere}.hero-rating{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:13px 0}.hero-rating>strong{font-size:34px;font-weight:650;line-height:1}.hero-rating>a,.hero-rating>span:not(.rating-stars){font-size:12px;color:#e1e7ed;text-decoration:none}.rating-stars{position:relative;display:inline-block;font-size:22px;letter-spacing:2px;white-space:nowrap;line-height:1.3;color:#b8bdc6}.stars-fill{position:absolute;left:0;top:0;overflow:hidden;color:#ffc955}.rating-pending{font-size:13px;color:#d5dfe8;margin:16px 0}
.hero-facts{display:flex;gap:12px 22px;flex-wrap:wrap;font-size:13px;margin:18px 0 12px}.hero-facts>div{display:flex;gap:7px}.hero-facts dt{color:#bdcdda;white-space:nowrap}.hero-facts dd{margin:0;color:#f1f5f8;overflow-wrap:anywhere}.release-note{font-size:11px;color:#ccd8e1;line-height:1.6;margin:4px 0 14px}.tag-row{display:flex;flex-wrap:wrap;gap:7px;margin:12px 0}.tag{display:inline-block;border:1px solid #ffffff54;border-radius:6px;background:#ffffff0e;padding:4px 10px;font-size:12px}.guide-tagline{color:#e1e9ef;font-size:13px;line-height:1.8;margin:13px 0 17px}.button-group{display:flex;flex-wrap:wrap;gap:9px}.main-button,.secondary-button{display:inline-flex;align-items:center;justify-content:center;border-radius:8px;padding:10px 16px;font:inherit;font-size:13px;cursor:pointer;text-decoration:none;line-height:1.4}.main-button{color:#294960;background:#fff;border:1px solid #fff;font-weight:600}.secondary-button{color:#edf3f7;background:#ffffff15;border:1px solid #ffffff54}.main-button:hover{background:#e7f7f5}.secondary-button:hover{background:#ffffff2a}.button-group :focus-visible{outline:2px solid #b9ffed;outline-offset:3px}
.state-card{padding:50px 25px;text-align:center;background:white;border-radius:16px}.state-card p{line-height:1.8;color:#778c9b}.state-icon{font-size:36px}.skeleton-panel{display:flex;gap:30px;background:#fff;padding:30px;border-radius:16px}.cover-skeleton{width:180px;height:240px;background:#e0e7ed;border-radius:12px}.detail-skeleton{flex:1}.sk-line{display:block;height:18px;background:#e9eef1;margin:20px 0;border-radius:5px}.source-list{list-style:none;padding:0}.source-item{margin:15px 0;padding-bottom:12px;border-bottom:1px solid #edf1f3}.source-item a{font-size:13px;color:#37748e}.source-time{font-size:11px;color:#91a0ab;margin-left:10px}.source-fields,.source-license,.source-note,.source-empty{color:#84959f;font-size:12px;line-height:1.9}.collected-line{text-align:center;color:#8799a5;font-size:11px;line-height:1.8}.section-title{font-size:21px;color:#314d60}
@media(max-width:680px){.content-wrapper{padding:0 14px;margin-top:18px}.game-hero{padding:20px;border-radius:16px}.breadcrumb{margin-bottom:18px}.hero-content{display:block}.cover-wrapper{float:left;width:95px;margin:0 16px 14px 0}.detail-wrapper{display:block}.hero-facts{clear:both}.novel-title{font-size:22px}.hero-rating{gap:9px;margin:12px 0}.hero-rating>strong{font-size:28px}.rating-stars{font-size:17px}.hero-rating>a{flex-basis:100%;line-height:1.7}.hero-facts{gap:9px;font-size:12px;flex-direction:column;margin-top:14px}.hero-facts>div{gap:10px}.tag-row{gap:6px}.tag{font-size:11px;padding:3px 7px}.guide-tagline{font-size:12px}.button-group{gap:7px}.main-button,.secondary-button{font-size:12px;padding:9px 10px}.original-title,.alias-line{font-size:11px}.source-time{display:block;margin:5px 0 0}.cover-skeleton{width:90px;height:120px}.skeleton-panel{padding:20px;gap:18px}}
@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto!important;transition:none!important}}
</style>

<style scoped src="../layout.css"></style>

<style scoped>

.reading-panel, .source-details { padding: 30px; margin-bottom: 24px; background: rgba(255,255,255,.97); border: 1px solid #e8edf0; border-radius: 15px; }
.reading-nav { display: flex; flex-wrap: wrap; gap: 8px; padding-bottom: 24px; border-bottom: 1px solid #e7edf1; }
.reading-nav button { border: 1px solid #d7e6ef; border-radius: 999px; padding: 8px 14px; color: #287da5; background: #f3faff; cursor: pointer; font: inherit; font-size: 13px; }
.reading-nav button:hover, .reading-nav button:focus-visible { background: #e1f3fc; outline: 2px solid #68b7de; outline-offset: 2px; }
.reading-section { padding-top: 30px; scroll-margin-top: 160px; }
.reading-eyebrow { color: #658da2; font-size: 11px; letter-spacing: 1.5px; }
.reading-section .section-title { margin-top: 10px; border: none; padding: 0; }
.reading-section p { font-size: 15px; color: #465a68; line-height: 1.95; margin: 12px 0; overflow-wrap: anywhere; }
.feature-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.feature-card { background: #f5fafc; border-radius: 12px; border: 1px solid #e2eff5; padding: 18px; }
.feature-number { color: #4898be; font-size: 12px; letter-spacing: 2px; }
.feature-card h3, .reading-pair h3 { margin: 10px 0; color: #2c4c60; font-size: 16px; }
.feature-card p, .reading-pair p { font-size: 14px; }
.reading-pair { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
.reading-section .reading-note { padding: 14px 18px; border-left: 3px solid #78b9d9; background: #f3f9fc; font-size: 14px; }
.source-details summary { color: #35596e; font-size: 15px; font-weight: 600; cursor: pointer; }
.sources-heading { margin: 22px 0 14px; font-size: 15px; color: #46677b; }
.source-details:not([open]) { padding: 20px 30px; }
@media (max-width: 768px) {
  .reading-panel, .source-details { padding: 22px; }
  .feature-grid, .reading-pair { grid-template-columns: 1fr; gap: 12px; }
  .reading-section p { font-size: 14px; }
}
</style>
