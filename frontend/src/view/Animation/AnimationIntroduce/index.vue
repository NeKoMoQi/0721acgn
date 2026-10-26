<template>
  <div class="root-container">
    <Headpage class="header" />

    <div class="background-layer"></div>

    <div class="content-wrapper" v-if="noveldata">
      <div class="info-container">
        <div class="cover-wrapper">
          <img :src="noveldata.bcimg" class="cover-image" alt="番剧封面">
        </div>

        <div class="detail-wrapper">
          <h1 class="novel-title">{{ noveldata.name }}</h1>

          <div class="status-container">
            <div class="status-item">
              <span class="status-label">当前进度</span>
              <span class="status-value">{{ noveldata.countpage }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">作品评分</span>
              <span class="status-value">{{ noveldata.rating }}分</span>
            </div>
            <div class="status-item">
              <span class="status-label">开播时间</span>
              <span class="status-value">{{ noveldata.releasedate }}</span>
            </div>
          </div>

          <p class="description-text">{{ noveldata.longcomment }}</p>

          <div class="button-group">
            <button type="button" class="main-button" @click="toTop">
              <span>▶</span> 观看进度
            </button>
            <button type="button" class="secondary-button" @click="toggleShelf">
              <span>⭐</span> {{ shelfActive ? '已收藏' : '加入书架' }}
            </button>
          </div>
        </div>
      </div>
      <div class="combined-container">
        <div class="chapter-section">
          <h2 class="section-title">章节列表</h2>
          <div v-if="episodes.length" class="episode-grid">
            <router-link
              v-for="item in episodes"
              :key="item.id"
              class="episode-card"
              :to="{ name: 'AnimationWatch', query: { animationid: animationIdNum, episodeid: item.id } }"
            >
              <span class="episode-no">第 {{ item.episodeNo }} 集</span>
              <span class="episode-name">{{ item.title || ('第 ' + item.episodeNo + ' 集') }}</span>
            </router-link>
          </div>
          <div v-else class="chapter-content">暂无视频资源</div>
        </div>

        <div class="voice-section">
          <h2 class="section-title">声优阵容</h2>
          <div class="voice-list">
            <div v-for="(item, index) in voicer" :key="index" class="voice-item">
              {{ item }}
            </div>
          </div>
        </div>
      </div>

      <AcgnCommunityPanel
        v-if="animationIdNum && noveldata"
        media-type="animation"
        :media-id="animationIdNum"
        :genre="noveldata.genre"
        :title="noveldata.name"
        :pic="noveldata.bcimg"
        :shelf-active="shelfActive"
        @toggle-shelf="toggleShelf"
      />

      <RelatedWorks v-if="animationIdNum" src-type="animation" :src-id="animationIdNum" />

      <div ref="episodeSection" class="episode-local">
        <h2 class="section-title">观看进度</h2>
        <p class="hint-text">记录看到第几集，数据与当前浏览器标识一并保存在服务器。</p>
        <div class="ep-row">
          <span>看到第</span>
          <el-input-number v-model="episode" :min="1" :max="9999" @change="persistEpisode" />
          <span>集</span>
        </div>
      </div>

    </div>

    <Behind class="footer" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute } from 'vue-router';
import RelatedWorks from '@/components/RelatedWorks.vue';
import AcgnCommunityPanel from '@/components/AcgnCommunityPanel.vue';
import { getAnimationById, getAnimationEpisodes } from '@/api/animation';
import {
  pushHistory,
  toggleBookshelf,
  isInBookshelf,
  saveAnimationEpisode,
  getAnimationEpisode,
  ensureAcgnUserHydrated,
} from '@/utils/acgnLocal';

const noveldata = ref<any>();
const voicer = ref<string[]>([]);
const route = useRoute();
const episode = ref(1);
const shelfActive = ref(false);
const episodeSection = ref<HTMLElement | null>(null);
const episodes = ref<any[]>([]);

const animationIdNum = computed(() => Number(route.query.animationid));

