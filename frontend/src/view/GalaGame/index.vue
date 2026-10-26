<template>
  <div class="root-container">
    <Headpage class="header" />

    <!-- 与番剧页一致的背景层 -->
    <div class="background-layer"></div>

    <main class="acgn_container">
      <!-- 编辑推荐轮播 -->
      <div v-if="hotLoading" class="hero-skeleton">
        <div class="hero-skeleton-cover"></div>
        <div class="hero-skeleton-lines">
          <span></span><span></span><span></span>
        </div>
      </div>
      <div v-else-if="hotList.length" class="block text-center">
        <el-carousel height="340px" :autoplay="false" indicator-position="outside">
          <el-carousel-item
            v-for="item in hotList"
            :key="'hot-' + item.id"
            @click="gotoDetail(item.id)"
          >
            <div class="hero-slide" role="link" tabindex="0" @keydown.enter.prevent="gotoDetail(item.id)">
              <div class="hero-bg" :style="{ backgroundImage: `url(${coverOf(item)})` }"></div>
              <div class="hero-shade"></div>
              <div class="hero-body">
                <img :src="coverOf(item)" class="hero-cover" alt="" @error="onImgError">
                <div class="hero-info">
                  <div class="hero-kicker">GALGAME · 编辑推荐</div>
                  <h2 class="hero-title">{{ item.nameZh }}</h2>
                  <div v-if="item.nameOriginal && item.nameOriginal !== item.nameZh" class="hero-original">
                    {{ item.nameOriginal }}
                  </div>
                  <p class="hero-desc">{{ item.shortIntro }}</p>
                  <div class="hero-meta">
                    <span v-if="item.releaseYear">{{ item.releaseYear }} 年</span>
                    <span v-if="item.developer">{{ item.developer }}</span>
                    <span v-if="item.rating != null && ratingAllowed(item.slug)" class="hero-rating">
                      {{ item.ratingSource || '评分' }} {{ formatRating(item.rating) }} / 10
                      <em v-if="item.ratingCount">· {{ formatCount(item.ratingCount) }} 人评分</em>
                    </span>
                  </div>
                  <div class="hero-tags">
                    <span v-for="tag in tagsOf(item)" :key="'hot-tag-' + tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div class="section-head">
        <div>
          <h1>Galgame 作品库</h1>
          <p>剧情简介、玩法特点与版本资料。</p>
        </div>
        <span class="section-count" v-if="!initialLoading && !loadError">已载入 {{ list.length }} 部</span>
      </div>

      <AcgnFilterBar
        :key="filterResetKey"
        :genres="GALGAME_GENRES"
        :show-status="false"
        :sort-options="GALGAME_SORT_OPTIONS"
        @change="onFilterChange"
      />

      <!-- 首次加载骨架 -->
      <div v-if="initialLoading" class="acgn_show" aria-label="加载中">
        <div v-for="n in 8" :key="'skeleton-' + n" class="skeleton-card">
          <div class="skeleton-cover"></div>
          <div class="skeleton-body">
            <span class="skeleton-line w70"></span>
            <span class="skeleton-line w50"></span>
            <span class="skeleton-line"></span>
            <span class="skeleton-line w60"></span>
          </div>
        </div>
      </div>

      <!-- 请求失败 -->
      <div v-else-if="loadError" class="state-card error-state">
        <div class="state-icon">!</div>
        <h3>作品列表暂时没有加载出来</h3>
        <p>{{ loadError }}</p>
        <el-button type="primary" round @click="reloadList">重新加载</el-button>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!list.length" class="state-card">
        <div class="state-icon">∅</div>
        <h3>暂无符合条件的作品</h3>
        <p>换一个题材或年代，看看还有哪些故事。</p>
        <el-button round @click="resetFilters">清除筛选</el-button>
      </div>

      <!-- 作品网格 -->
      <div v-else class="acgn_show">
        <div
          v-for="item in list"
          :key="'galgame-' + item.id"
          class="card-container"
          role="link"
          tabindex="0"
          @click="gotoDetail(item.id)"
          @keydown.enter.prevent="gotoDetail(item.id)"
        >
          <div class="card-inner">
            <div class="image-wrapper">
              <img
                :src="coverOf(item)"
                alt="作品封面"
                class="cover-image"
                loading="lazy"
                @error="onImgError"
              >
              <span v-if="item.releaseYear" class="cover-badge">{{ item.releaseYear }}</span>
              <span v-if="item.rating != null && ratingAllowed(item.slug)" class="cover-rating">{{ item.ratingSource || '评分' }} ★ {{ formatRating(item.rating) }}</span>
            </div>
            <div class="content-wrapper">
              <h4 class="title">{{ item.nameZh }}</h4>
              <div v-if="item.nameOriginal && item.nameOriginal !== item.nameZh" class="original">
                {{ item.nameOriginal }}
              </div>
              <p class="description">{{ guideFor(item.slug)?.tagline || item.shortIntro || '暂无简介' }}</p>
              <p v-if="item.rating != null && ratingAllowed(item.slug) && item.ratingCount != null" class="rating-count">{{ formatCount(item.ratingCount) }} 人评分</p>
              <div class="meta-row">
                <span v-if="item.developer" class="meta-item">{{ item.developer }}</span>
                <span v-if="item.platforms" class="meta-item platform">{{ platformShort(item.platforms) }}</span>
              </div>
              <div class="tag-row">
                <span v-for="tag in tagsOf(item)" :key="'tag-' + item.id + '-' + tag" class="tag">
                  {{ tag }}
                </span>
                <span v-if="!tagsOf(item).length" class="tag muted">暂无标签</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="list.length" class="tishi">
        <el-button v-if="hasMore" :loading="isloading" @click="loadMore">{{ moreError ? '重试加载更多' : '加载更多作品' }}</el-button>
        <span v-else>已显示全部作品</span>
        <p v-if="moreError" role="alert">{{ moreError }}</p>
      </div>
    </main>

    <Behind class="bottom" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import Behind from '@/components/behind/index.vue'
