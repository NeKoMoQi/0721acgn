<template>
  <div class="watch-root" :class="{ 'lights-off': lightsOff }">
    <Headpage class="header" />
    <div class="watch-bg"></div>
    <button v-if="lightsOff" type="button" class="light-exit" @click="lightsOff = false">
      退出熄灯
    </button>

    <main class="watch-wrap" v-if="animation && currentEpisode">
      <section class="watch-content">
        <section class="player-shell">
          <div class="title-bar">
            <div class="title-copy">
              <router-link
                class="back-link"
                :to="{ name: 'Animationintroduce', query: { animationid: animationIdNum } }"
              >
                返回详情
              </router-link>
              <h1 class="watch-title">{{ animation.name }}</h1>
              <p class="watch-subtitle">{{ episodeTitle(currentEpisode) }}</p>
            </div>

            <div class="watch-actions">
              <button type="button" class="tool-button" @click="goPrev" :disabled="!prevEpisode">上一集</button>
              <button type="button" class="tool-button primary" @click="goNext" :disabled="!nextEpisode">下一集</button>
              <button type="button" class="icon-toggle" :class="{ active: autoNext }" @click="autoNext = !autoNext">自动连播</button>
              <button type="button" class="icon-toggle" :class="{ active: lightsOff }" @click="lightsOff = !lightsOff">熄灯</button>
            </div>
          </div>

          <div class="video-frame">
            <video
              ref="videoRef"
              class="watch-video"
              controls
              autoplay
              playsinline
              preload="metadata"
              :src="currentEpisode.videoUrl"
              @loadedmetadata="onLoadedMetadata"
              @ended="onEnded"
              @play="persistCurrentEpisode"
            />
          </div>
        </section>

        <section class="detail-shell">
          <div class="work-summary">
            <img class="summary-cover" :src="animation.bcimg" alt="番剧封面">
            <div class="summary-copy">
              <div class="summary-head">
                <h2>{{ animation.name }}</h2>
                <span>{{ animation.rating || '暂无' }} 分</span>
              </div>
              <p>{{ animation.longcomment || animation.shortcomment || '暂无简介。' }}</p>
              <div class="meta-row">
                <span>{{ animation.releasedate }}</span>
                <span>{{ animation.countpage }}</span>
                <span>{{ animation.status === '1' ? '已上线' : '连载中' }}</span>
              </div>
            </div>
          </div>

          <div class="interaction-bar">
            <button type="button" :class="{ active: likeActive }" @click="toggleLike">
              <strong>👍</strong>
              <span>点赞 {{ likeCount }}</span>
            </button>
            <button type="button" :class="{ active: coinActive }" @click="toggleCoin">
              <strong>◎</strong>
              <span>投币 {{ coinCount }}</span>
            </button>
            <button type="button" :class="{ active: favoriteActive }" @click="favoriteActive = !favoriteActive">
              <strong>☆</strong>
              <span>{{ favoriteActive ? '已收藏' : '收藏' }}</span>
            </button>
            <button type="button" @click="scrollToComments">
              <strong>💬</strong>
              <span>评论 {{ comments.length }}</span>
            </button>
          </div>

          <div ref="commentSection" class="comment-section">
            <div class="comment-head">
              <h2>评论区</h2>
              <span>{{ comments.length }} 条短评</span>
            </div>
            <textarea
              v-model="commentText"
              class="comment-input"
              maxlength="480"
              placeholder="写一条短评，和大家聊聊这一集..."
            />
            <div class="comment-tools">
              <span>{{ commentText.length }} / 480</span>
              <button type="button" class="comment-submit" :disabled="!commentText.trim()" @click="submitComment">
                发布
              </button>
            </div>
            <div v-if="comments.length" class="comment-list">
              <div v-for="item in comments" :key="item.id" class="comment-item">
                <div class="avatar">{{ item.author.slice(0, 1) }}</div>
                <div>
                  <div class="comment-meta">
                    <strong>{{ item.author }}</strong>
                    <span>{{ item.time }}</span>
                  </div>
                  <p>{{ item.text }}</p>
                </div>
              </div>
            </div>
            <div v-else class="comment-empty">暂无短评，来抢沙发。</div>
          </div>
        </section>
      </section>

      <aside class="episode-panel">
        <div class="panel-head">
          <h2 class="panel-title">章节列表</h2>
          <span class="episode-count">{{ episodes.length }} 集</span>
        </div>

        <div class="episode-list">
          <router-link
            v-for="item in episodes"
            :key="item.id"
            class="episode-link"
            :class="{ active: Number(item.id) === Number(currentEpisode.id) }"
            :to="{ name: 'AnimationWatch', query: { animationid: animationIdNum, episodeid: item.id } }"
          >
            <span>第 {{ item.episodeNo }} 集</span>
            <small>{{ Number(item.id) === Number(currentEpisode.id) ? '播放中' : '观看' }}</small>
          </router-link>
        </div>
      </aside>
    </main>

    <main v-else class="watch-wrap">
      <section class="empty-state">
        <h1>暂无视频资源</h1>
        <router-link class="back-link" :to="{ name: 'Animationintroduce', query: { animationid: animationIdNum } }">
          返回详情
        </router-link>
      </section>
    </main>

    <Behind class="footer" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getAnimationById, getAnimationEpisodeById, getAnimationEpisodes } from '@/api/animation';
