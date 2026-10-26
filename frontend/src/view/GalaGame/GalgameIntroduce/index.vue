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
              <span v-if="game.rating != null" class="status-value rating-value">
                {{ game.ratingSource || '评分' }} {{ formatRating(game.rating) }} / 10
              </span>
              <span v-else class="status-value muted-value">暂无评分</span>
              <span class="status-note">
                <template v-if="game.rating != null">
                  <a v-if="game.ratingSourceUrl" :href="game.ratingSourceUrl" target="_blank" rel="noopener noreferrer">
                    {{ formatCount(game.ratingCount) }} 人评分
                  </a>
                  <template v-else>{{ formatCount(game.ratingCount) }} 人评分</template>
                </template>
                <template v-else>来源未提供评分</template>
              </span>
            </div>
          </div>

          <div class="tag-row">
            <span v-for="tag in tags" :key="'detail-tag-' + tag" class="tag">{{ tag }}</span>
            <span v-if="!tags.length" class="tag muted">暂无标签</span>
          </div>

          <p class="description-text">{{ game.intro || game.shortIntro || '暂无简介。' }}</p>

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

      <div class="combined-container">
        <div class="detail-section">
          <h2 class="section-title">作品资料</h2>
          <div class="info-grid">
            <div class="info-row">
              <span class="info-label">中文名</span>
              <span class="info-value">{{ game.nameZh || '暂无资料' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">原名</span>
              <span class="info-value">{{ game.nameOriginal || '暂无资料' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">开发商</span>
              <span class="info-value">{{ game.developer || '暂无资料' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">发行日期</span>
              <span class="info-value">
                {{ game.releaseDate || '暂无资料' }}
                <em v-if="game.releaseNote" class="info-note">{{ game.releaseNote }}</em>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">支持平台</span>
              <span class="info-value">{{ game.platforms || '暂无资料' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">作品类型</span>
              <span class="info-value">{{ tags.length ? tags.join(' / ') : '暂无标签' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">评分</span>
              <span class="info-value">
                <template v-if="game.rating != null">
                  {{ game.ratingSource || '来源' }} {{ formatRating(game.rating) }} / 10
                  <em class="info-note">{{ formatCount(game.ratingCount) }} 人评分</em>
                </template>
                <template v-else>暂无评分</template>
              </span>
            </div>
          </div>
        </div>

        <div class="source-section">
          <h2 class="section-title">资料来源</h2>
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
            评分来自 VNDB，页面已注明评分人数；Bangumi 在本次采集环境中连接超时，未使用其评分或条目数据。
            封面仅用于作品识别，版权归原发行方所有；如权利方有异议，可替换为默认封面。
          </p>
        </div>
      </div>

      <div class="collected-line">
        采集时间：{{ formatDate(game.fetchedAt) }} · 本页仅提供作品介绍，不提供任何下载链接
      </div>
    </div>

    <Behind class="footer" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import Behind from '@/components/behind/index.vue'
import { getGalgameById } from '@/api/galgame'

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
  if (n == null) return '0'
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
  const id = Number(route.query.galgameid)
  if (!id) {
    loading.value = false
    error.value = '链接中缺少作品编号，请从 Galgame 列表进入。'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await getGalgameById(id)
    if (!res?.data) {
      error.value = '没有找到这部作品的资料，可能已被移除。'
      game.value = null
      return
    }
    game.value = res.data
  } catch (e: any) {
    error.value = e?.message || '网络连接失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
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
  flex: 1;
  max-width: 1200px;
  margin: 100px auto 50px;
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
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.combined-container {
  display: flex;
  gap: 30px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.detail-section {
  flex: 1;
  min-width: 0;
}

.source-section {
  flex: 0 0 340px;
  border-left: 1px solid #eee;
  padding-left: 30px;
}

.section-title {
  font-size: 1.35rem;
  color: #2c3e50;
  margin: 0 0 18px;
  padding-bottom: 10px;
  border-bottom: 2px solid #3498db;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 24px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fbfd;
  border: 1px solid #eef5f9;
}

.info-label {
  font-size: 12px;
  color: #95a5a6;
}

.info-value {
  color: #34495e;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.info-note {
  display: block;
  margin-top: 2px;
  font-style: normal;
  color: #9aa9b4;
  font-size: 12px;
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
  top: 200px;
}

@keyframes shimmer {
  to {
    background-position-x: -200%;
  }
}

@media (max-width: 1024px) {
  .combined-container {
    flex-direction: column;
  }
  .source-section {
    border-left: none;
    border-top: 1px solid #eee;
    padding-left: 0;
    padding-top: 26px;
    flex: none;
  }
}

@media (max-width: 768px) {
  .content-wrapper {
    margin-top: 112px;
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
  .combined-container {
    padding: 22px;
  }
  .info-grid {
    grid-template-columns: 1fr;
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
