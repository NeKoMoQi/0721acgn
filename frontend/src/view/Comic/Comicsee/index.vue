<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <nav v-if="pagelist.length" class="reader-report comic-jump" aria-label="漫画页码">
        <PageJump :page="lastSavedPage || 1" :max="hasmore ? undefined : pagelist.length" :busy="isloading" @jump="jumpToImagePage" />
        <p v-if="jumpError" role="status">{{ jumpError }}</p>
      </nav>
      <!-- 内容容器 -->
      <div ref="readingContainer" class="acgn_container">
        <button v-for="(item, index) in pagelist" :key="index" type="button" class="comic-page"
          :aria-label="`从第 ${index + 1} 页进入沉浸阅读`" @click="openImmersive(index)">
          <img :src="optimizeImageUrl(item.fileurl)" alt="" decoding="async" @load="restoreScroll"
            :loading="index % PAGES_PER_BATCH === 0 ? 'eager' : 'lazy'"
            :fetchpriority="index === 0 ? 'high' : 'auto'">
        </button>
        <template v-if="hasmore && pagelist.length && !isloading && !loadError && immersivePage === null">
          <div v-if="canObserve" ref="loadMarker" class="load-marker" aria-hidden="true"></div>
          <button v-else class="more-pages" type="button" @click="loadpage">加载后续页面</button>
        </template>
        <div v-if="isloading" class="tishi">加载中...请稍等哦^_^</div>
        <div v-else-if="loadError" class="tishi" role="alert">
          {{ loadError }}
          <button v-if="chapterId" type="button" class="retry-pages" @click="loadpage">重试</button>
        </div>
        <div v-else-if="!hasmore" class="tishi">{{ pagelist.length ? '已经读完本章' : '本章暂无正文' }}</div>
      </div>
      <dialog ref="immersiveDialog" class="immersive-reader" aria-label="沉浸阅读"
        @close="finishImmersive" @keydown="handleImmersiveKeys">
        <template v-if="immersivePage !== null">
          <header class="immersive-bar">
            <span aria-live="polite">第 {{ immersivePage + 1 }} 页</span>
            <button type="button" autofocus @click="immersiveDialog.close()">退出沉浸模式</button>
          </header>
          <div class="immersive-stage" @pointerdown="startSwipe" @pointerup="endSwipe" @pointercancel="swipeStart = null">
            <img :src="optimizeImageUrl(pagelist[immersivePage].fileurl)" :alt="`漫画第 ${immersivePage + 1} 页`"
              fetchpriority="high" draggable="false">
          </div>
          <div hidden>
            <img v-for="src in upcomingImages" :key="src" :src="src" alt="" fetchpriority="low">
          </div>
          <footer>
            <div class="immersive-bar immersive-page-controls">
              <button type="button" :disabled="immersivePage === 0" @click="turnPage(-1)">上一页</button>
              <span v-if="immersiveLoading" role="status">加载中…</span>
              <span v-else-if="loadError && immersivePage >= pagelist.length - 1" role="status">{{ loadError }}</span>
              <span v-else-if="!hasmore && immersivePage >= pagelist.length - 1">本章已读完</span>
              <span v-else>左右滑动翻页</span>
              <button type="button" :disabled="immersiveLoading || (!hasmore && immersivePage >= pagelist.length - 1)"
                @click="turnPage(1)">{{ loadError && immersivePage >= pagelist.length - 1 ? '重试下一页' : '下一页' }}</button>
            </div>
            <PageJump :page="immersivePage + 1" :max="hasmore ? undefined : pagelist.length" :busy="isloading" @jump="jumpToImagePage" />
            <p v-if="jumpError" role="status">{{ jumpError }}</p>
            <nav class="immersive-bar immersive-chapters" aria-label="章节导航">
              <button type="button" :disabled="!previousChapter" @click="goToChapter(previousChapter, true)">上一章</button>
              <span>第 {{ chapterNo }} 章</span>
              <button type="button" :disabled="!nextChapter" @click="goToChapter(nextChapter, true)">下一章</button>
            </nav>
          </footer>
        </template>
      </dialog>
      <div class="chapter-nav">
      <button class="nav-btn directory" @click="goToCatalog">
        <i class="iconfont icon-catalog"></i> 目录
      </button>
      <div class="chapter-control" v-if="chapterId">
        <span v-if="lastSavedPage" class="current-page" :aria-label="`第 ${chapterNo} 章，第 ${lastSavedPage} 页`">第 {{ lastSavedPage }} 页</span>
        <button class="nav-btn prev" @click="goToChapter(previousChapter)" :disabled="!previousChapter">
          上一章
        </button>
        <button class="nav-btn next" @click="goToChapter(nextChapter)" :disabled="!nextChapter">
          下一章
        </button>
      </div>
    </div>
      <MediaResource v-if="comicId && chapterId" class="reader-report" type="comic" :id="comicId" :position="reportPosition" :report-only="!loadError" :unavailable="!!loadError" />
      <section v-if="chapterId && pagelist.length" class="reader-report" aria-label="漫画分页">
      </section>
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
import MediaResource from '@/components/MediaResource.vue'
import PageJump from '@/components/PageJump.vue'
  import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue';
  import { getcartoonpage, getcartoonchapter, getcartoonbyid } from '@/api/cartoon';
  import { useRoute, useRouter } from 'vue-router';
  import useUserStore from '@/stores/modules/users';
  import { ensureAcgnUserHydrated, getComicProgress, saveComicProgress, pushHistory } from '@/utils/acgnLocal';
  import { optimizeImageUrl } from '@/utils/imageOptimizer';

  const PAGES_PER_BATCH = 4;
  const IMMERSIVE_LOOKAHEAD = 2;
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
  const readingContainer = ref(null);
  const pagelist = ref([]);
  const isloading = ref(false);
  const hasmore = ref(true);
  const loadError = ref('');
  const canObserve = typeof IntersectionObserver !== 'undefined';
  const loadMarker = ref(null);
  const immersiveDialog = ref(null);
  const immersivePage = ref(null);
  const immersiveLoading = ref(false);
  const upcomingImages = computed(() => {
    if (immersivePage.value === null) return [];
    const start = immersivePage.value + 1;
    return pagelist.value.slice(start, start + IMMERSIVE_LOOKAHEAD).map(item => optimizeImageUrl(item.fileurl));
  });
  let openedPage = null;
  let immersiveSession = 0;
  let swipeStart = null;
  let observer;
  let nextPage = 1;
  let pageRequest = null;
  let disposed = false;
  const requestedPage = Number(route.query.pageNumber);
  let restoreTarget = Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;
  let resumeImmersive = route.query.immersive === '1';
  let restoring = restoreTarget > 1 || resumeImmersive;
  let scrollRestoreIndex = null;
  const lastSavedPage = ref(0);
  const reportPosition = computed(() => `第 ${chapterNo} 章${lastSavedPage.value ? `第 ${lastSavedPage.value} 页` : ''}`);
  let scrollFrame = 0;

  function handleImmersiveKeys(event) {
    if (event.target?.closest('input') || !['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
    event.preventDefault();
    turnPage(event.key === 'ArrowLeft' ? -1 : 1);
  }

  const jumpError = ref('');
  let explicitJump = false;

  function jumpToImagePage(page) {
    explicitJump = true;
    jumpError.value = '';
    if (page <= pagelist.value.length) loadError.value = '';
    restoreTarget = page;
    restoring = true;
    if (!pageRequest) {
      if (loadError.value && page > pagelist.value.length) void loadpage();
      else void finishLoadingPage();
    }
  }

  function recordPage(pageNumber) {
    if (pageNumber === lastSavedPage.value) return;
    lastSavedPage.value = pageNumber;
    if (comicId) saveComicProgress(comicId, { chapterId, chapterno: chapterNo, pageNumber });
  }

  function pageElements() {
    return readingContainer.value?.querySelectorAll('.comic-page') ?? [];
  }

  function restoreScroll(event) {
    if (scrollRestoreIndex === null) return;
    const pages = pageElements();
    if (event && !Array.from(pages).slice(0, scrollRestoreIndex + 1).some(page => page.contains(event.target))) return;
    pages[scrollRestoreIndex]?.scrollIntoView?.({ block: 'start' });
  }

  function releaseScrollRestore() {
    scrollRestoreIndex = null;
  }

  function recordScrollPosition() {
    if (restoring || scrollRestoreIndex !== null || immersivePage.value !== null || scrollFrame) return;
    scrollFrame = window.requestAnimationFrame(() => {
      scrollFrame = 0;
      recordVisiblePage();
    });
  }

  function recordVisiblePage() {
    if (restoring || scrollRestoreIndex !== null || immersivePage.value !== null) return;
    const container = readingContainer.value?.getBoundingClientRect();
    const top = Math.max(0, container?.top ?? 0);
    const bottom = Math.min(window.innerHeight, container?.bottom ?? window.innerHeight);
    if (bottom <= top) return;
    const readingLine = top + (bottom - top) * 0.38;
    const pages = pageElements();
    let low = 0;
    let high = pages.length - 1;
    while (low <= high) {
      const middle = Math.floor((low + high) / 2);
      const { top, bottom } = pages[middle].getBoundingClientRect();
      if (bottom <= readingLine) low = middle + 1;
      else if (top > readingLine) high = middle - 1;
      else {
        recordPage(middle + 1);
        return;
      }
    }
  }

  async function finishLoadingPage() {
    if (disposed || loadError.value || !pagelist.value.length) return;
    if (!restoring) {
      if (!lastSavedPage.value) recordPage(1);
      return;
    }
    if (pagelist.value.length < restoreTarget && hasmore.value) {
      void loadpage();
      return;
    }
    restoring = false;
    if (explicitJump) {
      explicitJump = false;
      if (restoreTarget > pagelist.value.length) {
        jumpError.value = `本章共 ${pagelist.value.length} 页。`;
        return;
      }
    }
    const index = Math.min(restoreTarget, pagelist.value.length) - 1;
    if (immersivePage.value !== null) {
      immersivePage.value = index;
      recordPage(index + 1);
      return;
    }
    if (resumeImmersive) {
      resumeImmersive = false;
      void openImmersive(index);
      return;
    }
    scrollRestoreIndex = index;
    await nextTick();
    if (disposed) return;
    restoreScroll();
    recordPage(index + 1);
  }

  watch(loadMarker, (marker) => {
    observer?.disconnect();
    if (!marker || restoring) return;
    observer = new IntersectionObserver((entries) => {
      if (loadMarker.value === marker && immersivePage.value === null && entries.some(entry => entry.isIntersecting)) {
        void loadpage();
      }
    }, { rootMargin: '0px 0px 350px 0px' });
    // 最后一张图片接近视口时就请求下一批，给长图留出阅读时间。
    observer.observe(marker.previousElementSibling);
  }, { flush: 'post' });

  watch([immersivePage, () => pagelist.value.length], ([current, count]) => {
    if (current !== null && count - current - 1 < IMMERSIVE_LOOKAHEAD && !loadError.value) {
      void loadpage();
    }
  });

  function loadpage() {
    if (pageRequest) return pageRequest;
    if (!chapterId || !hasmore.value || disposed) return;
    isloading.value = true;
    loadError.value = '';
    pageRequest = Promise.resolve().then(() => requestPage(nextPage));
    return pageRequest;
  }

  async function requestPage(page) {
    try {
      const result = await getcartoonpage(chapterId, page);
      if (disposed) return;
      if (result?.code !== 200 || !Array.isArray(result.data)) throw new Error('漫画正文加载失败');
      const images = result.data;
      pagelist.value.push(...images);
      hasmore.value = images.length >= PAGES_PER_BATCH;
      nextPage = page + 1;
    } catch {
      if (!disposed) loadError.value = '漫画正文加载失败，请重试。';
    } finally {
      pageRequest = null;
      isloading.value = false;
    }
    void finishLoadingPage();
  }

  onMounted(async () => {
    if (!chapterId) {
      loadError.value = '缺少章节信息，请从作品目录重新打开。';
      return;
    }
    window.scrollTo({ top: 0 });
    window.addEventListener('pagehide', recordVisiblePage);
    window.addEventListener('scroll', recordScrollPosition, { capture: true, passive: true });
    window.addEventListener('wheel', releaseScrollRestore, { passive: true });
    window.addEventListener('touchstart', releaseScrollRestore, { passive: true });
    window.addEventListener('keydown', releaseScrollRestore);
    window.addEventListener('pointerdown', releaseScrollRestore, { passive: true });
    if (comicId && performance.getEntriesByType('navigation')[0]?.type === 'reload') {
      await ensureAcgnUserHydrated();
      if (disposed) return;
      const saved = getComicProgress(comicId);
      if (saved?.chapterId === chapterId && saved.pageNumber) {
        restoreTarget = saved.pageNumber;
        restoring = restoreTarget > 1 || resumeImmersive;
      }
    }
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

  onBeforeUnmount(() => {
    if (scrollFrame) recordVisiblePage();
    disposed = true;
    window.removeEventListener('pagehide', recordVisiblePage);
    observer?.disconnect();
    window.cancelAnimationFrame(scrollFrame);
    window.removeEventListener('scroll', recordScrollPosition, true);
    window.removeEventListener('wheel', releaseScrollRestore);
    window.removeEventListener('touchstart', releaseScrollRestore);
    window.removeEventListener('keydown', releaseScrollRestore);
    window.removeEventListener('pointerdown', releaseScrollRestore);
  });

  async function openImmersive(index) {
    if (!immersiveDialog.value || immersivePage.value !== null) return;
    immersiveSession++;
    openedPage = index;
    immersivePage.value = index;
    recordPage(index + 1);
    await nextTick();
    immersiveDialog.value?.showModal();
  }

  function finishImmersive() {
    const current = immersivePage.value;
    immersiveSession++;
    immersivePage.value = null;
    swipeStart = null;
    if (current === null || current === openedPage) return;
    const page = pageElements()[current];
    page?.scrollIntoView({ block: 'start' });
    page?.focus({ preventScroll: true });
  }

  async function turnPage(direction) {
    const current = immersivePage.value;
    if (current === null || immersiveLoading.value) return;
    const session = immersiveSession;
    const target = current + direction;
    if (target < 0) return;
    if (target >= pagelist.value.length && hasmore.value) {
      immersiveLoading.value = true;
      try {
        await loadpage();
      } finally {
        immersiveLoading.value = false;
      }
    }
    if (!disposed && session === immersiveSession && target < pagelist.value.length) {
      immersivePage.value = target;
      recordPage(target + 1);
    }
  }

  function startSwipe(event) {
    if (!event.isPrimary || event.button !== 0) return;
    swipeStart = { id: event.pointerId, x: event.clientX, y: event.clientY };
  }

  function endSwipe(event) {
    if (swipeStart?.id !== event.pointerId) return;
    const dx = event.clientX - swipeStart.x;
    const dy = event.clientY - swipeStart.y;
    swipeStart = null;
    if (Math.abs(dx) >= 45 && Math.abs(dx) > Math.abs(dy) * 1.3) {
      void turnPage(dx < 0 ? 1 : -1);
    }
  }

  function goToCatalog() {
    router.push(comicId ? { name: 'ComicIntroduce', query: { comicid: comicId } } : { name: 'Comic' });
  }

  function goToChapter(chapter, immersive = false) {
    if (!chapter) return;
    const query = { comicid: comicId, chapterid: chapter.chapterid, chapterno: chapter.chapterno };
    router.push({
      name: 'Comicsee',
      query: immersive ? { ...query, immersive: '1' } : query,
    });
  }
  </script>
  <style scoped>
.comic-jump { margin-block: 20px !important; position: relative; }
.reader-report { max-width: 900px; margin: 20px auto 72px; padding-inline: 16px; }
  .current-page { align-self: center; color: white; font-size: 14px; white-space: nowrap; }
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
.load-marker { height: 1px; }
.more-pages { display: block; margin: 18px auto; padding: 0 16px; min-height: 44px; }
.comic-page {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: #f5f5f5;
  cursor: zoom-in;
  scroll-margin-top: calc(var(--mobile-header-height, 80px) + 8px);
}
.comic-page img {
  display: block;
  width: 100%;
  aspect-ratio: auto 2 / 3;
}
.immersive-reader[open] {
  position: fixed;
  inset: 0;
  margin: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: 100%;
  height: 100dvh;
  max-width: none;
  max-height: none;
  border: 0;
  padding: env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left);
  background: #111;
  color: #fff;
}
:global(html:has(.immersive-reader[open])) { overflow: hidden; }
.immersive-stage { min-width: 0; min-height: 0; touch-action: pan-y pinch-zoom; }
.immersive-stage img { width: 100%; height: 100%; object-fit: contain; display: block; user-select: none; }
.immersive-bar { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 10px; }
.immersive-chapters { border-top: 1px solid #444; }
.immersive-bar span { text-align: center; }
.immersive-bar button {
  flex-shrink: 0;
  min-height: 44px;
  padding: 4px 10px;
  border: 1px solid #777;
  border-radius: 8px;
  background: #292929;
  color: #fff;
}
.immersive-bar button:disabled { opacity: .45; }
.retry-pages { min-height: 44px; margin-left: 8px; padding: 0 12px; }

@media (max-width: 820px) {
  .acgn_container {
    inset: auto;
    width: 100%;
    height: auto;
    margin: 0 0 14px;
    padding: 0 0 76px;
  }
  .chapter-nav {
    bottom: calc(8px + env(safe-area-inset-bottom));
    width: min(360px, calc(100% - 16px));
    gap: 6px;
    padding: 6px;
  }
  .chapter-control { flex: 2; gap: 6px; padding-left: 6px; }
  .nav-btn { flex: 1; justify-content: center; min-height: 44px; padding: 4px 8px; white-space: nowrap; }
  .bottom { top: auto; }
}
</style>