import { ensureAcgnUserHydrated, pushHistory, saveAnimationEpisode } from '@/utils/acgnLocal';

const route = useRoute();
const router = useRouter();
const animation = ref<any>(null);
const currentEpisode = ref<any>(null);
const episodes = ref<any[]>([]);
const videoRef = ref<HTMLVideoElement | null>(null);
const commentSection = ref<HTMLElement | null>(null);
const lightsOff = ref(false);
const autoNext = ref(true);
const duration = ref(0);
const favoriteActive = ref(false);
const likeActive = ref(false);
const coinActive = ref(false);
const likeCount = ref(1280);
const coinCount = ref(233);
const commentText = ref('');
const comments = ref<Array<{ id: number; author: string; text: string; time: string }>>([]);

const animationIdNum = computed(() => Number(route.query.animationid));
const episodeIdNum = computed(() => Number(route.query.episodeid));
const currentIndex = computed(() =>
  episodes.value.findIndex((item) => Number(item.id) === Number(currentEpisode.value?.id)),
);
const prevEpisode = computed(() => currentIndex.value > 0 ? episodes.value[currentIndex.value - 1] : null);
const nextEpisode = computed(() =>
  currentIndex.value >= 0 && currentIndex.value < episodes.value.length - 1
    ? episodes.value[currentIndex.value + 1]
    : null,
);

onMounted(() => {
  loadPage();
  window.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});
watch(() => route.query.episodeid, loadEpisode);

async function loadPage() {
  const animationId = animationIdNum.value;
  if (!animationId) return;
  const [animationRes, episodeRes] = await Promise.all([
    getAnimationById(animationId),
    getAnimationEpisodes(animationId),
  ]);
  animation.value = animationRes?.data || null;
  episodes.value = Array.isArray(episodeRes?.data) ? episodeRes.data : [];
  await loadEpisode();
  await ensureAcgnUserHydrated();
  if (animation.value) {
    pushHistory({
      type: 'animation',
      id: animationId,
      name: animation.value.name,
      pic: animation.value.bcimg,
    });
  }
}

async function loadEpisode() {
  duration.value = 0;
  const episodeId = episodeIdNum.value;
  if (!episodeId) {
    currentEpisode.value = episodes.value[0] || null;
    return;
  }
  const res = await getAnimationEpisodeById(episodeId);
  currentEpisode.value = res?.data || null;
  await nextTick();
  persistCurrentEpisode();
}

function episodeTitle(item: any) {
  return item?.title || `第 ${item?.episodeNo || ''} 集`;
}

function toggleLike() {
  likeActive.value = !likeActive.value;
  likeCount.value += likeActive.value ? 1 : -1;
}