import AcgnFilterBar from '@/components/AcgnFilterBar.vue'
import { getGalgame, getGalgameHot, type GalgameListParams } from '@/api/galgame'
import { GALGAME_GENRES, GALGAME_SORT_OPTIONS, decadeRange } from '@/utils/acgnConstants'
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
  bangumiUrl?: string
  vndbUrl?: string
  officialUrl?: string
  steamUrl?: string
  sourcesJson?: string
  sourceNote?: string
  featured?: number
  fetchedAt?: string
}

const PLACEHOLDER = '/galgame/placeholder.svg'
const router = useRouter()
const filterResetKey = ref(0)
let listRequest = 0
const moreError = ref('')
const list = ref<Galgame[]>([])
const hotList = ref<Galgame[]>([])
const params = ref<GalgameListParams>({})
const page = ref(2)
const hasMore = ref(true)
const initialLoading = ref(true)
const hotLoading = ref(true)
const isloading = ref(false)
const loadError = ref('')

function coverOf(item: Galgame) {
  return item.cover || PLACEHOLDER
}

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  if (!img || img.src.endsWith(PLACEHOLDER)) return
  img.src = PLACEHOLDER
}

function tagsOf(item: Galgame) {
  return (item.genres || '')
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
    .slice(0, 4)
}

function platformShort(platforms: string) {
  const parts = platforms.split(' / ').filter(Boolean)
  if (parts.length <= 2) return parts.join(' / ')
  return `${parts.slice(0, 2).join(' / ')} +${parts.length - 2}`
}

function formatCount(n: number) {
  return Number(n).toLocaleString('zh-CN')
}

function formatRating(v: number | string | null | undefined) {
  if (v == null) return ''
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(1) : String(v)
}

function gotoDetail(id: number) {
  router.push({ name: 'GalgameIntroduce', query: { galgameid: id } })
}

async function loadHot() {
  hotLoading.value = true
  try {
    const res = await getGalgameHot()
    hotList.value = Array.isArray(res?.data) ? res.data : []
  } catch {
    hotList.value = []
  } finally {
    hotLoading.value = false
  }
}

async function reloadList() {
  const requestId = ++listRequest
  moreError.value = ''
  initialLoading.value = true
  loadError.value = ''
  isloading.value = false
  hasMore.value = true
  page.value = 2
  list.value = []
  try {
    const res = await getGalgame(1, params.value)
    if (requestId !== listRequest) return
    const rows = Array.isArray(res?.data) ? res.data : []
    list.value = rows
    hasMore.value = rows.length >= 20
  } catch (e: any) {
    if (requestId !== listRequest) return
    loadError.value = e?.message || '网络连接失败，请稍后重试'
  } finally {
    if (requestId === listRequest) initialLoading.value = false
  }
}

function onFilterChange(f: { genre?: string; decade?: string; sortBy?: string }) {
  const yr = decadeRange(f.decade || '')
  params.value = {
    genre: f.genre || undefined,
    sortBy: (f.sortBy as GalgameListParams['sortBy']) || 'default',
    ...yr,
  }
  reloadList()
}

function resetFilters() {
  params.value = {}
  filterResetKey.value += 1
}

async function loadMore() {
  if (isloading.value || !hasMore.value || initialLoading.value || loadError.value) return
  isloading.value = true
  moreError.value = ''
  const requestId = listRequest
  try {
    const res = await getGalgame(page.value, params.value)
    if (requestId !== listRequest) return
    const rows = Array.isArray(res?.data) ? res.data : []
    hasMore.value = rows.length >= 20
    if (rows.length) list.value.push(...rows)
    page.value += 1
  } catch {
    if (requestId === listRequest) moreError.value = '暂时没能加载更多，已显示的作品仍可浏览。'
  } finally {
    if (requestId === listRequest) isloading.value = false
  }
}

