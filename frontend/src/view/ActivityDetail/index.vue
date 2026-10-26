<template>
  <div class="activity-root">
    <Headpage style="position: sticky !important; top: 0; z-index: 10" />
    <main>
      <RouterLink class="back" to="/">← 回到首页</RouterLink>
      <div v-if="loading" class="notice" role="status">正在读取…</div>
      <div v-else-if="error" class="notice" role="alert">
        <p>{{ error }}</p><RouterLink to="/">回首页看看别的</RouterLink>
      </div>
      <article v-else-if="activity" class="card" data-activity>
        <div class="cover" v-if="activity.activityimg">
          <img :src="activity.activityimg" :alt="activity.activityname" />
        </div>
        <div class="copy">
          <span class="type">站内活动 / 作品推荐</span>
          <h1>{{ activity.activityname }}</h1>
          <p class="meta">
            <span v-if="dateLabel">{{ dateLabel }}</span>
            <span v-if="activity.createname"> · {{ activity.createname }}</span>
            <span v-if="activity.click !== undefined"> · {{ activity.click }} 次浏览</span>
          </p>
          <p v-for="(paragraph, index) in paragraphs" :key="index" class="body">{{ paragraph }}</p>
        </div>
      </article>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import request from '@/utils/request'

interface Activity {
  id: number
  activityname: string
  activityimg?: string
  activitydescribe?: string
  createtime?: string
  createname?: string
  click?: number
}

const route = useRoute()
const activity = ref<Activity | null>(null)
const loading = ref(false)
const error = ref('')

const paragraphs = computed(() =>
  (activity.value?.activitydescribe || '')
    .split(/\\n|\n{2,}/)
    .map((text) => text.trim())
    .filter(Boolean),
)
const dateLabel = computed(() => {
  const raw = activity.value?.createtime || ''
  const time = Date.parse(raw)
  return Number.isNaN(time) ? raw : new Date(time).toLocaleDateString('zh-CN')
})

async function load() {
  const id = String(route.params.id || '')
  if (!/^\d+$/.test(id)) {
    error.value = '活动编号不对。'
    return
  }
  loading.value = true
  error.value = ''
  activity.value = null
  try {
    const result: any = await request.get('/activity/byid', { params: { id } })
    if (result?.code !== 200 || !result.data) throw new Error('empty')
    activity.value = result.data
  } catch {
    error.value = '这个活动暂时读不到。'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, () => { void load() }, { immediate: true })
</script>

<style scoped>
.activity-root {
  min-height: 100vh;
  background: #f5f7f5;
  color: #213a33;
}
main {
  max-width: 900px;
  margin: 0 auto;
  padding: 48px 24px 80px;
}
.back {
  display: inline-block;
  margin-bottom: 24px;
  color: #477b67;
  text-decoration: none;
}
.notice {
  background: #fff;
  padding: 48px;
  border-radius: 16px;
  text-align: center;
  color: #70817c;
}
.notice a {
  color: #328566;
}
.card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 8px 28px #203c3214;
}
.cover img {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  display: block;
}
.copy {
  padding: 32px 36px 40px;
}
.type {
  font-size: 12px;
  letter-spacing: .08em;
  color: #658078;
}
.copy h1 {
  font-size: clamp(24px, 3vw, 34px);
  margin: 12px 0 14px;
}
.meta {
  color: #8a9a93;
  font-size: 13px;
  margin: 0 0 24px;
}
.body {
  font-size: 16px;
  line-height: 2;
  color: #3d5349;
  text-align: justify;
  margin: 0 0 16px;
}
</style>
