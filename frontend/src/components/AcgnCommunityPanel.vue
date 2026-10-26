<template>
  <section class="acgn-community-panel" v-if="mediaId">
    <h3 class="panel-title">互动 · 评价 · 推荐</h3>
    <p class="muted">
      点赞与不喜欢会计入作品互动；你可以在下方发表评论参与讨论。<span
        v-if="useLocalFallback"
        class="warn"
      >当前网络不稳，以下内容可能未及时同步。</span>
    </p>

    <div class="toolbar">
      <button
        type="button"
        class="chip"
        :class="{ active: engage.myVote === 'up' }"
        title="赞同"
        @click="vote('up')"
      >
        👍 赞 {{ engage.likes }}
      </button>
      <button
        type="button"
        class="chip"
        :class="{ active: engage.myVote === 'down' }"
        title="不喜欢"
        @click="vote('down')"
      >
        👎 {{ engage.dislikes }}
      </button>
      <button type="button" class="chip" :class="{ active: subActive }" @click="onSubToggle">
        {{ subActive ? '已订阅动态' : '订阅更新' }}
      </button>
      <button
        type="button"
        class="chip shelf"
        :class="{ active: shelfActive }"
        @click="emit('toggleShelf')"
      >
        {{ shelfActive ? '★ 已收藏' : '收藏' }}
      </button>
      <router-link class="chip link" :to="{ name: 'Bookshelf' }">历史记录</router-link>
    </div>

    <div class="review-block">
      <div class="review-input-row">
        <div class="review-author">
          <img v-if="currentUserAvatar" :src="currentUserAvatar" class="review-avatar" alt="" />
          <span v-else class="review-avatar fallback">{{ currentUserNick.slice(0, 1) }}</span>
          <span class="review-author-name">{{ currentUserNick }}</span>
        </div>
        <el-input
          v-model="reviewDraft"
          type="textarea"
          :rows="2"
          maxlength="480"
          show-word-limit
          placeholder="写一条短评，和大家聊聊…"
        />
      </div>
      <el-button type="primary" size="small" round @click="submitReview">发表评论</el-button>

      <ul v-if="reviews.length" class="review-list">
        <li v-for="(r, i) in reviews" :key="i" class="review-item">
          <img v-if="r.avatar" :src="r.avatar" class="r-avatar" alt="" />
          <span v-else class="r-avatar fallback">{{ r.nick.slice(0, 1) }}</span>
          <span class="r-nick">{{ r.nick }}</span>
          <span class="r-time">{{ fmtTime(r.at) }}</span>
          <div class="r-text">{{ r.text }}</div>
        </li>
      </ul>
      <div v-else class="muted">暂无短评，来抢沙发。</div>
    </div>

    <div v-if="guessItems.length" class="guess-section">
      <h4 class="guess-title">猜你会喜欢</h4>
      <div class="guess-track">
        <button
          v-for="item in guessItems"
          :key="guessKey(item)"
          type="button"
          class="guess-card"
          @click="goGuess(item)"
        >
          <img v-if="guessPic(item)" :src="guessPic(item)" class="guess-cover" alt="" />
          <div class="guess-meta">
            <span class="g-name">{{ guessName(item) }}</span>
            <span class="g-sub">{{ genreLine }}</span>
          </div>
        </button>
      </div>
    </div>
    <div v-else class="muted">暂无推荐条目（可多选题材后在列表筛选）。</div>
  </section>
</template>

<script setup lang="ts">
import type { AcgnMediaType } from '@/utils/acgnLocal'
import {
  getEngagement,
  setVote,
  toggleSubscribeAsync,
  isSubscribed,
  listReviews,
  addReview,
  ensureAcgnUserHydrated,
} from '@/utils/acgnLocal'
import {
  fetchAcgnEngagement,
  postAcgnVote,
  fetchAcgnComments,
  postAcgnComment,
} from '@/api/acgnSocial'
import { appendMyPublishedComment } from '@/utils/acgnPersonalFeed'
import { getAnimation } from '@/api/animation'
import { getcartoon } from '@/api/cartoon'
import { getnovel } from '@/api/novel'
import useUserStore from '@/stores/modules/users'
import { useRouter } from 'vue-router'
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  mediaType: AcgnMediaType
  mediaId: number
  genre?: string
  title: string
  pic?: string
  shelfActive: boolean
}>()

const emit = defineEmits<{
  (e: 'toggleShelf'): void
}>()

const router = useRouter()
const userStore = useUserStore()

type UiRv = { nick: string; avatar?: string; text: string; at: number }

