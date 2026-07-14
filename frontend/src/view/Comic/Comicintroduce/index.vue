<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll">
        <div class="introduce" v-if="cartoondata">
  <div class="intro-image">
    <img :src=cartoondata.picurl alt="漫画封面">
  </div>
  <div class="intro-content">
    <h1 class="anime-title">{{cartoondata.name}}</h1>
    <div class="anime-subtitle">{{cartoondata.author}}</div>
    <div class="update-info">
      <div>现已更新至{{cartoondata.chaptercount}}章</div>
      <div>{{cartoondata.status}}</div>
    </div>
    <p class="anime-description">
{{ 
cartoondata.longcomment
 }}
    </p>
    <div class="intro-actions">
      <button v-if="hasProgress" class="action-button" @click="continueRead">继续阅读</button>
      <button class="action-button" @click="toTop">从头开始</button>
      <button class="action-button" :class="{ 'is-shelf': shelfActive }" type="button" @click="toggleShelf">{{ shelfActive ? '已在书架' : '追漫' }}</button>
    </div>
  </div>
</div>
<div class="chapter">
  <div class="chapter-list-scroll">
    <div class="chapter-list">
      <div v-for="(item, index) in chapter" :key="index">
        <button @click="tomain(item.chapterid,item.chapterno)">{{ item.title }}</button>
      </div>
    </div>
  </div>
</div>
<AcgnCommunityPanel
  v-if="comicIdNum && cartoondata"
  media-type="comic"
  :media-id="comicIdNum"
  :genre="cartoondata.genre"
  :title="cartoondata.name"
  :pic="cartoondata.picurl"
  :shelf-active="shelfActive"
  @toggle-shelf="toggleShelf"
/>
<RelatedWorks v-if="comicIdNum" src-type="comic" :src-id="comicIdNum" />
      </div>


      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, computed } from 'vue';
  import { ElMessage } from 'element-plus';
  import { getcartoonchapter,getcartoonbyid } from '@/api/cartoon';
  import { useRoute,useRouter } from 'vue-router';
  import useUserStore from '@/stores/modules/users';
  import RelatedWorks from '@/components/RelatedWorks.vue';
  import AcgnCommunityPanel from '@/components/AcgnCommunityPanel.vue';
  import {
    pushHistory,
    toggleBookshelf,
    isInBookshelf,
    getComicProgress,
    ensureAcgnUserHydrated,
  } from '@/utils/acgnLocal';

  const cartoondata = ref()
  const chapter = ref([])
  const $router = useRouter()
  const route = useRoute()
  const useStore = useUserStore();
  const comicIdNum = computed(() => Number(route.query.comicid))
  const shelfActive = ref(false)
  const hasProgress = ref(false)

  onMounted(async() => {
    const id = route.query.comicid;
    const result =await getcartoonchapter(id);
    const result1 = await getcartoonbyid(id)
    cartoondata.value = await result1.data;
    chapter.value = await result.data;
    useStore.cartooncomicid = id
    useStore.cartoonchapterlength = chapter.value.length
    const nid = Number(id)
    await ensureAcgnUserHydrated()
    if (nid && cartoondata.value?.name) {
      pushHistory({ type: 'comic', id: nid, name: cartoondata.value.name, pic: cartoondata.value.picurl })
      shelfActive.value = isInBookshelf('comic', nid)
    }
    hasProgress.value = !!(comicIdNum.value && getComicProgress(comicIdNum.value))
  });

  function toggleShelf() {
    const id = comicIdNum.value
    if (!cartoondata.value || !id) return
    const ok = toggleBookshelf({ type: 'comic', id, name: cartoondata.value.name, pic: cartoondata.value.picurl })
    shelfActive.value = ok
    ElMessage.success(ok ? '已加入书架' : '已移除书架')
  }

  function continueRead() {
    const id = comicIdNum.value
    const p = getComicProgress(id)
    if (p?.chapterId) {
      $router.push({ name: 'Comicsee', query: { chapterid: p.chapterId, chapterno: p.chapterno } })
      return
    }
    toTop()
  }

   const toTop =() =>{
    if (!chapter.value?.length) return
    $router.push({
      name:'Comicsee',
      query: {
        chapterid:chapter.value[0].chapterid,
        chapterno:chapter.value[0].chapterno
    }
  })
   }
    const tomain = (id,no) => {
    console.log(id)
    $router.push({
      name:'Comicsee',
      query: {
        chapterid:id,
        chapterno:no
    }
  })

  }
  function handscroll() {}
  </script>
  <style scoped>
  .chapter {
  width: 100%;
  padding: 40px 0;
  background: #f8f9fa;
  position: relative;
  z-index: 0;
}

