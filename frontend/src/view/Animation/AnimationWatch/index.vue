<template>
  <div class="watch-root" :class="{ 'lights-off': lightsOff }">
    <Headpage class="header" />
    <div class="watch-bg"></div>
    <button v-if="lightsOff" type="button" class="light-exit" @click="lightsOff = false">
      退出熄灯
    </button>

    <main class="watch-wrap" v-if="watchState === 'ready'">
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
            <div v-if="playbackFailed" class="playback-error" role="alert">
              <p>视频加载失败，请重试。</p>
              <button type="button" class="tool-button" @click="playbackFailed = false">重新加载视频</button>
            </div>
            <video
              v-else
              class="watch-video"
              controls
              autoplay
              playsinline
              preload="metadata"
              :src="currentEpisode.videoUrl"
              @error="playbackFailed = true"
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

          <AcgnCommunityPanel
            media-type="animation"
            :media-id="animationIdNum"
            :genre="animation.genre"
            :title="animation.name"
            :pic="animation.bcimg"
            :shelf-active="favoriteActive"
            @toggle-shelf="toggleFavorite"
          />
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
        <h1 v-if="watchState === 'loading'" role="status">正在加载番剧…</h1>
        <template v-else-if="watchState === 'error'">
          <h1 role="alert">番剧加载失败</h1>
          <button type="button" class="tool-button" @click="loadPage">重新加载</button>
        </template>
        <h1 v-else>{{ emptyMessage }}</h1>
        <router-link v-if="animationIdNum" class="back-link" :to="{ name: 'Animationintroduce', query: { animationid: animationIdNum } }">
          返回详情
        </router-link>
      </section>
    </main>

    <MediaResource v-if="animationIdNum" class="reader-report" type="animation" :id="animationIdNum" :position="reportPosition" :report-only="!playbackFailed && watchState !== 'error'" :unavailable="playbackFailed || watchState === 'error'" />
      <Behind class="footer" />
  </div>
</template>

<script setup lang="ts">
import MediaResource from '@/components/MediaResource.vue'
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getAnimationById, getAnimationEpisodeById, getAnimationEpisodes } from '@/api/animation';
import { ensureAcgnUserHydrated, isInBookshelf, pushHistory, saveAnimationEpisode, toggleBookshelfAsync } from '@/utils/acgnLocal';
import AcgnCommunityPanel from '@/components/AcgnCommunityPanel.vue';

const route = useRoute();
const router = useRouter();
const animation = ref<any>(null);
const currentEpisode = ref<any>(null);
const episodes = ref<any[]>([]);
const watchState = ref<'loading' | 'error' | 'empty' | 'ready'>('loading');
const emptyMessage = ref('暂无视频资源');
const playbackFailed = ref(false);
let disposed = false;
const lightsOff = ref(false);
const autoNext = ref(true);
const favoriteActive = ref(false);
const favoritePending = ref(false);

// App 按完整路由重建观看页，保留本次请求对应的分集。
const requestedAnimation = Number(route.query.animationid);
const animationIdNum = Number.isInteger(requestedAnimation) && requestedAnimation > 0 ? requestedAnimation : 0;
const episodeId = Number(route.query.episodeid);
const resumeEpisode = Number(route.query.episode);
const reportPosition = computed(() => {
  const number = Number(currentEpisode.value?.episodeNo || route.query.episode);
  return number > 0 ? `第 ${number} 集` : episodeId > 0 ? `分集编号 ${episodeId}` : '';
});
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
  void loadPage();
  window.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => {
  disposed = true;
  window.removeEventListener('keydown', handleKeydown);
});
async function loadPage() {
  watchState.value = 'loading';
  playbackFailed.value = false;
  if (!animationIdNum) {
    emptyMessage.value = '请选择一部番剧';
    watchState.value = 'empty';
    return;
  }

  try {
    const [work, listing, selected] = await Promise.all([
      getAnimationById(animationIdNum),
      getAnimationEpisodes(animationIdNum),
      episodeId ? getAnimationEpisodeById(episodeId) : Promise.resolve(null),
    ]);
    if (disposed) return;
    if (work?.code !== 200 || listing?.code !== 200 || !Array.isArray(listing.data) ||
        (episodeId && selected?.code !== 200)) {
      throw new Error('番剧资源请求失败');
    }

    animation.value = work.data;
    episodes.value = listing.data;
    const episode = episodeId
      ? selected?.data
      : resumeEpisode
        ? episodes.value.find(item => Number(item.episodeNo) === resumeEpisode)
        : episodes.value[0];
    if (!animation.value || typeof episode?.videoUrl !== 'string' || !episode.videoUrl.trim() ||
        (episode.animationId != null && Number(episode.animationId) !== animationIdNum)) {
      emptyMessage.value = episodes.value.length ? '所选分集暂无视频资源' : '暂无视频资源';
      watchState.value = 'empty';
      return;
    }

    currentEpisode.value = episode;
    watchState.value = 'ready';
    void loadViewerState();
  } catch {
    if (!disposed) watchState.value = 'error';
  }
}

async function loadViewerState() {
  try {
    await ensureAcgnUserHydrated();
    if (disposed) return;
    favoriteActive.value = isInBookshelf('animation', animationIdNum);
    pushHistory({
      type: 'animation',
      id: animationIdNum,
      name: animation.value.name,
      pic: animation.value.bcimg,
    });
  } catch {
    // 书架状态暂不可用时，播放器仍然可用。
  }
}

function episodeTitle(item: any) {
  return item?.title || `第 ${item?.episodeNo || ''} 集`;
}

async function toggleFavorite() {
  if (favoritePending.value || !animation.value || !animationIdNum) return;
  favoritePending.value = true;
  try {
    const state = await toggleBookshelfAsync({
      type: 'animation',
      id: animationIdNum,
      name: animation.value.name,
      pic: animation.value.bcimg,
    });
    if (state !== null) favoriteActive.value = state;
  } finally {
    favoritePending.value = false;
  }
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
    query: { animationid: animationIdNum, episodeid: item.id },
  });
}

function onEnded() {
  if (autoNext.value && nextEpisode.value) {
    navigateToEpisode(nextEpisode.value);
  }
}

function persistCurrentEpisode() {
  const animationId = animationIdNum;
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
.reader-report { max-width: 900px; margin: 20px auto 72px; padding-inline: 16px; }
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
.icon-toggle {
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
.icon-toggle:hover {
  border-color: #3498db;
  transform: translateY(-1px);
}
.tool-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  transform: none;
}
.tool-button.primary {
  background: #2f89c8;
  border-color: #2f89c8;
  color: #fff;
}
.icon-toggle.active {
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
.playback-error {
  padding: 24px;
  background: white;
}
.playback-error .tool-button,
.empty-state .tool-button {
  margin: 8px 12px 8px 0;
  min-height: 44px;
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

@media (max-width: 820px) {
  .watch-wrap { width: calc(100% - 16px); margin: 12px auto 24px; }
  .watch-video { min-height: 0; aspect-ratio: 16 / 9; }
  .watch-actions { flex-wrap: wrap; }
  .episode-link { min-height: 44px; }
}
</style>