function toggleCoin() {
  coinActive.value = !coinActive.value;
  coinCount.value += coinActive.value ? 1 : -1;
}

function submitComment() {
  const text = commentText.value.trim();
  if (!text) return;
  comments.value.unshift({
    id: Date.now(),
    author: '游客',
    text,
    time: '刚刚',
  });
  commentText.value = '';
}

function scrollToComments() {
  commentSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function goPrev() {
  if (prevEpisode.value) navigateToEpisode(prevEpisode.value);
}

function goNext() {
  if (nextEpisode.value) navigateToEpisode(nextEpisode.value);
}

function navigateToEpisode(item: any) {
  router.push({
    name: 'AnimationWatch',
    query: { animationid: animationIdNum.value, episodeid: item.id },
  });
}

function onLoadedMetadata() {
  duration.value = videoRef.value?.duration || 0;
}

function onEnded() {
  if (autoNext.value && nextEpisode.value) {
    navigateToEpisode(nextEpisode.value);
  }
}

function persistCurrentEpisode() {
  const animationId = animationIdNum.value;
  const episodeNo = Number(currentEpisode.value?.episodeNo);
  if (animationId && episodeNo) {
    saveAnimationEpisode(animationId, episodeNo);
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) return;
  if (event.key === 'ArrowLeft') goPrev();
  if (event.key === 'ArrowRight') goNext();
  if (event.key.toLowerCase() === 'l') lightsOff.value = !lightsOff.value;
}

</script>

<style scoped>
.watch-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #263747;
}
.header {
  position: sticky;
  top: 0;
  z-index: 1000;
}
.watch-bg {
  position: fixed;
  inset: 0;
  background:
    linear-gradient(rgba(15, 18, 22, 0.42), rgba(15, 18, 22, 0.2)),
    url('/BCPHOTO/bc9.jpg');
  background-size: cover;
  background-attachment: fixed;
  z-index: -2;
}
.watch-root.lights-off::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 10;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.72);
}
.watch-root.lights-off .player-shell {
  position: relative;
  z-index: 20;
}
.light-exit {
  position: fixed;
  top: 86px;
  right: 22px;
  z-index: 30;
  padding: 9px 14px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  background: rgba(20, 24, 28, 0.88);
  color: #fff;
  cursor: pointer;
}
.watch-wrap {
  flex: 1;
  width: min(1320px, calc(100% - 40px));
  margin: 84px auto 40px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}