onMounted(() => {
  window.scrollTo({ top: 0, behavior: 'auto' })
  loadHot()
})
</script>

<style scoped>
.root-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  position: sticky;
  top: 0;
  z-index: 1000;
}

/* 与番剧页一致的背景层 */
.background-layer {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-45%);
  width: 100vw;
  height: 0;
  padding-top: 56.25%;
  background: url('/BCPHOTO/acgnbc4.png') no-repeat center/contain;
  overflow: hidden;
  z-index: -1;
}

.acgn_container {
  box-sizing: border-box;
  width: min(1200px, 92%);
  margin: 32px auto 40px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(211, 211, 211, 0.8);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 28px;
  min-height: 60vh;
  position: relative;
  scrollbar-width: thin;
  scrollbar-color: #cfe6f3 transparent;
}

.acgn_container::-webkit-scrollbar {
  width: 8px;
}
.acgn_container::-webkit-scrollbar-thumb {
  background: #cfe6f3;
  border-radius: 8px;
}

/* 轮播 */
.hero-slide {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  background: #eef7fc;
}

.hero-bg {
  position: absolute;
  inset: -20px;
  background-size: cover;
  background-position: center;
  filter: blur(18px) brightness(0.55) saturate(1.1);
  transform: scale(1.08);
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, rgba(8, 24, 36, 0.8) 0%, rgba(8, 24, 36, 0.5) 45%, rgba(8, 24, 36, 0.25) 100%);
}

.hero-body {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 28px;
  height: 100%;
  padding: 24px 36px;
  box-sizing: border-box;
}

.hero-cover {
  width: 180px;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
  flex: 0 0 auto;
  background: #eef7fc;
}

.hero-info {
  color: #fff;
  min-width: 0;
}

.hero-kicker {
  display: inline-block;
  padding: 4px 12px;
  margin-bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #d7f1ff;
  background: rgba(0, 174, 236, 0.2);
}

.hero-title {
  margin: 0 0 6px;
  font-size: clamp(1.7rem, 2.6vw, 2.4rem);
  line-height: 1.2;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
}

.hero-original {
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.95rem;
  margin-bottom: 14px;
  word-break: break-all;
}

.hero-desc {
  margin: 0 0 18px;
  max-width: 760px;
  line-height: 1.75;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-meta span {
  position: relative;
}

.hero-meta span + span::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  transform: translateY(-50%);
}

.hero-rating {
  color: #ffe08a;
  font-weight: 600;
}

.hero-rating em {
  font-style: normal;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.7);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-tags span {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #f2fbff;
  font-size: 12px;
}

.hero-skeleton {
  display: flex;
  gap: 40px;
  align-items: center;
  box-sizing: border-box;
  height: 340px;
  padding: 24px 36px;
  margin-bottom: 20px;
  border-radius: 20px;
  background: linear-gradient(110deg, #e8f4fb 8%, #f5fbff 18%, #e8f4fb 33%);
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}

.hero-skeleton-cover {
  width: 230px;
  aspect-ratio: 3 / 4;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  flex: 0 0 auto;
}

.hero-skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-skeleton-lines span {
  height: 18px;
  width: 60%;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.65);
}

.hero-skeleton-lines span:nth-child(2) {
  width: 82%;
  height: 14px;
}

.hero-skeleton-lines span:nth-child(3) {
  width: 70%;
  height: 14px;
}

/* 标题与筛选 */
.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-top: 34px;
}

.section-head h1 {
  margin: 0 0 6px;
  font-size: 1.7rem;
  color: #2c3e50;
}

.section-head p {
  margin: 0;
  color: #8a9aa6;
  font-size: 13px;
}

.section-count {
  color: #00aeec;
  font-weight: 600;
  white-space: nowrap;
}

/* 卡片网格 */
.acgn_show {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  width: auto;
  height: auto;
  min-height: 360px;
  margin-top: 20px;
  padding: 8px 4px 24px;
}

.card-container {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  background: #fff;
  cursor: pointer;
  outline: none;
}

.card-container:hover,
.card-container:focus-visible {
  transform: translateY(-4px);
  box-shadow: 0 12px 26px rgba(52, 152, 219, 0.2);
}

.card-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: #f1f8fc;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-wrapper:hover .cover-image {
  transform: scale(1.05);
}

.cover-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.cover-rating {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(255, 237, 179, 0.95);
  color: #8a5a00;
  font-size: 12px;
  font-weight: 700;
}

