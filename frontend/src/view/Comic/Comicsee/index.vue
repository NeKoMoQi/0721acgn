<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll">
        <div v-for="(item, index) in pagelist" >
            <img :src=item.fileurl alt="" style="width: 100%;">
        </div>
        <div v-if="isloading" class="tishi">加载中...请稍等哦^_^</div>
        <div v-if="!hasmore"  class="tishi">已经到底啦~亲(๑•̀ㅂ•́)و✧</div>
      </div>
      <div class="chapter-nav">
      <button class="nav-btn directory" @click="goToCatalog">
        <i class="iconfont icon-catalog"></i> 目录
      </button>
      <div class="chapter-control" v-if="pagelist">
        <button class="nav-btn prev" @click="goPrev" :disabled="chapterno <= 1">
          上一章
        </button>
        <button class="nav-btn next" @click="goNext" :disabled="chapterno >= chaptercount">
          下一章
        </button>
      </div>
    </div>
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
  import { ref, watch, onMounted } from 'vue';
  import { getcartoonpage } from '@/api/cartoon';
  import { useRoute, useRouter } from 'vue-router';
  import useUserStore from '@/stores/modules/users';
  import { saveComicProgress } from '@/utils/acgnLocal';

  const route = useRoute();
  const $Router = useRouter();
  const useStore = useUserStore();

  const chapterIdStr = ref('');
  const chapterno = ref(1);
  const intChapterNum = ref(0);
  const pagelist = ref([]);
  const pages = ref(1);
  const chaptercount = ref(0);
  const maintain = ref(null);
  const isloading = ref(false);
  const hasmore = ref(true);

  function paddedChapterId(num) {
    return String(num).padStart(7, '0');
  }

  function persistProgress() {
    const cid = Number(useStore.cartooncomicid);
    if (!cid || !chapterIdStr.value) return;
    saveComicProgress(cid, {
      chapterId: chapterIdStr.value,
      chapterno: chapterno.value,
      imagePage: pages.value,
    });
  }

  async function loadChapterFromRoute() {
    chapterIdStr.value = String(route.query.chapterid ?? '');
    chapterno.value = Number(route.query.chapterno ?? 1);
    const n = parseInt(chapterIdStr.value, 10);
    intChapterNum.value = Number.isFinite(n) ? n : 0;
    pages.value = 1;
    hasmore.value = true;
    isloading.value = false;
    pagelist.value = [];
    chaptercount.value = Number(useStore.cartoonchapterlength) || 0;
    if (!chapterIdStr.value) return;
    const result = await getcartoonpage(chapterIdStr.value, 1);
    pagelist.value = result.data || [];
    persistProgress();
    if (maintain.value) maintain.value.scrollTop = 0;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  watch(() => [route.query.chapterid, route.query.chapterno], loadChapterFromRoute);
  onMounted(loadChapterFromRoute);

  const goToCatalog = () => {
    $Router.push({ path: '/ComicIntroduce', query: { comicid: useStore.cartooncomicid } });
  };

  const getmorepage = async () => {
    const result = await getcartoonpage(chapterIdStr.value, pages.value);
    const chunk = result.data || [];
    pagelist.value.push(...chunk);
    if (chunk.length < 4) hasmore.value = false;
    isloading.value = false;
    persistProgress();
  };

  const loadpage = () => {
    if (isloading.value || !hasmore.value) return;
    isloading.value = true;
    pages.value++;
    getmorepage();
  };

  const handscroll = () => {
    const minsee = 900;
    if (!maintain.value) return;
    const scrollTop = maintain.value.scrollTop;
    const scrollHeight = maintain.value.scrollHeight;
    const clientHeight = maintain.value.clientHeight;
    if (scrollTop + clientHeight >= scrollHeight - minsee) {
      loadpage();
    }
  };

  const goPrev = () => {
    if (chapterno.value <= 1) return;
    const prevNum = intChapterNum.value - 1;
    const nextNo = chapterno.value - 1;
    $Router.push({
      path: '/Comicsee',
      query: { chapterid: paddedChapterId(prevNum), chapterno: nextNo },
    });
  };

  const goNext = () => {
    if (chapterno.value >= chaptercount.value) return;
    const nextNum = intChapterNum.value + 1;
    const nextNo = chapterno.value + 1;
    $Router.push({
      path: '/Comicsee',
      query: { chapterid: paddedChapterId(nextNum), chapterno: nextNo },
    });
  };
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
  </style>