.chapter-list-scroll {
  max-height: min(70vh, 680px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
}

/* 章节列表 */
.chapter-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
  padding: 0 20px;
  margin-bottom: 20px;
}

/* 单个章节项 */
.chapter-list > div {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 4px rgba(0,0,0,0.03);
}

/* 悬停效果 */
.chapter-list > div:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #007bff;
}

/* 章节按钮样式 */
.chapter-list button {
  width: 100%;
  border: none;
  background: none;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

/* 章节标题 */
.chapter-list button::before {
  content: "第" counter(chapter) "话";
  counter-increment: chapter;
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.chapter-list button {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chapter-list {
    grid-template-columns: 1fr;
  }
  
  .chapter {
    padding: 20px 10px;
  }
}

/* 添加卷轴编号 */
.chapter-list {
  counter-reset: chapter;
}

  .introduce {
  background: white;
  width: 100%;
  min-height: 500px;
  display: flex;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin: 10px 0;
}

.intro-image {
  flex: 0 0 40%;
  height: 480px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.intro-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.intro-content {
  flex: 1;
  margin-left: 60px;
  padding: 20px;
}

.anime-title {
  font-size: 32px;
  font-weight: 800;
  color: #2d2d2d;
  margin-bottom: 8px;
  font-family: 'Segoe UI', sans-serif;
}

.anime-subtitle {
  font-size: 20px;
  color: #666;
  margin-bottom: 15px;
}

.update-info {
  color: #888;
  font-size: 14px;
  margin-bottom: 25px;
}

.anime-description {
  color: #444;
  line-height: 1.8;
  margin: 40px 0;
  font-size: 16px;
}

.intro-actions {
    margin-top: 100px;
  display: flex;
  gap: 20px;
}

.action-button {
  padding: 12px 35px;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-button:first-child {
  background: #007bff;
  color: white;
}

.action-button:last-child {
  background: #f0f2f5;
  color: #444;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.action-button:first-child:hover {
  background: #0056b3;
}

.action-button:last-child:hover {
  background: #e4e6eb;
}
  /* 新增背景层 */
  .background-layer {
    position:fixed;
    left:0;
    top:50%;
    transform:translateY(-45%);
    width:100vw;
    height:0;
    padding-top:56.25%;          /* 1080÷1920 = 0.5625，16:9 高宽比 */
    background:url("/BCPHOTO/acgnbc4.png") no-repeat center/contain;
    overflow:hidden;            /* 如果内部还有绝对定位元素，防止溢出 */
  }
  .acgn_container {
    min-height: calc(100vh - 150px); /* 最小高度保证 */
  height: auto; /* 允许内容扩展 */
  top: 0; /* 移除定位偏移 */
  margin-top: 100px; /* 改用外边距实现间距 */
    top: 100px;
    bottom: 50px;
    width: 62%;
    margin: 0px auto 0px; /* 顶部留出Header空间 */
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(211, 211, 211, 0.8);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 40px;
    position: relative; /* 建立新的层叠上下文 */
    margin-bottom: 40px;
    overflow-y: auto;
    -ms-overflow-style: none; /* 适用于 Internet Explorer 和旧版 Edge */
    scrollbar-width: none; /* 适用于 Firefox */
  }
  .bottom {
    position: relative;
    top: 200px;
  }
  </style>