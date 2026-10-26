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
      <div class="info-container">
        <div class="cover-wrapper">
          <img :src="coverOf" class="cover-image" alt="作品封面" @error="onImgError">
        </div>

        <div class="detail-wrapper">
          <div class="breadcrumb">
            <router-link to="/Galagame">Galgame</router-link>
            <span>/</span>
            <span>{{ game.nameZh }}</span>
          </div>

          <h1 class="novel-title">{{ game.nameZh }}</h1>
          <div v-if="game.nameOriginal && game.nameOriginal !== game.nameZh" class="original-title">
            {{ game.nameOriginal }}
          </div>
          <div v-if="game.aliases" class="alias-line">别名：{{ game.aliases }}</div>

          <div class="status-container">
            <div class="status-item">
              <span class="status-label">发行日期</span>
              <span class="status-value">{{ game.releaseDate || '暂无资料' }}</span>
              <span v-if="game.releaseNote" class="status-note">{{ game.releaseNote }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">开发商</span>
              <span class="status-value">{{ game.developer || '暂无资料' }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">支持平台</span>
              <span class="status-value platform-value">{{ game.platforms || '暂无资料' }}</span>
            </div>
            <div class="status-item rating-item">
              <span class="status-label">作品评分</span>
              <span v-if="game.rating != null && ratingAllowed(game.slug)" class="status-value rating-value">
                {{ game.ratingSource || '评分' }} {{ formatRating(game.rating) }} / 10
              </span>
              <span v-else class="status-value muted-value">{{ ratingAllowed(game.slug) ? '暂无评分' : '评分信息核对中' }}</span>
              <span class="status-note">
                <template v-if="game.rating != null && ratingAllowed(game.slug)">
                  <a v-if="game.ratingSourceUrl && game.ratingCount != null" :href="game.ratingSourceUrl" target="_blank" rel="noopener noreferrer">
                    {{ formatCount(game.ratingCount) }} 人评分
                  </a>
                  <template v-else-if="game.ratingCount != null">{{ formatCount(game.ratingCount) }} 人评分</template>
                  <template v-else>评分人数未提供</template>
                </template>
                <template v-else>{{ ratingAllowed(game.slug) ? '来源未提供评分' : '确认对应篇章后再展示' }}</template>
              </span>
            </div>
          </div>

          <div class="tag-row">
            <span v-for="tag in tags" :key="'detail-tag-' + tag" class="tag">{{ tag }}</span>
            <span v-if="!tags.length" class="tag muted">暂无标签</span>
          </div>

          <p v-if="guide" class="guide-tagline">{{ guide.tagline }}</p>
          <p v-else class="description-text">{{ game.intro || game.shortIntro || '暂无简介。' }}</p>

          <div class="button-group">
            <a
              v-if="officialLink"
              class="main-button"
              :href="officialLink"
              target="_blank"
              rel="noopener noreferrer"
            >
              <span>↗</span> 官方 / 商店页
            </a>
            <a
              v-if="game.vndbUrl"
              class="secondary-button"
              :href="game.vndbUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              <span>VNDB</span> 条目
            </a>
            <button type="button" class="secondary-button" @click="backToList">
              <span>←</span> 返回列表
            </button>
          </div>
        </div>
      </div>

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
          <p class="source-note">
            评分对应所链接的来源条目，支持平台可能涵盖多个历史发行版本，具体内容与语言以所选商品为准。
            封面版权归原权利人，原始资料遵循各来源许可。
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

function scrollToSection(section: string) {
  document.getElementById(`guide-${section}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const coverOf = computed(() => game.value?.cover || PLACEHOLDER)
const officialLink = computed(() => game.value?.officialUrl || game.value?.steamUrl || '')
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
  const id = Number(route.query.galgameid)
  if (!Number.isSafeInteger(id) || id <= 0) {
    loading.value = false
    error.value = '链接中缺少作品编号，请从 Galgame 列表进入。'
    return
  }
  loading.value = true
  error.value = ''
  try {
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

watch(() => route.query.galgameid, () => {
  window.scrollTo({ top: 0, behavior: 'auto' })
  loadDetail()
})
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  position: sticky;
  top: 0;
  z-index: 1000;
}

.background-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('/BCPHOTO/bc9.jpg');
  background-size: cover;
  background-attachment: fixed;
  z-index: -1;
}

.content-wrapper {
  box-sizing: border-box;
  width: min(1200px, 100%);
  margin: 32px auto 40px;
  padding: 0 20px;
}

.info-container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.cover-wrapper {
  flex: 0 0 280px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  background: #f1f8fc;
  align-self: flex-start;
}

.cover-image {
  width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
}

.detail-wrapper {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #93a4b1;
  font-size: 13px;
}

.breadcrumb a {
  color: #3498db;
  text-decoration: none;
}

.breadcrumb a:hover {
  text-decoration: underline;
}

.novel-title {
  font-size: 2.2rem;
  color: #2c3e50;
  margin: 0 0 8px;
  line-height: 1.2;
  word-break: break-word;
}

.original-title {
  font-size: 1rem;
  color: #8a9aa6;
  margin-bottom: 6px;
  word-break: break-all;
}

.alias-line {
  font-size: 13px;
  color: #9aa9b4;
  margin-bottom: 14px;
  line-height: 1.6;
}

.status-container {
  display: flex;
  gap: 30px;
  padding: 15px 0;
  border-bottom: 1px solid #ecf0f1;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.status-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 120px;
  max-width: 260px;
}

.status-label {
  font-size: 0.85rem;
  color: #95a5a6;
  margin-bottom: 5px;
}

.status-value {
  font-size: 1.05rem;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.4;
}

.platform-value {
  font-size: 0.92rem;
  font-weight: 500;
}

.rating-value {
  color: #b7791f;
}

.muted-value {
  color: #a6b4be;
  font-weight: 500;
}

.status-note {
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.5;
  color: #9aa9b4;
}

.status-note a {
  color: #3498db;
  text-decoration: none;
}

.status-note a:hover {
  text-decoration: underline;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f0f8fd;
  color: #4d91b5;
  font-size: 12px;
  line-height: 1.5;
}

.tag.muted {
  color: #a6b4be;
  background: #f6f8fa;
}

.description-text {
  line-height: 1.85;
  color: #34495e;
  font-size: 15px;
  margin: 8px 0 24px;
  white-space: pre-wrap;
}

.button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: auto;
}

.main-button,
.secondary-button {
  padding: 11px 22px;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.main-button {
  background: #3498db;
  color: #fff;
}

.secondary-button {
  background: #f8f9fa;
  border: 1px solid #bdc3c7;
  color: #2c3e50;
}

.main-button:hover,
.secondary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(52, 152, 219, 0.18);
}

.section-title {
  font-size: 1.35rem;
  color: #2c3e50;
  margin: 0 0 18px;
  padding-bottom: 10px;
  border-bottom: 2px solid #3498db;
}

.source-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.source-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #eef5f9;
}

.source-item a {
  color: #3498db;
  font-weight: 600;
  text-decoration: none;
  font-size: 14px;
}

.source-item a:hover {
  text-decoration: underline;
}

.source-time {
  margin-left: 8px;
  color: #9aa9b4;
  font-size: 11px;
}

.source-fields {
  margin-top: 5px;
  color: #61727f;
  font-size: 12px;
  line-height: 1.6;
}

.source-license {
  margin-top: 4px;
  color: #8a9aa6;
  font-size: 11px;
}

.source-note {
  margin: 12px 0 0;
  color: #8a9aa6;
  font-size: 12px;
  line-height: 1.7;
}

.source-empty {
  color: #9aa9b4;
  font-size: 13px;
}

.collected-line {
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e9ecef;
  color: #8a9aa6;
  font-size: 12px;
  text-align: center;
}

/* 状态与骨架 */
.state-card {
  margin-top: 30px;
  padding: 60px 30px;
  text-align: center;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 15px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  color: #61727f;
}

.state-card h2 {
  color: #2c3e50;
  margin: 14px 0 8px;
}

.state-card p {
  max-width: 560px;
  margin: 0 auto 22px;
  line-height: 1.7;
}

.state-icon {
  width: 52px;
  height: 52px;
  line-height: 52px;
  margin: 0 auto;
  border-radius: 50%;
  background: #fdeeee;
  color: #d96666;
  font-size: 24px;
  font-weight: 700;
}

.skeleton-panel {
  align-items: stretch;
}

.cover-skeleton {
  flex: 0 0 280px;
  min-height: 380px;
  border-radius: 10px;
  background: linear-gradient(110deg, #e8f4fb 8%, #f5fbff 18%, #e8f4fb 33%);
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}

.detail-skeleton {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 12px;
}

.sk-line {
  height: 14px;
  width: 70%;
  border-radius: 7px;
  background: #eef4f8;
}

.sk-line.tall {
  height: 34px;
  width: 55%;
}

.sk-line.w80 {
  width: 80%;
}
.sk-line.w70 {
  width: 70%;
}
.sk-line.w50 {
  width: 50%;
}
.sk-line.w40 {
  width: 40%;
}

.footer {
  position: relative;
  margin-top: auto;
}

@keyframes shimmer {
  to {
    background-position-x: -200%;
  }
}

@media (max-width: 768px) {
  .content-wrapper {
    margin-top: 24px;
    padding: 0 15px;
  }
  .info-container {
    flex-direction: column;
    padding: 22px;
  }
  .cover-wrapper {
    flex: none;
    max-width: 280px;
    width: 70%;
    margin: 0 auto;
  }
  .novel-title {
    font-size: 1.7rem;
  }
  .status-container {
    gap: 16px 22px;
  }
  .status-item {
    min-width: 100px;
    max-width: 100%;
  }
}

@media (max-width: 520px) {
  .cover-wrapper {
    width: 62%;
  }
  .status-container {
    flex-direction: column;
  }
  .button-group {
    flex-direction: column;
  }
  .main-button,
  .secondary-button {
    justify-content: center;
  }
}
</style>

<style scoped src="../layout.css"></style>

<style scoped>
.guide-tagline { margin: 6px 0 0; color: #2385b4; font-size: 16px; line-height: 1.8; font-weight: 500; }
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