.content-wrapper {
  padding: 14px 16px 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.title {
  margin: 0 0 4px;
  font-size: 1.02rem;
  line-height: 1.35;
  color: #2c3e50;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.original {
  margin-bottom: 8px;
  font-size: 11px;
  color: #9aa9b4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.description {
  margin: 0 0 10px;
  font-size: 0.8rem;
  color: #687784;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5em;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-bottom: 8px;
  font-size: 11px;
  color: #8a9aa6;
}

.meta-item {
  position: relative;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-item + .meta-item::before {
  content: '·';
  margin-right: 10px;
  color: #c3d0d9;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f0f8fd;
  color: #4d91b5;
  font-size: 11px;
  line-height: 1.6;
}

.tag.muted {
  color: #a6b4be;
  background: #f6f8fa;
}

/* 状态与骨架 */
.state-card {
  margin: 28px 0 12px;
  padding: 46px 24px;
  text-align: center;
  border: 1px dashed #cfe1ec;
  border-radius: 12px;
  background: #f8fcfe;
  color: #61727f;
}

.state-card h3 {
  margin: 12px 0 8px;
  color: #2c3e50;
}

.state-card p {
  margin: 0 auto 18px;
  max-width: 560px;
  line-height: 1.7;
  font-size: 13px;
}

.state-icon {
  width: 46px;
  height: 46px;
  line-height: 46px;
  margin: 0 auto;
  border-radius: 50%;
  background: #e6f4fb;
  color: #4d9cc4;
  font-size: 22px;
  font-weight: 700;
}

.error-state .state-icon {
  background: #fdeeee;
  color: #d96666;
}

.skeleton-card {
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.06);
}

.skeleton-cover {
  aspect-ratio: 3 / 4;
  background: linear-gradient(110deg, #e8f4fb 8%, #f5fbff 18%, #e8f4fb 33%);
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}

.skeleton-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: #eef4f8;
}

.skeleton-line.w70 {
  width: 70%;
}
.skeleton-line.w60 {
  width: 60%;
}
.skeleton-line.w50 {
  width: 50%;
}

.tishi {
  text-align: center;
  padding: 1rem;
  color: #666;
  font-size: 0.9rem;
  background: #f8f9fa;
  border-radius: 4px;
  margin: 10px 0;
  border: 1px solid #e9ecef;
}

.bottom {
  position: relative;
  margin-top: auto;
}

@keyframes shimmer {
  to {
    background-position-x: -200%;
  }
}

@media (max-width: 1300px) {
  .acgn_container {
    width: 78%;
  }
  .acgn_show {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .hero-cover {
    width: 200px;
  }
  .hero-body {
    padding: 36px 40px;
    gap: 30px;
  }
}

@media (max-width: 1024px) {
  .acgn_container {
    width: 88%;
    padding: 28px;
  }
  .acgn_show {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }
  .hero-body {
    padding: 30px 32px;
  }
  .hero-cover {
    width: 170px;
  }
}

@media (max-width: 768px) {
  .acgn_container {
    width: 94%;
    margin-top: 24px;
    padding: 18px;
    max-height: none;
    overflow-y: visible;
  }
  .section-head {
    margin-top: 24px;
  }
  .section-head h1 {
    font-size: 1.4rem;
  }
  .acgn_show {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
    padding: 4px 0 20px;
  }
  .hero-body {
    padding: 22px;
    gap: 18px;
  }
  .hero-cover {
    width: 120px;
  }
  .hero-title {
    font-size: 1.35rem;
  }
  .hero-desc {
    font-size: 13px;
    -webkit-line-clamp: 2;
  }
  .hero-meta {
    font-size: 12px;
    gap: 8px 14px;
  }
  .hero-tags span {
    font-size: 11px;
  }
  .hero-skeleton {
    height: 300px;
    padding: 22px;
  }
  .hero-skeleton-cover {
    width: 120px;
  }
}

@media (max-width: 520px) {
  .acgn_container {
    width: 96%;
    padding: 14px;
  }
  .acgn_show {
    grid-template-columns: 1fr;
  }
  .image-wrapper {
    aspect-ratio: 4 / 5;
  }
  .hero-slide :deep(.el-carousel__container) {
    height: 360px !important;
  }
  .hero-body {
    align-items: flex-end;
    padding: 18px;
  }
  .hero-cover {
    display: none;
  }
  .hero-desc {
    -webkit-line-clamp: 3;
  }
}
</style>

<style scoped src="./layout.css"></style>

<style scoped>
.rating-count { margin: 0 0 10px; color: #697d8c; font-size: 11px; }
@media (max-width: 520px) {
  .hero-body { align-items: center; }
  .hero-meta { gap: 6px 14px; }
  .section-head { align-items: flex-start; }
  .section-head p { line-height: 1.7; }
}
</style>