onMounted(async () => {
  const id = route.query.animationid;
  const result = await getAnimationById(id);
  noveldata.value = result.data;
  if (noveldata.value?.voicer) {
    voicer.value = noveldata.value.voicer.split(/\s+/).filter(Boolean);
  } else {
    voicer.value = ['暂未收录'];
  }
  const nid = Number(id);
  if (nid) {
    const episodeRes = await getAnimationEpisodes(nid);
    episodes.value = Array.isArray(episodeRes?.data) ? episodeRes.data : [];
  }
  await ensureAcgnUserHydrated();
  episode.value = getAnimationEpisode(nid) || 1;
  if (nid && noveldata.value?.name) {
    pushHistory({
      type: 'animation',
      id: nid,
      name: noveldata.value.name,
      pic: noveldata.value.bcimg,
    });
    shelfActive.value = isInBookshelf('animation', nid);
  }
});


function persistEpisode() {
  saveAnimationEpisode(animationIdNum.value, episode.value);
  ElMessage.success('观看进度已保存');
}

function toTop() {
  episodeSection.value?.scrollIntoView({ behavior: 'smooth' });
}

function toggleShelf() {
  const id = animationIdNum.value;
  if (!noveldata.value || !id) return;
  const ok = toggleBookshelf({
    type: 'animation',
    id,
    name: noveldata.value.name,
    pic: noveldata.value.bcimg,
  });
  shelfActive.value = ok;
  ElMessage.success(ok ? '已加入书架' : '已移除书架');
}

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
}
.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.detail-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.novel-title {
  font-size: 2.2rem;
  color: #2c3e50;
  margin: 0 0 8px;
  line-height: 1.2;
}
.status-container {
  display: flex;
  gap: 30px;
  padding: 15px 0;
  border-bottom: 1px solid #ecf0f1;
  margin-bottom: 20px;
}
.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.status-label {
  font-size: 0.9rem;
  color: #95a5a6;
  margin-bottom: 5px;
}
.status-value {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
}
.description-text {
  line-height: 1.8;
  color: #34495e;
  font-size: 15px;
  margin: 10px 0 25px;
}
.button-group {
  display: flex;
  gap: 15px;
  margin-top: auto;
}
.main-button,
.secondary-button {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
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

.combined-container {
  display: flex;
  gap: 30px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}
.chapter-section {
  flex: 1;
  min-height: 300px;
}
.voice-section {
  flex: 0 0 300px;
  border-left: 1px solid #eee;
  padding-left: 30px;
}
.section-title {
  font-size: 1.5rem;
  color: #2c3e50;
  margin: 0 0 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #3498db;
}
.chapter-content {
  color: #7f8c8d;
  font-size: 14px;
  padding: 15px 0;
}
.episode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 12px;
}
.episode-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-height: 68px;
  padding: 12px 14px;
  border: 1px solid #dce8f2;
  border-radius: 8px;
  background: #f7fbff;
  color: #2c3e50;
  text-decoration: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;
}
.episode-card:hover {
  border-color: #3498db;
  box-shadow: 0 6px 16px rgba(52, 152, 219, 0.18);
  transform: translateY(-2px);
}
.episode-no {
  font-size: 15px;
  font-weight: 700;
}
.episode-name {
  font-size: 13px;
  color: #6b7b88;
}
.voice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.voice-item {
  padding: 10px 15px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
  color: #34495e;
}

.episode-local {
  margin-top: 24px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}
.hint-text {
  color: #7f8c8d;
  font-size: 14px;
  margin-bottom: 12px;
}
.ep-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.footer {
  position: relative;
  top: 200px;
}

@media (max-width: 1024px) {
  .combined-container {
    flex-direction: column;
  }
  .voice-section {
    border-left: none;
    border-top: 1px solid #eee;
    padding-left: 0;
    padding-top: 30px;
    flex: none;
  }
}
@media (max-width: 768px) {
  .content-wrapper {
    margin-top: 80px;
    padding: 0 15px;
  }
  .info-container {
    flex-direction: column;
  }
  .cover-wrapper {
    max-width: 300px;
    margin: 0 auto;
  }
}
</style>
