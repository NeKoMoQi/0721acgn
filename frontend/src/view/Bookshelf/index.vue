<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10; flex-shrink: 0" />
    <div class="bookshelf-main">
      <div class="background-layer" />
      <div class="acgn_container">
      <h1 class="page-title">历史与收藏</h1>
      <p class="hint">
        浏览会留下时间线；在详情页点「收藏」会在这里星标置顶。同一浏览器下的数据与你的匿名标识一起保存在服务端。
      </p>

      <div class="toolbar">
        <button type="button" class="seg" :class="{ on: tab === 'all' }" @click="tab = 'all'">全部足迹</button>
        <button type="button" class="seg" :class="{ on: tab === 'starred' }" @click="tab = 'starred'">
          我的收藏 ★
        </button>
      </div>

      <div v-if="!shown.length" class="empty">{{ emptyHint }}</div>

      <ul v-else class="timeline">
        <li v-for="it in shown" :key="it.type + '-' + it.id" class="row" @click="open(it)">
          <img v-if="it.pic" class="thumb" :src="it.pic" alt="" />
          <div class="mid">
            <span class="badge">{{ typeLabel(it.type) }}</span>
            <span v-if="it.starred" class="star">★ 已收藏</span>
            <div class="tit">{{ it.name }}</div>
            <div class="tip">上次访问 {{ fmt(it.at) }}</div>
            <div v-if="it.starred && it.starredAt" class="tip muted">收藏于 {{ fmt(it.starredAt) }}</div>
          </div>
          <div class="acts">
            <el-button size="small" text type="primary" @click.stop="toggleStar(it)">
              {{ it.starred ? '取消收藏' : '收藏' }}
            </el-button>
          </div>
        </li>
      </ul>
      </div>
    </div>
    <Behind class="page-footer" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  listHistory,
  ensureAcgnUserHydrated,
  toggleBookshelf,
} from '@/utils/acgnLocal'

const $router = useRouter()
const tab = ref('all')
const items = ref([])

const sorted = computed(() =>
  [...items.value].sort((a, b) => (b.at || 0) - (a.at || 0)),
)

const starredFirst = computed(() => {
  const s = [...items.value].sort((a, b) => {
    const ds = Number(!!b.starred) - Number(!!a.starred)
    if (ds !== 0) return ds
    return (b.at || 0) - (a.at || 0)
  })
  return s
})

const shown = computed(() => {
  if (tab.value === 'starred') {
    return starredFirst.value.filter((x) => x.starred)
  }
  return sorted.value
})

const emptyHint = computed(() =>
  tab.value === 'starred'
    ? '暂无星标收藏，在作品详情里点「收藏」添加。'
    : '暂无浏览记录，去翻翻番剧·漫画·轻小说简介页。',
)

async function refresh() {
  await ensureAcgnUserHydrated()
  items.value = listHistory()
}

onMounted(() => void refresh())

function typeLabel(t) {
  if (t === 'animation') return '番剧'
  if (t === 'comic') return '漫画'
  if (t === 'novel') return '轻小说'
  return t
}

function open(it) {
  if (it.type === 'animation') {
    $router.push({ name: 'Animationintroduce', query: { animationid: it.id } })
  } else if (it.type === 'comic') {
    $router.push({ name: 'ComicIntroduce', query: { comicid: it.id } })
  } else if (it.type === 'novel') {
    $router.push({ name: 'NovelIntroduce', query: { novelid: it.id } })
  }
}

function toggleStar(it) {
  toggleBookshelf({
    type: it.type,
    id: it.id,
    name: it.name,
    pic: it.pic,
  })
  setTimeout(() => void refresh(), 400)
}

function fmt(at) {
  try {
    return new Date(at).toLocaleString()
  } catch {
    return ''
  }
}
</script>

<style scoped>
.root-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.bookshelf-main {
  flex: 1 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.background-layer {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-45%);
  width: 100vw;
  height: 0;
  padding-top: 56.25%;
  background: url('/BCPHOTO/acgnbc4.png') no-repeat center/contain;
  z-index: -1;
}
.acgn_container {
  width: 62%;
  max-width: 880px;
  margin: 100px auto 40px;
  flex: 0 1 auto;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.page-title {
  margin-top: 0;
}
.hint {
  color: #666;
  font-size: 13px;
  line-height: 1.55;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin: 20px 0 16px;
}
.seg {
  border-radius: 999px;
  border: 1px solid #e1e8ed;
  padding: 8px 16px;
  background: #f8fafc;
  cursor: pointer;
}
.seg.on {
  background: rgba(251, 114, 153, 0.15);
  border-color: #fb7299;
  color: #c71f5f;
}
.empty {
  padding: 48px;
  text-align: center;
  color: #999;
}
.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
}
.row {
  display: flex;
  gap: 14px;
  padding: 14px;
  border: 1px solid #eef2f5;
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  align-items: stretch;
}
.row:hover {
  background: #f9fbfd;
}
.thumb {
  width: 62px;
  height: 86px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.mid {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.badge {
  font-size: 12px;
  color: #00aeec;
}
.star {
  font-size: 12px;
  color: #f39c12;
}
.tit {
  font-weight: 600;
  color: #2c3e50;
}
.tip {
  font-size: 12px;
  color: #7f8c8d;
}
.tip.muted {
  opacity: 0.85;
}
.acts {
  display: flex;
  align-items: center;
}
.page-footer {
  flex-shrink: 0;
  width: 100%;
  margin-top: auto;
}
</style>
