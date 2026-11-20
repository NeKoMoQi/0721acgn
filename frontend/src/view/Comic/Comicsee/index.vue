<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll">
        <div v-for="(item, index) in pagelist" :key="index">
            <img :src=item.fileurl alt="" style="width: 100%;">
        </div>
        <button v-if="hasmore && pagelist.length && !isloading && !loadError" class="more-pages" type="button" @click="loadpage">加载后续页面</button>
        <div v-if="isloading" class="tishi">加载中...请稍等哦^_^</div>
        <div v-else-if="loadError" class="tishi" role="alert">
          {{ loadError }}
          <button v-if="chapterId" type="button" class="retry-pages" @click="loadpage">重试</button>
        </div>
        <div v-else-if="!hasmore" class="tishi">{{ pagelist.length ? '已经读完本章' : '本章暂无正文' }}</div>
      </div>
      <div class="chapter-nav">
      <button class="nav-btn directory" @click="goToCatalog">
        <i class="iconfont icon-catalog"></i> 目录
      </button>
      <div class="chapter-control" v-if="chapterId">
        <button class="nav-btn prev" @click="goToChapter(previousChapter)" :disabled="!previousChapter">
          上一章
        </button>
        <button class="nav-btn next" @click="goToChapter(nextChapter)" :disabled="!nextChapter">
          下一章
        </button>
      </div>
    </div>
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
  import { computed, ref, onMounted, onBeforeUnmount } from 'vue';
  import { getcartoonpage, getcartoonchapter, getcartoonbyid } from '@/api/cartoon';
  import { useRoute, useRouter } from 'vue-router';
  import useUserStore from '@/stores/modules/users';
  import { saveComicProgress, pushHistory } from '@/utils/acgnLocal';

  const route = useRoute();
  const router = useRouter();
  const store = useUserStore();
  // App 的路由视图按完整 URL 重建，保留本次打开的作品和章节。
  const comicId = Number(route.query.comicid || store.cartooncomicid);
  const chapterId = String(route.query.chapterid ?? '');
  const chapterNo = Number(route.query.chapterno ?? 1);
  const chapters = ref([]);
  const chapterIndex = computed(() => chapters.value.findIndex(row => String(row.chapterid) === chapterId));
  const previousChapter = computed(() => chapterIndex.value > 0 ? chapters.value[chapterIndex.value - 1] : null);
  const nextChapter = computed(() => chapterIndex.value >= 0 ? chapters.value[chapterIndex.value + 1] : null);
  const pagelist = ref([]);
  const maintain = ref(null);
  const isloading = ref(false);
  const hasmore = ref(true);
  const loadError = ref('');
  let nextPage = 1;
  let disposed = false;

  async function loadpage() {
    if (!chapterId || isloading.value || !hasmore.value || disposed) return;
    const page = nextPage;
    isloading.value = true;
    loadError.value = '';
    try {
      const result = await getcartoonpage(chapterId, page);
      if (disposed) return;
      if (result?.code !== 200 || !Array.isArray(result.data)) throw new Error('漫画正文加载失败');
      const images = result.data;
      pagelist.value.push(...images);
      hasmore.value = images.length >= 4;
      nextPage = page + 1;
      if (comicId && images.length) {
        saveComicProgress(comicId, { chapterId, chapterno: chapterNo, imagePage: page });
      }
    } catch {
      if (!disposed) loadError.value = '漫画正文加载失败，请重试。';
    } finally {
      isloading.value = false;
    }
  }

  onMounted(() => {
    if (!chapterId) {
      loadError.value = '缺少章节信息，请从作品目录重新打开。';
      return;
    }
    window.scrollTo({ top: 0 });
    void loadpage();
    if (!comicId) return;
    getcartoonchapter(comicId).then((result) => {
      if (!disposed && result?.code === 200 && Array.isArray(result.data)) chapters.value = result.data;
    }).catch(() => {});
    getcartoonbyid(comicId).then((result) => {
      if (!disposed && result?.code === 200 && result.data?.name) {
        pushHistory({ type: 'comic', id: comicId, name: result.data.name, pic: result.data.picurl });
      }
    }).catch(() => {});
  });

  onBeforeUnmount(() => { disposed = true; });

  function handscroll() {
    const container = maintain.value;
    if (container && !loadError.value && container.scrollHeight - container.scrollTop - container.clientHeight < 900) {
      void loadpage();
    }
  }

  function goToCatalog() {
    router.push(comicId ? { name: 'ComicIntroduce', query: { comicid: comicId } } : { name: 'Comic' });
  }

  function goToChapter(chapter) {
    if (!chapter) return;
    router.push({
      name: 'Comicsee',
      query: { comicid: comicId, chapterid: chapter.chapterid, chapterno: chapter.chapterno },
    });
  }
  </script>
  <style scoped>
  .tishi{
  text-align: center;    /* 文字居中 */
  padding: 1rem;         /* 内边距 */
  color: #666;           /* 文字颜色 */
  font-size: 0.9rem;     /* 字体大小 */
  background: #f8f9fa;   /* 浅灰色背景 */
  border-radius: 4px;    /* 圆角 */
  margin: 10px 0;        /* 上下外边距 */
  border: 1px solid #e9ecef;
}
  .chapter-nav {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
  background: rgba(0, 0, 0, 0.7);
  padding: 12px 24px;
  border-radius: 30px;
  backdrop-filter: blur(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.nav-btn {
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  color: white;
  
  /* 目录按钮样式 */
  &.directory {
    background: linear-gradient(135deg, #6a11ff, #2575fc);
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(106, 17, 255, 0.4);
    }
  }

  /* 上一章/下一章样式 */
  &.prev, &.next {
    background: rgba(255, 255, 255, 0.1);
    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.2);
      transform: translateY(-2px);
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.chapter-control {
  display: flex;
  gap: 12px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
  padding-left: 20px;
}
  /* 新增背景层 */
  .background-layer {
    position: fixed;
    top: 80px;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: -1; /* 置于最底层 */
    background-image: url("/BCPHOTO/seebc2.jpg");
    background-size: cover;
    background-attachment: fixed;
  }
  .acgn_container {
    top: 100px;
    bottom: 50px;
    width: 62%;
    margin: 0px auto 0px; /* 顶部留出Header空间 */
    background: white;
    border: 1px solid rgba(211, 211, 211, 0.8);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 40px;
    position: relative; /* 建立新的层叠上下文 */
    margin-bottom: 40px;
    overflow-y: auto;
    height: 2500px; /* 允许内容扩展 */
    -ms-overflow-style: none; /* 适用于 Internet Explorer 和旧版 Edge */
    scrollbar-width: none; /* 适用于 Firefox */
  }
  .bottom {
    position: relative;
    top: 200px;
  }
  .more-pages { display: none; }
.retry-pages { min-height: 44px; margin-left: 8px; padding: 0 12px; }

@media (max-width: 820px) {
  .acgn_container {
    inset: auto;
    width: 100%;
    height: auto;
    margin: 0 0 14px;
    padding: 0 0 76px;
  }
  .acgn_container img { display: block; }
  .chapter-nav {
    bottom: calc(8px + env(safe-area-inset-bottom));
    width: min(360px, calc(100% - 16px));
    gap: 6px;
    padding: 6px;
  }
  .chapter-control { flex: 2; gap: 6px; padding-left: 6px; }
  .nav-btn { flex: 1; justify-content: center; min-height: 44px; padding: 4px 8px; white-space: nowrap; }
  .more-pages { display: block; margin: 18px auto; padding: 0 16px; min-height: 44px; }
  .bottom { top: auto; }
}
</style>
