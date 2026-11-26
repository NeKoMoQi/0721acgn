<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll">
        <div v-if="loading" class="reader-status" role="status">正在加载正文…</div>
        <div v-if="loadError" class="reader-status" role="alert">
          <p>{{ loadError }}</p>
          <button @click="retry">重新加载</button>
          <router-link v-if="novelId" :to="{ name: 'NovelIntroduce', query: { novelid: novelId } }">返回作品目录</router-link>
        </div>
        <div ref="viewerEl" class="viewer" v-show="!loadError"></div>
        <div class="controls">
      <button @click="switchVolume(-1)" :disabled="loading || !!loadError || currentVolume <= 1">上一卷</button>
      <button @click="prevPage" :disabled="loading || !!loadError || !rendition">上一页</button>
      <button @click="showToc = !showToc" :disabled="loading || !!loadError || !rendition">目录</button>
      <button @click="nextPage" :disabled="loading || !!loadError || !rendition">下一页</button>
      <button @click="switchVolume(1)" :disabled="loading || !!loadError || currentVolume >= maxVolume">下一卷</button>
    </div>

    <!-- 目录侧边栏 -->
    <div v-if="showToc" class="toc-container">
  <div class="toc-paper">
    <div class="toc-header">目次</div>
    <div class="toc-content">
      <div 
        v-for="item in toc" 
        :key="item.id" 
        @click="navigateTo(item.href)"
        class="toc-item"
      >
        {{ item.label }}
      </div>
    </div>
    <div class="toc-decoration"></div>
  </div>
