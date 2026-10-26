<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll">
        <div ref="viewerEl" class="viewer"></div>
        <div class="controls">
      <button @click="switchVolume(-1)" :disabled="currentVolume === 1">上一卷</button>
      <button @click="prevPage">上一页</button>
      <button @click="showToc = !showToc">目录</button>
      <button @click="nextPage">下一页</button>
      <button @click="switchVolume(1)" :disabled="currentVolume === maxVolume">下一卷</button>
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
        
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ePub from 'epubjs'
const currentVolume = ref(1)
const maxVolume = ref(3) // 根据实际卷数修改
const bookUrl = ref("")

const viewerEl = ref(null)
const book = ref(null)
const rendition = ref(null)
const toc = ref([])
const showToc = ref(false)

// 修复函数声明
const navigateTo = (href) => {
  rendition.value?.display(href)
  showToc.value = false
}
const prevPage = async () => {
  try {
    if (!rendition.value) {
      console.warn('阅读器未初始化')
      return
    }
    console.log('翻页前位置:', currentLocation)
    await rendition.value.prev()
    window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
  } catch (error) {
    console.error('翻页失败:', error)
  }
}
const nextPage = async () => {
  try {
    if (!rendition.value) {
      console.warn('阅读器未初始化')
      return
    }
    const currentLocation = rendition.value.currentLocation()
    await rendition.value.next()
    window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
  } catch (error) {
    console.error('翻页失败:', error)
  }
}

const switchVolume = async (delta) => {
  const newVolume = currentVolume.value + delta
  if (newVolume < 1 || newVolume > maxVolume.value) return
  
  currentVolume.value = newVolume
  await initBook()
}

const initBook = async () => {
  try {
    // 销毁之前的实例
    if (book.value) {
      book.value.destroy()
      book.value = null
      rendition.value = null
    }

    // 生成新的书籍URL
    bookUrl.value = "https://acgn0721.oss-cn-hangzhou.aliyuncs.com/slzs1.epub"
    
    book.value = ePub(bookUrl.value)
    rendition.value = book.value.renderTo(viewerEl.value, {
      width: '100%',
      height: '100%',
      spread: 'none',
      manager: 'continuous',
      iframe: {
        sandbox: 'allow-scripts allow-same-origin allow-popups',
        allow: 'scripts'
      }
    })

    // 保持原有主题设置
    rendition.value.themes.register("lightnovel", {
      body: {
        'font-family': 'inherit !important',
        'background': 'transparent !important'
      }
    })
    rendition.value.themes.select("lightnovel")

    // 加载目录
    const navigation = await book.value.loaded.navigation
    toc.value = navigation.toc.map(item => ({
      id: item.id,
      label: item.label,
      href: item.href
    }))

    await rendition.value.display()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    console.error('电子书加载失败:', error)
  }
}


const handleResize = () => {
  rendition.value?.resize()
}

onMounted(() => {
  initBook()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  book.value?.destroy()
  window.removeEventListener('resize', handleResize)
})
  </script>
  <style scoped>
  .toc-container {
  position: fixed;
  bottom: 80px; /* 位于目录按钮上方 */
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
  </style>