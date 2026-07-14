<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10" />
    <div class="bg" />
    <div class="wrap">
      <h1 class="h1">全站搜索</h1>
      <p class="muted">支持番剧 / 漫画 / 轻小说统一检索，并可按题材、状态、年代与排序组合筛选。</p>

      <div class="bar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入作品名关键词"
          class="kw"
          @keydown.enter.prevent="runSearch"
        />
        <el-button type="primary" @click="runSearch">搜索</el-button>
        <el-button @click="syncFromRoute">重置为地址栏</el-button>
      </div>

      <div class="filters">
        <el-select v-model="mediaType" placeholder="类型" clearable class="sel">
          <el-option label="不限类型" value="" />
          <el-option label="动漫" value="animation" />
          <el-option label="漫画" value="comic" />
          <el-option label="轻小说" value="novel" />
        </el-select>
        <el-select v-model="genre" placeholder="题材" clearable class="sel">
          <el-option label="不限题材" value="" />
          <el-option v-for="g in ACGN_GENRES" :key="g" :label="g" :value="g" />
        </el-select>
        <el-select v-model="status" placeholder="状态" clearable class="sel">
          <el-option label="不限状态" value="" />
          <el-option label="连载" value="连载" />
          <el-option label="完结" value="完结" />
        </el-select>
        <el-select v-model="decade" placeholder="年代" clearable class="sel">
          <el-option label="不限年代" value="" />
          <el-option label="2020年代" value="2020s" />
          <el-option label="2010年代" value="2010s" />
          <el-option label="2000年代" value="2000s" />
          <el-option label="1990年代" value="1990s" />
          <el-option label="更早" value="earlier" />
        </el-select>
        <el-select v-model="sortBy" placeholder="排序" class="sel">
          <el-option label="默认" value="default" />
          <el-option label="热度（评分）" value="hot" />
          <el-option label="更新时间" value="update" />
        </el-select>
      </div>

      <div v-if="loading" class="hint">加载中…</div>
      <div v-else-if="!rows.length" class="empty-block">
        <p class="empty-title">暂无匹配结果</p>
        <p class="muted">试着换个关键词，或放宽题材 / 年代筛选。</p>
        <div v-if="recs.length" class="rec-wrap">
          <h3>为你推荐 · 各频道热门</h3>
          <div class="rec-grid">
            <button v-for="(it, i) in recs" :key="i" type="button" class="rec-card" @click="goRec(it)">
              <img v-if="it.pic" :src="it.pic" class="rec-img" alt="" />
              <div class="rec-meta">
                <span class="rec-name">{{ it.name }}</span>
                <span class="rec-sub">{{ it.type }}</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <ul v-else class="list">
        <li v-for="(item, index) in rows" :key="index" class="row" @click="open(item)">
          <img v-if="item.picurl" class="thumb" :src="item.picurl" alt="" />
          <div class="mid">
            <span class="tag">{{ item.sourcetype }}</span>
            <div class="title">
              <template v-for="(seg, j) in keywordSegments(item.name, keyword)" :key="j">
                <mark v-if="seg.hl" class="hl">{{ seg.text }}</mark>
                <template v-else>{{ seg.text }}</template>
              </template>
            </div>
          </div>
        </li>
      </ul>
    </div>
    <Behind class="foot" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import { getAllSearch, type GlobalSearchParams } from '@/api/AllSearch'
import { ACGN_GENRES, decadeRange } from '@/utils/acgnConstants'
import { keywordSegments } from '@/utils/highlightKeyword'
import { getAnimationhot } from '@/api/animation'
import { getcartoonhot } from '@/api/cartoon'
import { getnovelhot } from '@/api/novel'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const genre = ref('')
const status = ref('')
const decade = ref('')
const sortBy = ref('default')
const mediaType = ref('')
const loading = ref(false)
const rows = ref<
  Array<{
    id: number
    name: string
    picurl?: string
    sourcetype: string
  }>
>([])

const recs = ref<Array<{ id: number; name: string; pic?: string; type: string; path: string; query: Record<string, string> }>>([])

const yearSpan = computed(() => decadeRange(decade.value))

function applyRouteQuery() {
  const q = route.query as Record<string, string | undefined>
  keyword.value = (q.q || q.search || '').trim()
  genre.value = q.genre || ''
  status.value = q.status || ''
  decade.value = q.decade || ''
  sortBy.value = q.sortBy || 'default'
  mediaType.value = q.mediaType || ''
}