</div>
      </div>
        
      <section v-if="loadedSuccessfully" class="reader-report" aria-label="轻小说分页">
        <PageJump :page="currentVolume" :max="maxVolume" label="目标卷号" @jump="jumpVolume" />
        <button v-if="!locationCount" type="button" :disabled="indexing" @click="indexPages">{{ indexing ? '正在计算阅读页…' : '打开卷内页码跳转' }}</button>
        <PageJump v-else :page="readingPage" :max="locationCount" label="卷内目标页码" @jump="jumpReadingPage" />
        <p v-if="pageError" role="status">{{ pageError }}</p>
      </section>
      <MediaResource v-if="novelId" class="reader-report" type="novel" :id="novelId" :position="`第 ${currentVolume} 卷`" :report-only="!loadError" :unavailable="!!loadError" />
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
import PageJump from '@/components/PageJump.vue'
import MediaResource from '@/components/MediaResource.vue'
import { ref, shallowRef, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/stores/modules/users'
import ePub from 'epubjs'
import { getnovelcount, getnovelbyid, getnovelcontent } from '@/api/novel/index'
import { getNovelProgress, saveNovelProgress, ensureAcgnUserHydrated, pushHistory } from '@/utils/acgnLocal'
const route = useRoute()
const router = useRouter()
const usestore = useUserStore()
const novelId = Number(route.query.novelid || usestore.novelid)
const currentVolume = ref(1)
const requestedVolume = ref(1)
const maxVolume = ref(0)
const viewerEl = ref(null)
// EPUB 实例包含内部队列与 DOM 引用，不交给 Vue 做深层代理。
const book = shallowRef(null)
const rendition = shallowRef(null)
const toc = ref([])
const showToc = ref(false)
const loading = ref(false)
const loadError = ref('')
let disposed = false
const loadedSuccessfully = ref(false)
const locationCount = ref(0)
const readingPage = ref(1)
const indexing = ref(false)
const pageError = ref('')
let lastCfi = ''
let lastSave = 0
const reloading = performance.getEntriesByType('navigation')[0]?.type === 'reload'
let allowRestart = route.query.restart === '1' && !reloading

async function indexPages() {
  const active = book.value
  if (!active || indexing.value) return
  indexing.value = true
  pageError.value = ''
  try {
    await active.locations.generate(1600)
    if (active !== book.value) return
    locationCount.value = active.locations.length()
    if (lastCfi) readingPage.value = Math.max(1, active.locations.locationFromCfi(lastCfi) + 1)
  } catch {
    if (active === book.value) pageError.value = '这卷暂时无法计算页码，可继续使用目录。'
  } finally {
    if (active === book.value) indexing.value = false
  }
}

async function jumpReadingPage(page) {
  const target = book.value?.locations.cfiFromLocation(page - 1)
  if (!target || !rendition.value) return
  try { await rendition.value.display(target) }
  catch { pageError.value = '无法跳到这一页。' }
}

function saveReadingPosition() {
  if (loadedSuccessfully.value && lastCfi) saveNovelProgress(novelId, currentVolume.value, lastCfi)
}
let historyRecorded = false

const navigateTo = async (href) => {
  if (!rendition.value || loading.value) return
  try { await rendition.value.display(href); showToc.value = false }
  catch { loadError.value = '该章节暂时无法显示，请重试' }
}
const prevPage = async () => {
  if (!rendition.value || loading.value) return
  try { await rendition.value.prev() }
  catch { loadError.value = '翻页失败，请重试' }
}
const nextPage = async () => {
  if (!rendition.value || loading.value) return
  try { await rendition.value.next() }
  catch { loadError.value = '翻页失败，请重试' }
}

function destroyBook() {
  book.value?.destroy()
  book.value = null
  rendition.value = null
}

async function loadVolume(volume) {
  if (loading.value || disposed) return
  requestedVolume.value = volume
  loading.value = true
  loadError.value = ''
  saveReadingPosition()
  loadedSuccessfully.value = false
  lastCfi = ''
  locationCount.value = 0
  readingPage.value = 1
  indexing.value = false
  pageError.value = ''
  showToc.value = false
  toc.value = []
  destroyBook()
  try {
    // 同源接口拿 ArrayBuffer：OSS 没配 CORS，浏览器不能直接 fetch epub。
    const buffer = await getnovelcontent(novelId, volume)
    if (disposed) return
    if (!(buffer instanceof ArrayBuffer) || buffer.byteLength < 4) throw new Error('暂无正文数据')
    const instance = ePub()
    book.value = instance
    // 已经是二进制字节，按 binary 打开；传 'epub' 会被 epub.js 当成 URL 再去请求一次。
    await instance.open(buffer, 'binary')
    if (disposed) return
    const reader = instance.renderTo(viewerEl.value, {
      width: '100%', height: '100%', spread: 'none', manager: 'continuous',
    })
    rendition.value = reader
    reader.themes.register('lightnovel', { body: { 'font-family': 'inherit !important', 'background': 'transparent !important' } })
    reader.themes.select('lightnovel')
    const navigation = await instance.loaded.navigation
    toc.value = navigation.toc.map(item => ({ id: item.id, label: item.label, href: item.href }))
    const saved = getNovelProgress(novelId)
    const cfi = !allowRestart && saved?.chapterno === volume ? saved.cfi : undefined
    allowRestart = false
    reader.on('relocated', location => {
      if (reader !== rendition.value) return
      lastCfi = location?.start?.cfi || lastCfi
      if (locationCount.value && lastCfi) readingPage.value = Math.max(1, instance.locations.locationFromCfi(lastCfi) + 1)
      if (Date.now() - lastSave >= 5000) { lastSave = Date.now(); saveReadingPosition() }
    })
    try { await reader.display(cfi) }
    catch (error) {
      if (!cfi) throw error
      await reader.display()
    }
    if (disposed) return
    currentVolume.value = volume
    usestore.novelchapter = volume
    loadedSuccessfully.value = true
    saveNovelProgress(novelId, volume, lastCfi || undefined)
    if (!historyRecorded) {
      // 每次成功打开都是补写历史的机会，包括首卷失败后的重试。
      historyRecorded = true
      getnovelbyid(novelId).then(work => {
        if (!disposed && work?.data?.name) {
          pushHistory({ type: 'novel', id: novelId, name: work.data.name, pic: work.data.picurl })
        } else {
          historyRecorded = false
        }
      }).catch(() => { historyRecorded = false })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    if (!disposed) {
      console.error('电子书加载失败:', error)
      loadError.value = '正文加载失败，可能是该卷资源缺失或网络异常。请重试，或返回作品目录选择其他卷。'
      destroyBook()
    }
  } finally {
    loading.value = false
  }
}

function jumpVolume(volume) {
  if (loading.value || volume < 1 || volume > maxVolume.value) return
  saveReadingPosition()
  void router.push({ name: 'Novelsee', query: { novelid: novelId, chapterno: volume } })
}

const switchVolume = (delta) => jumpVolume(currentVolume.value + delta)
const handleResize = () => { rendition.value?.resize() }
function handscroll() {}

async function initializeReader() {
  if (loading.value || disposed) return
  loadError.value = ''
  if (!Number.isInteger(novelId) || novelId < 1) {
    loadError.value = '缺少作品信息，请从轻小说详情页重新进入正文。'
    return
  }
  loading.value = true
  try {
    await ensureAcgnUserHydrated()
    usestore.novelid = String(novelId)
    const count = await getnovelcount(novelId)
    const total = Number(count.data)
    if (count.code !== 200 || !Number.isInteger(total) || total < 1) throw new Error('暂无正文卷数')
    maxVolume.value = total
    const saved = getNovelProgress(novelId)
    const position = Number(route.query.chapterno ?? saved?.chapterno ?? 1)
    requestedVolume.value = Math.max(1, Math.min(Number.isInteger(position) ? position : 1, total))
  } catch (error) {
    if (!disposed) loadError.value = '无法读取作品卷数，请检查网络后重试。'
  } finally {
    loading.value = false
  }
  if (loadError.value || disposed) return
  await loadVolume(requestedVolume.value)
}
function retry() {
  if (maxVolume.value > 0) return loadVolume(requestedVolume.value)
  return initializeReader()
}
onMounted(() => {
  initializeReader()
  window.addEventListener('resize', handleResize)
  window.addEventListener('pagehide', saveReadingPosition)
})
onBeforeUnmount(() => {
  disposed = true
  saveReadingPosition()
  window.removeEventListener('pagehide', saveReadingPosition)
  destroyBook()
  window.removeEventListener('resize', handleResize)
})
  </script>
  <style scoped>
.reader-report { max-width: 900px; margin: 20px auto 72px; padding-inline: 16px; }
.reader-status { padding: 20px; color: #675642; line-height: 1.8; }
.reader-status button, .reader-status a { margin-right: 16px; }

  .toc-container {
  position: fixed;
  bottom: 120px; /* 位于目录按钮上方 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
}

.toc-paper {
  background: #fff5e6;
  width: 280px;
  border: 1px solid #e0d0b5;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  position: relative;
  padding: 20px;
  font-family: "游明朝", "YuMincho", serif;
}

.toc-paper::before {
  content: "";
  position: absolute;
  top: 0;
  left: 10px;
  right: 10px;
  height: 15px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.1) 0%, transparent 100%);
}

.toc-header {
  text-align: center;
  font-size: 1.4em;
  color: #6b4f36;
  border-bottom: 1px solid #d0b090;
  padding-bottom: 8px;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.toc-content {
  max-height: 50vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #d0b090 #fff5e6;
}

.toc-item {
  padding: 8px 15px;
  margin: 4px 0;
  color: #5a422c;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 3px;
  position: relative;
}

.toc-item:hover {
  background: #f0e0c5;
  padding-left: 20px;
}

.toc-item::before {
  content: "・";
  position: absolute;
  left: 5px;
  color: #b08050;
}

/* 自定义滚动条 */
.toc-content::-webkit-scrollbar {
  width: 6px;
}

.toc-content::-webkit-scrollbar-thumb {
  background: #d0b090;
  border-radius: 4px;
}

.toc-content::-webkit-scrollbar-track {
  background: #fff5e6;
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
    background: #fff5e6;
    border: 1px solid rgba(211, 211, 211, 0.8);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 40px;
    position: relative; /* 建立新的层叠上下文 */
    margin-bottom: 40px;
    overflow-y: auto;
    height: calc(100vh - 150px); /* 动态高度 */
    min-height: 1600px;
    -ms-overflow-style: none; /* 适用于 Internet Explorer 和旧版 Edge */
    scrollbar-width: none; /* 适用于 Firefox */
    scroll-behavior: smooth;
  }
  .viewer {
  /* 核心阅读区域 */
  height: 100% !important;
  max-width: 780px; /* 轻小说典型阅读宽度 */
  margin: 0 auto;
  padding: 40px 60px;
  background-color: #fff5e6 !important; /* 经典纸张米色 */
  box-shadow: 0 0 20px rgba(0,0,0,0.05);
  line-height: 1.8 !important;
  font-family: "游明朝", "YuMincho", "Hiragino Mincho ProN", serif !important; /* 日式明朝体 */
  position: relative;

  /* 段落样式 */
  p {
    text-indent: 2em; /* 首行缩进 */
    margin: 1.2em 0 !important;
    font-size: 1.1rem;
    color: #4a4a4a;
    letter-spacing: 0.05em;
  }

  /* 章节标题 */
  h1, h2 {
    text-align: center !important;
    font-family: "Noto Sans JP", sans-serif !important;
    font-weight: 500;
    margin: 2em 0 !important;
    position: relative;

    &::before {
      content: "◆";
      color: #d0a878;
      margin-right: 10px;
    }
  }

  /* 强调文本 */
  em {
    color: #c7696a;
    font-style: normal;
    font-weight: 600;
  }

  /* 页眉装饰线 */
  &::before {
    content: "";
    position: absolute;
    top: 30px;
    left: 50%;
    transform: translateX(-50%);
    width: 60%;
    height: 1px;
    background: linear-gradient(90deg, transparent, #d0a878 30%, #d0a878 70%, transparent);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .viewer {
    padding: 20px;
    max-width: 92%;
    
    p {
      font-size: 1rem;
      line-height: 1.7 !important;
    }
  }
}

/* 控制按钮美化 */
.controls {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255,255,255,0.9);
  padding: 12px;
  border-radius: 30px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  backdrop-filter: blur(5px);

  button {
    margin: 0 8px;
    padding: 8px 20px;
    border: none;
    background: #d0a878;
    color: white;
    border-radius: 20px;
    transition: all 0.3s;

    &:hover {
      background-color: #b58e6a;
      transform: translateY(-2px);
    }
  }
}
button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #cccccc !important;
}
  .bottom {
    position: relative;
    top: 200px;
  }
  @media (max-width: 820px) {
  .acgn_container {
    inset: auto;
    width: 100%;
    min-height: 0;
    height: auto;
    margin: 0;
    padding: 0;
  }
  .viewer {
    max-width: none;
    height: max(240px, calc(100dvh - var(--mobile-header-height) - 66px - env(safe-area-inset-bottom))) !important;
    padding: 10px 12px;
  }
  .viewer::before { display: none; }
  .controls {
    position: sticky;
    bottom: 0;
    left: auto;
    transform: none;
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 4px;
    padding: 6px 6px calc(6px + env(safe-area-inset-bottom));
    border-radius: 0;
    z-index: 1000;
  }
  .controls button { min-height: 44px; padding: 4px 0; margin: 0; font-size: 13px; }
  .toc-container {
    bottom: calc(66px + env(safe-area-inset-bottom));
    right: 8px;
    width: min(320px, calc(100% - 16px));
    max-height: calc(100dvh - var(--mobile-header-height) - 84px);
    overflow: auto;
    z-index: 1001;
  }
  .bottom { display: none; }
}
</style>
