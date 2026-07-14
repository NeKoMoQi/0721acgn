<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10" />
    <div class="sheet">
      <h1 class="h1">我的动态</h1>
      <p class="muted">串联你的短评足迹、演示投稿与站内通知（节选）。</p>
      <div class="timeline">
        <div v-for="row in merged" :key="row.uid" class="evt" @click="row.act?.()">
          <div class="dot" />
          <div class="bubble">
            <div class="t">{{ row.title }}</div>
            <div class="b">{{ row.body }}</div>
            <div class="when">{{ fmtAt(row.at) }}</div>
          </div>
        </div>
        <div v-if="!merged.length" class="empty">暂时没有可见动态。</div>
      </div>
    </div>
    <Behind class="bottom" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import Headpage from '@/components/Headpage/index.vue'
import { useRouter } from 'vue-router'
import { ensureAcgnUserHydrated, listHistory, listSubscriptions, type AcgnMediaType } from '@/utils/acgnLocal'
import { listNotifications } from '@/utils/acgnNotifications'
import { listMyPublishedComments } from '@/utils/acgnPersonalFeed'
const router = useRouter()
onMounted(async () => {
  await ensureAcgnUserHydrated()
})

const merged = computed(() => {
  const acts: Array<{ uid: string; at: number; title: string; body: string; act?: () => void }> = []

  for (const n of listNotifications().slice(0, 40)) {
    acts.push({
      uid: `n-${n.id}`,
      at: n.at,
      title: n.title,
      body: `【${kind(n.kind)}】${n.body}`,
      act: () => router.push({ name: 'Messages' }),
    })
  }

  for (const r of listMyPublishedComments().slice(0, 40)) {
    acts.push({
      uid: `c-${r.type}-${r.mediaId}-${r.at}-${r.text.slice(0, 8)}`,
      at: r.at,
      title: `发表了评论 · ${r.workName}`,
      body: r.text,
      act: () => router.push(goRev(r)),
    })
  }

  for (const s of listSubscriptions()) {
    acts.push({
      uid: `s-${s.type}-${s.id}`,
      at: s.subscribedAt || 0,
      title: `追更订阅 · ${s.name}`,
      body: `${typ(s.type)} | 你已开启更新提醒占位`,
      act: () =>
        router.push({
          ...(s.type === 'animation'
            ? { name: 'Animationintroduce' as const, query: { animationid: String(s.id) } }
            : s.type === 'comic'
              ? { name: 'ComicIntroduce' as const, query: { comicid: String(s.id) } }
              : { name: 'NovelIntroduce' as const, query: { novelid: String(s.id) } }),
        }),
    })
  }

  for (const h of listHistory()
    .filter((x) => x.starred)
    .slice(0, 20)) {
    acts.push({
      uid: `f-${h.type}-${h.id}-${h.starredAt || h.at}`,
      at: h.starredAt || h.at,
      title: `收藏了 · ${h.name}`,
      body: `${typ(h.type)} | 从历史页可跳转继续阅读`,
      act: () => router.push(goMedia(h)),
    })
  }

  return acts.sort((a, b) => b.at - a.at).slice(0, 80)
})

function fmtAt(ms: number) {
  try {
    return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(ms)
  } catch {
    return ''
  }
}

function kind(k: string) {
  switch (k) {
    case 'subscribe_update':
      return '订阅'
    case 'comment_reply':
      return '回复'
    case 'review_result':
      return '投稿'
    default:
      return '系统'
  }
}

function typ(t: AcgnMediaType | string) {
  if (t === 'animation') return '动漫'
  if (t === 'comic') return '漫画'
  return '小说'
}

function goRev(r: ReturnType<typeof listMyPublishedComments>[0]) {
  if (r.type === 'animation')
    return { name: 'Animationintroduce' as const, query: { animationid: String(r.mediaId) } }
  if (r.type === 'comic')
    return { name: 'ComicIntroduce' as const, query: { comicid: String(r.mediaId) } }
  return { name: 'NovelIntroduce' as const, query: { novelid: String(r.mediaId) } }
}

function goMedia(h: { type: AcgnMediaType; id: number }) {
  if (h.type === 'animation')
    return { name: 'Animationintroduce' as const, query: { animationid: String(h.id) } }
  if (h.type === 'comic')
    return { name: 'ComicIntroduce' as const, query: { comicid: String(h.id) } }
  return { name: 'NovelIntroduce' as const, query: { novelid: String(h.id) } }
}
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fdf9fc;
}
.sheet {
  flex: 1;
  max-width: 720px;
  margin: 100px auto 40px;
  padding: 0 16px 40px;
}
.h1 {
  margin: 0 0 8px;
}
.muted {
  color: #909399;
  font-size: 14px;
  margin-bottom: 18px;
  line-height: 1.6;
}
.timeline {
  position: relative;
  padding-left: 18px;
  border-left: 2px solid #fce8ea;
}
.evt {
  position: relative;
  margin-bottom: 18px;
  cursor: pointer;
}
.dot {
  position: absolute;
  left: -24px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fb7299;
}
.bubble {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(251, 114, 153, 0.18);
}
.bubble:hover {
  border-color: #fb7299;
}
.t {
  font-weight: 700;
}
.b {
  margin-top: 6px;
  color: #303133;
  font-size: 14px;
  line-height: 1.5;
}
.when {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.empty {
  color: #909399;
}
.bottom {
  margin-top: auto;
}
</style>