.watch-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.player-shell,
.episode-panel,
.detail-shell,
.empty-state {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: 12px;
  box-shadow: 0 14px 42px rgba(0, 0, 0, 0.18);
}
.player-shell,
.detail-shell {
  padding: 20px;
}
.title-bar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.title-copy {
  min-width: 0;
}
.watch-title {
  margin: 8px 0 6px;
  font-size: 26px;
  line-height: 1.25;
  color: #22313f;
}
.watch-subtitle {
  margin: 0;
  color: #647789;
  font-size: 15px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 7px;
  background: #eef6fc;
  color: #2d7fb8;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
}
.watch-actions {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  flex-wrap: wrap;
}
.tool-button,
.icon-toggle,
.interaction-bar button,
.comment-submit {
  min-height: 36px;
  padding: 0 13px;
  border: 1px solid #d5e2ec;
  border-radius: 8px;
  background: #fff;
  color: #2c3e50;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease;
}
.tool-button:hover,
.icon-toggle:hover,
.interaction-bar button:hover,
.comment-submit:hover {
  border-color: #3498db;
  transform: translateY(-1px);
}
.tool-button:disabled,
.comment-submit:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  transform: none;
}
.tool-button.primary,
.comment-submit {
  background: #2f89c8;
  border-color: #2f89c8;
  color: #fff;
}
.icon-toggle.active,
.interaction-bar button.active {
  background: #e9f6f0;
  border-color: #42a676;
  color: #267553;
}
.video-frame {
  overflow: hidden;
  border-radius: 12px;
  background: #000;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}
.watch-video {
  display: block;
  width: 100%;
  max-height: calc(100vh - 250px);
  min-height: 430px;
  background: #000;
}
.work-summary {
  display: flex;
  gap: 16px;
}
.summary-cover {
  width: 96px;
  height: 132px;
  border-radius: 8px;
  object-fit: cover;
  background: #e8eef3;
}
.summary-copy {
  flex: 1;
  min-width: 0;
}
.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.summary-head h2 {
  margin: 0;
  font-size: 21px;
  color: #22313f;
}
.summary-head span {
  color: #2f89c8;
  font-weight: 800;
}
.summary-copy p {
  margin: 10px 0 12px;
  color: #526577;
  line-height: 1.7;
  font-size: 14px;
}
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.meta-row span {
  padding: 5px 9px;
  border-radius: 999px;
  background: #eef6fc;
  color: #52728d;
  font-size: 12px;
}
.interaction-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
  padding-top: 16px;
  border-top: 1px solid #e4edf4;
}
.interaction-bar button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.comment-section {
  padding-top: 16px;
  border-top: 1px solid #e4edf4;
}
.comment-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.comment-head h2 {
  margin: 0;
  font-size: 20px;
  color: #22313f;
}
.comment-head span,
.comment-tools span {
  color: #7b8a97;
  font-size: 13px;
}
.comment-input {
  width: 100%;
  min-height: 86px;
  padding: 12px;
  resize: vertical;
  border: 1px solid #dbe6ee;
  border-radius: 9px;
  outline: none;
  color: #263747;
  font-size: 14px;
  line-height: 1.6;
  background: #fbfdff;
}
.comment-input:focus {
  border-color: #3498db;
}
.comment-tools {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 10px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}
.comment-item {
  display: flex;
  gap: 10px;
}
.avatar {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #eaf5fd;
  color: #2f89c8;
  font-weight: 800;
}
.comment-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #7b8a97;
  font-size: 12px;
}
.comment-meta strong {
  color: #263747;
  font-size: 14px;
}
.comment-item p {
  margin: 4px 0 0;
  line-height: 1.7;
  color: #405466;
}
.comment-empty {
  margin-top: 14px;
  padding: 18px;
  border-radius: 9px;
  background: #f7fafc;
  color: #7b8a97;
  text-align: center;
}
.episode-panel {
  padding: 14px;
  align-self: start;
  max-height: calc(100vh - 112px);
  overflow: auto;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #dce8f2;
}
.panel-title {
  margin: 0;
  font-size: 17px;
  color: #263747;
}
.episode-count {
  color: #6d7e8c;
  font-size: 13px;
}
.episode-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.episode-link {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 38px;
  padding: 0 10px;
  border-radius: 7px;
  border: 1px solid #dce8f2;
  color: #2c3e50;
  text-decoration: none;
  background: #f8fbfd;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}
.episode-link:hover {
  border-color: #3498db;
  box-shadow: 0 6px 16px rgba(52, 152, 219, 0.14);
}
.episode-link.active {
  border-color: #2f89c8;
  background: #eaf5fd;
}
.episode-link span {
  font-weight: 800;
  font-size: 13px;
}
.episode-link small {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 999px;
  background: #eef6fc;
  color: #5f7384;
  font-size: 11px;
}
.episode-link.active small {
  background: #2f89c8;
  color: #fff;
}
.empty-state {
  grid-column: 1 / -1;
  padding: 32px;
  color: #6b7b88;
}
.empty-state h1 {
  margin: 0 0 18px;
  color: #263747;
  font-size: 24px;
}
.footer {
  margin-top: auto;
}
@media (max-width: 980px) {
  .watch-wrap {
    grid-template-columns: 1fr;
    width: min(100% - 28px, 760px);
  }
  .title-bar,
  .work-summary {
    flex-direction: column;
  }
  .watch-actions {
    justify-content: flex-start;
  }
  .episode-panel {
    max-height: none;
  }
  .watch-video {
    min-height: 260px;
  }
}
</style>
