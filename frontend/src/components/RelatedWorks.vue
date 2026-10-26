<template>
  <div v-if="items.length" class="related-block">
    <h2 class="title">同 IP / 关联作品</h2>
    <p class="subtitle">Bangumi 边、题名完全相同，以及<strong>同一主标题 IP</strong>：凡题名以该主名为前缀（多季、配音版、剧场版副标题等），且衔接处合理，即视为同系列作品。</p>
    <div class="cards">
      <div v-for="(it, idx) in items" :key="idx" class="card" @click="go(it)">
        <img v-if="it.picurl" :src="it.picurl" class="cover" alt="" />
        <div class="meta">
          <span class="tag">{{ labelType(it.type) }}</span>
          <span class="name">{{ it.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRelated } from '@/api/related'

const props = defineProps({
  srcType: { type: String, required: true },
  srcId: { type: [Number, String], required: true },
})

const items = ref([])
const $router = useRouter()

function labelType(t) {
  if (t === 'animation') return '番剧'
  if (t === 'comic') return '漫画'
  if (t === 'novel') return '轻小说'
  return t
}

function go(it) {
  const id = it.id
  if (it.type === 'animation') {
    $router.push({ name: 'Animationintroduce', query: { animationid: id } })
  } else if (it.type === 'comic') {
    $router.push({ name: 'ComicIntroduce', query: { comicid: id } })
  } else if (it.type === 'novel') {
    $router.push({ name: 'NovelIntroduce', query: { novelid: id } })
  }
}

async function load() {
  const sid = Number(props.srcId)
  if (!sid || !props.srcType) {
    items.value = []
    return
  }
  try {
    const res = await getRelated(props.srcType, sid)
    items.value = Array.isArray(res.data) ? res.data : []
  } catch {
    items.value = []
  }
}

watch(
  () => [props.srcType, props.srcId],
  () => load(),
  { immediate: true },
)
</script>

<style scoped>
.related-block {
  margin-top: 28px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 12px;
  border: 1px solid #e9ecef;
  position: relative;
  z-index: 1;
  clear: both;
}
.title {
  margin: 0 0 8px;
  font-size: 1.2rem;
  color: #2c3e50;
}
.subtitle {
  margin: 0 0 16px;
  font-size: 12px;
  line-height: 1.45;
  color: #6c757d;
}
.cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.card {
  width: 140px;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease;
}
.card:hover {
  transform: translateY(-3px);
}
.cover {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
  background: #f0f0f0;
}
.meta {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tag {
  font-size: 11px;
  color: #00aeec;
}
.name {
  font-size: 13px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