async function fetchRecs() {
  try {
    const [ah, ch, nh] = await Promise.all([
      getAnimationhot().catch(() => ({ code: 0, data: [] as any[] })),
      getcartoonhot().catch(() => ({ code: 0, data: [] as any[] })),
      getnovelhot().catch(() => ({ code: 0, data: [] as any[] })),
    ])
    const a = ah?.code === 200 ? ah.data?.slice?.(0, 3) ?? [] : []
    const c = ch?.code === 200 ? ch.data?.slice?.(0, 3) ?? [] : []
    const n = nh?.code === 200 ? nh.data?.slice?.(0, 3) ?? [] : []
    const out: typeof recs.value = []
    for (const x of a) {
      out.push({
        id: x.id,
        name: x.name,
        pic: x.bcimg || x.hengpic,
        type: '动漫',
        path: '/AnimationIntroduce',
        query: { animationid: String(x.id) },
      })
    }
    for (const x of c) {
      out.push({
        id: x.comicid,
        name: x.name,
        pic: x.picurl || x.hengpic,
        type: '漫画',
        path: '/ComicIntroduce',
        query: { comicid: String(x.comicid) },
      })
    }
    for (const x of n) {
      const id = x.novelid ?? x.id
      out.push({
        id,
        name: x.name,
        pic: x.picurl || x.hengpic,
        type: '轻小说',
        path: '/NovelIntroduce',
        query: { novelid: String(id) },
      })
    }
    recs.value = out.slice(0, 12)
  } catch {
    recs.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const yrs = yearSpan.value
    const payload: GlobalSearchParams = {
      search: keyword.value || undefined,
      genre: genre.value || undefined,
      status: status.value || undefined,
      yearMin: yrs.yearMin,
      yearMax: yrs.yearMax,
      sortBy: sortBy.value,
      mediaType: mediaType.value || undefined,
      limit: 200,
    }
    const res: any = await getAllSearch(payload)
    rows.value = res?.code === 200 && Array.isArray(res.data) ? res.data : []
    if (!rows.value.length) {
      await fetchRecs()
    } else {
      recs.value = []
    }
  } catch {
    rows.value = []
    await fetchRecs()
  } finally {
    loading.value = false
  }
}

function runSearch() {
  const q: Record<string, string> = {}
  if (keyword.value.trim()) q.q = keyword.value.trim()
  if (genre.value) q.genre = genre.value
  if (status.value) q.status = status.value
  if (decade.value) q.decade = decade.value
  if (sortBy.value && sortBy.value !== 'default') q.sortBy = sortBy.value
  if (mediaType.value) q.mediaType = mediaType.value
  router.replace({ path: '/search', query: q })
}

function syncFromRoute() {
  applyRouteQuery()
  void fetchList()
}

function open(item: { sourcetype: string; id: number }) {
  if (item.sourcetype === '轻小说') {
    router.push({ name: 'NovelIntroduce', query: { novelid: String(item.id) } })
  } else if (item.sourcetype === '漫画') {
    router.push({ name: 'ComicIntroduce', query: { comicid: String(item.id) } })
  } else {
    router.push({ name: 'Animationintroduce', query: { animationid: String(item.id) } })
  }
}

function goRec(it: (typeof recs.value)[0]) {
  router.push({ path: it.path, query: it.query })
}

watch(
  () => route.fullPath,
  () => {
    applyRouteQuery()
    void fetchList()
  },
)

onMounted(() => {
  applyRouteQuery()
  void fetchList()
})
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background-image: url('/BCPHOTO/bc9.jpg');
  background-size: cover;
  background-position: center;
  opacity: 0.35;
}
.wrap {
  flex: 1;
  max-width: 980px;
  margin: 100px auto 48px;
  padding: 0 20px 40px;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}
.h1 {
  margin: 24px 0 8px;
  font-size: 26px;
}
.muted {
  color: #909399;
  font-size: 14px;
  line-height: 1.6;
}
.bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 24px 0 12px;
  align-items: center;
}
.kw {
  flex: 1 1 280px;
  max-width: 520px;
}
.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}
.sel {
  width: 150px;
}
.hint {
  padding: 20px;
  color: #606266;
}
.list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.row {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.row:hover {
  background: #fafafa;
}
.thumb {
  width: 54px;
  height: 76px;
  object-fit: cover;
  border-radius: 6px;
}
.tag {
  display: inline-block;
  padding: 2px 8px;
  background: #e8f7ff;
  color: #00aeec;
  border-radius: 10px;
  font-size: 12px;
  margin-bottom: 6px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.hl {
  background: #fff3bf;
  color: inherit;
  padding: 0 2px;
  border-radius: 4px;
}
.empty-block {
  padding: 32px 8px 12px;
  text-align: center;
}
.empty-title {
  font-size: 18px;
  margin: 0 0 8px;
}
.rec-wrap {
  margin-top: 28px;
  text-align: left;
}
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  margin-top: 14px;
}
.rec-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  text-align: left;
  padding: 0;
}
.rec-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
}
.rec-meta {
  padding: 8px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rec-name {
  font-size: 14px;
  font-weight: 600;
}
.rec-sub {
  font-size: 12px;
  color: #909399;
}
.foot {
  margin-top: auto;
}
</style>