const engage = ref({ likes: 0, dislikes: 0, myVote: null as null | 'up' | 'down' })
const subActive = ref(false)
const reviews = ref<UiRv[]>([])
const reviewDraft = ref('')
const guessItems = ref<any[]>([])
const useLocalFallback = ref(false)

const currentUserNick = computed(() => userStore.username?.trim() || '访客')
const currentUserAvatar = computed(() => userStore.avatar?.trim() || '')

const genreLine = computed(() => {
  const g = props.genre?.split(/[,，]/)[0]?.trim()
  return g ? `题材 · ${g}` : '同题材 / 热门'
})

function fmtTime(t: number) {
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadServerSocial() {
  useLocalFallback.value = false
  await ensureAcgnUserHydrated()
  subActive.value = isSubscribed(props.mediaType, props.mediaId)
  try {
    const [en, cm] = await Promise.all([
      fetchAcgnEngagement(props.mediaType, props.mediaId),
      fetchAcgnComments(props.mediaType, props.mediaId),
    ])
    if (en?.code === 200 && en.data) {
      engage.value = {
        likes: Number(en.data.likes) || 0,
        dislikes: Number(en.data.dislikes) || 0,
        myVote: (en.data.myVote as 'up' | 'down' | null) ?? null,
      }
    }
    if (cm?.code === 200 && Array.isArray(cm.data)) {
      reviews.value = cm.data.map((c) => ({
        nick: c.nickname || '访客',
        text: c.body,
        avatar: c.avatar || '',
        at: c.at,
      }))
    }
  } catch {
    useLocalFallback.value = true
    engage.value = { ...getEngagement(props.mediaType, props.mediaId) }
    reviews.value = listReviews(props.mediaType, props.mediaId).map((x) => ({
      nick: x.nick,
      text: x.text,
      avatar: x.avatar,
      at: x.at,
    }))
  }
}

async function loadGuess() {
  const gid = props.mediaId
  const firstGenre = props.genre?.split(/[,，]/)[0]?.trim() || ''
  try {
    if (props.mediaType === 'animation') {
      const r = await getAnimation(1, {
        genre: firstGenre || undefined,
        sortBy: 'hot',
      })
      const rows = Array.isArray(r?.data) ? r.data : []
      guessItems.value = shuffle(
        rows.filter((x: any) => (x.id ?? x.animationid) !== gid),
      ).slice(0, 14)
      return
    }
    if (props.mediaType === 'comic') {
      const r = await getcartoon(1, {
        genre: firstGenre || undefined,
        sortBy: 'hot',
      })
      const rows = Array.isArray(r?.data) ? r.data : []
      guessItems.value = shuffle(
        rows.filter((x: any) => (x.comicid ?? x.id) !== gid),
      ).slice(0, 14)
      return
    }
    const r = await getnovel(1, {
      genre: firstGenre || undefined,
      sortBy: 'hot',
    })
    const rows = Array.isArray(r?.data) ? r.data : []
    guessItems.value = shuffle(
      rows.filter((x: any) => (x.novelid ?? x.id) !== gid),
    ).slice(0, 14)
  } catch {
    guessItems.value = []
  }
}

function shuffle<T>(arr: T[]): T[] {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

watch(
  () => [props.mediaType, props.mediaId, props.genre] as const,
  async () => {
    await Promise.all([loadGuess(), loadServerSocial()])
  },
  { immediate: true },
)

async function vote(v: 'up' | 'down') {
  try {
    const res = await postAcgnVote(props.mediaType, props.mediaId, v)
    if (res?.code === 200 && res.data) {
      engage.value = {
        likes: Number(res.data.likes) || 0,
        dislikes: Number(res.data.dislikes) || 0,
        myVote: (res.data.myVote as 'up' | 'down' | null) ?? null,
      }
      useLocalFallback.value = false
      return
    }
  } catch {
    //
  }
  useLocalFallback.value = true
  engage.value = { ...setVote(props.mediaType, props.mediaId, v) }
}

async function onSubToggle() {
  const ok = await toggleSubscribeAsync({
    type: props.mediaType,
    id: props.mediaId,
    name: props.title,
    pic: props.pic,
  })
  subActive.value = ok
  ElMessage.success(ok ? '已开启本作动态提醒' : '已关闭动态提醒')
}

async function submitReview() {
  const body = reviewDraft.value.trim()
  if (!body) {
    ElMessage.warning('内容不能为空')
    return
  }
  try {
    const res = await postAcgnComment(
      props.mediaType,
      props.mediaId,
      currentUserNick.value,
      currentUserAvatar.value,
      body,
    )
    if (res?.code === 200) {
      reviewDraft.value = ''
      appendMyPublishedComment({
        type: props.mediaType,
        mediaId: props.mediaId,
        workName: props.title,
        text: body,
        channel: 'server',
      })
      await loadServerSocial()
      ElMessage.success('评论已发送')
      return
    }
    ElMessage.warning(res?.msg || '发表评论失败')
  } catch {
    //
  }
  useLocalFallback.value = true
  if (addReview(props.mediaType, props.mediaId, body, currentUserNick.value, props.title, currentUserAvatar.value)) {
    reviews.value = listReviews(props.mediaType, props.mediaId).map((x) => ({
      nick: x.nick,
      text: x.text,
      avatar: x.avatar,
      at: x.at,
    }))
    ElMessage.warning('网络暂时不可用，评论未写入服务器（刷新后可能丢失）')
    return
  }
  ElMessage.warning('发表评论失败')
}

function guessPic(item: any) {
  if (props.mediaType === 'animation') return item?.bcimg
  return item?.picurl
}

function guessName(item: any) {
  if (props.mediaType === 'animation') return item?.name
  return item?.name
}

function guessKey(item: any) {
  if (props.mediaType === 'animation') return `a-${item.id}`
  if (props.mediaType === 'comic') return `c-${item.comicid}`
  return `n-${item.novelid ?? item.id}`
}

function goGuess(item: any) {
  if (props.mediaType === 'animation') {
    router.push({ name: 'Animationintroduce', query: { animationid: item.id } })
  } else if (props.mediaType === 'comic') {
    router.push({ name: 'ComicIntroduce', query: { comicid: item.comicid } })
  } else {
    router.push({
      name: 'NovelIntroduce',
      query: { novelid: item.novelid ?? item.id },
    })
  }
}
</script>

<style scoped>
.acgn-community-panel {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  margin: 24px 0 32px;
  padding: 24px;
  border: none;
  border-radius: 0;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
}
.panel-title {
  margin: 0 0 6px;
  font-size: 1.2rem;
  color: #1f2f3d;
}
.muted {
  margin: 0 0 16px;
  font-size: 12px;
  color: #8492a6;
  line-height: 1.5;
}
.warn {
  color: #e67e22;
  margin-left: 4px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 22px;
}
.chip {
  border: none;
  background: #eef2f6;
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  color: #334455;
}
.chip:hover {
  background: rgba(251, 114, 153, 0.12);
}
.chip.active {
  background: rgba(251, 114, 153, 0.2);
  color: #c71f5f;
}
.chip.shelf.active {
  background: rgba(52, 152, 219, 0.18);
  color: #217dbb;
}
.chip.link {
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.review-block {
  border: none;
  padding-top: 20px;
  margin-top: 4px;
}
.review-input-row {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
  width: 100%;
}
.review-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.review-avatar,
.r-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex: 0 0 auto;
  vertical-align: middle;
}
.review-avatar.fallback,
.r-avatar.fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef2f6;
  color: #fb7299;
}
.review-input-row :deep(.el-textarea),
.review-input-row :deep(.el-textarea__inner) {
  width: 100%;
}
.review-list {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
  max-height: 320px;
  overflow-y: auto;
}
.review-item {
  padding: 12px 0;
  border: none;
  border-bottom: 1px solid #f1f5f9;
}
.r-avatar {
  margin-right: 8px;
}
.r-nick {
  font-weight: 600;
  color: #fb7299;
  margin-right: 8px;
  font-size: 13px;
}
.r-time {
  font-size: 11px;
  color: #94a3b8;
}
.r-text {
  margin-top: 6px;
  font-size: 14px;
  color: #334155;
  line-height: 1.55;
  white-space: pre-wrap;
}

.guess-section {
  margin-top: 28px;
  border: none;
  padding-top: 20px;
}
.guess-title {
  margin: 0 0 12px;
  font-size: 1rem;
  color: #1f2f3d;
}
.guess-track {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 6px;
  scrollbar-width: thin;
}
.guess-card {
  flex: 0 0 120px;
  border: none;
  border-radius: 8px;
  overflow: hidden;
  background: #f4f7fa;
  cursor: pointer;
  text-align: left;
  padding: 0;
  transition: transform 0.15s, opacity 0.15s;
}
.guess-card:hover {
  transform: translateY(-1px);
  opacity: 0.95;
}
.guess-cover {
  width: 100%;
  height: 86px;
  object-fit: cover;
  display: block;
  background: #eee;
}
.guess-meta {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.g-name {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.g-sub {
  font-size: 10px;
  color: #94a3b8;
}
</style>
