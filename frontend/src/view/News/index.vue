<template>
  <div class="news-root">
    <Headpage style="position: sticky !important; top: 0; z-index: 10" />
    <main>
      <RouterLink v-if="articleId" class="back" to="/news">← 全部资讯</RouterLink>
      <header v-else class="page-heading">
        <span class="eyebrow">0721 ACGN · NEWSROOM</span>
        <h1>新鲜事，慢慢看<span>。</span></h1>
        <p>动画、漫画、轻小说与游戏的新消息。</p>
      </header>
      <div v-if="loading" class="notice" role="status">正在读取资讯…</div>
      <div v-else-if="error" class="notice" role="alert">
        <p>{{ error }}</p><button @click="load">重新加载</button>
      </div>
      <article v-else-if="article" class="detail">
        <img v-if="cover(article)" :src="cover(article)" alt="" class="article-cover" @error="imageFailed(article.id)" />
        <div v-else class="cover detail-cover" aria-hidden="true"><span>ACGN / NEWS</span><strong>{{ article.title }}</strong><i>0721</i></div>
        <div class="article-copy">
          <div class="date">{{ formatDate(article.published_at) }} · {{ kindLabel(article.kind) }} · AI 辅助整理</div>
          <h1>{{ article.title }}</h1>
          <p class="summary">{{ article.summary }}</p>
          <p v-for="(paragraph, index) in article.paragraphs" :key="index">{{ paragraph }}</p>
          <figure v-for="image in article.images?.filter(i => i.role !== 'cover')" :key="image.role">
            <img :src="safeImage(image.url)" :alt="image.caption" class="inline-image" />
            <figcaption>{{ image.caption }} · {{ image.credit }}</figcaption>
          </figure>
          <footer><h2>资料来源</h2>
            <a v-for="source in article.sources" :key="source.url" :href="safeUrl(source.url)" target="_blank" rel="noopener noreferrer">{{ source.title || source.url }} ↗</a>
            <small v-if="article.images?.length">封面：{{ article.images[0]?.credit }}。</small>
            <small>{{ article.event_date ? `事件或原内容日期：${article.event_date}` : '原内容日期未能确认，以资料来源为准。' }}</small>
          </footer>
        </div>
      </article>
      <template v-else>
        <div v-if="!items.length" class="notice">暂时还没有发布的资讯，新消息整理好后会出现在这里。</div>
        <div class="news-grid">
          <RouterLink v-for="(item, index) in items" :key="item.id" :to="`/news/${item.id}`" class="news-card">
            <img v-if="cover(item)" :src="cover(item)" alt="" loading="lazy" class="card-cover" @error="imageFailed(item.id)" />
            <div v-else class="cover" :class="`tone-${index % 3}`" aria-hidden="true"><span>ACGN / NEWS</span><strong>{{ item.title }}</strong><i>0721</i></div>
            <div class="card-copy"><span class="date">{{ formatDate(item.published_at) }} · {{ kindLabel(item.kind) }}</span><h2>{{ item.title }}</h2><p>{{ item.summary }}</p><span class="read">读这篇 ↗</span></div>
          </RouterLink>
        </div>
        <nav v-if="total > 12" class="pagination" aria-label="资讯分页"><button :disabled="page === 1" @click="changePage(-1)">上一页</button><span>{{ page }} / {{ Math.ceil(total / 12) }}</span><button :disabled="page * 12 >= total" @click="changePage(1)">下一页</button></nav>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import request from '@/utils/request'

interface NewsArticle {
  id: string; title: string; summary: string; paragraphs: string[]; published_at: string; event_date: string
  sources: { url: string; title: string }[]
  kind?: string
  images?: { role: string; url: string; caption: string; credit: string }[]
}
const route = useRoute()
const articleId = computed(() => String(route.params.id || ''))
const items = ref<NewsArticle[]>([])
const article = ref<NewsArticle | null>(null)
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const failedImages = ref(new Set<string>())
const safeImage = (url: string) => /^\/api\/news\/image\?id=[a-f0-9]{24}&role=(cover|stats)$/.test(url) ? url : ''
const cover = (item: NewsArticle) => failedImages.value.has(item.id) ? '' : safeImage(item.images?.find(i => i.role === 'cover')?.url || '')
const imageFailed = (id: string) => { failedImages.value = new Set([...failedImages.value, id]) }
const kindLabel = (kind?: string) => kind === 'discussion' ? '社区观察' : kind === 'rumor' ? '未证实消息' : '资讯'
let generation = 0
const safeUrl = (value: string) => /^https?:\/\//i.test(value) ? value : '#'
const formatDate = (value: string) => new Date(value).toLocaleDateString('zh-CN', { timeZone: 'Asia/Shanghai' })
async function load() {
  const current = ++generation
  loading.value = true
  error.value = ''
  article.value = null
  try {
    const result: any = articleId.value
      ? await request.get('/news/byid', { params: { id: articleId.value } })
      : await request.get('/news', { params: { page: page.value } })
    if (current !== generation) return
    if (result.code !== 200) throw new Error('读取失败')
    if (articleId.value) article.value = result.data
    else { items.value = result.data.items; total.value = result.data.total }
  } catch {
    if (current === generation) error.value = articleId.value ? '这篇资讯暂时无法读取，可能已撤下。' : '资讯暂时无法读取，请稍后重试。'
  } finally {
    if (current === generation) loading.value = false
  }
}
function changePage(delta: number) { page.value += delta; void load() }
watch(articleId, () => { page.value = 1; void load() }, { immediate: true })
</script>

<style scoped>
.article-cover{display:block;width:100%;aspect-ratio:16/9;object-fit:cover}.card-cover{display:block;width:100%;height:244px;object-fit:contain;background:#eef4f1}.inline-image{display:block;width:100%;height:auto}.article-copy figure{margin:30px 0}.article-copy figcaption{font-size:12px;color:#71877c;overflow-wrap:anywhere;margin-top:10px}
.news-root{min-height:100vh;background:#f5f7f5;color:#213a33}main{max-width:1160px;margin:auto;padding:50px 24px 80px}.eyebrow,.date{font-size:12px;letter-spacing:.08em;color:#658078}.page-heading{margin:12px 0 40px}.page-heading h1{font-size:clamp(30px,4vw,48px);margin:14px 0}.page-heading h1 span{color:#61b89b}.page-heading p{color:#6c8079}.news-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px}.news-card{background:white;border-radius:18px;overflow:hidden;text-decoration:none;color:inherit;box-shadow:0 6px 24px #203c3210;transition:transform .2s}.news-card:hover{transform:translateY(-4px)}.cover{height:200px;background:#d9ebe2;position:relative;padding:22px;display:flex;flex-direction:column;justify-content:space-between;overflow:hidden}.cover:before{content:'';position:absolute;width:180px;height:180px;border:35px solid #ffffff55;border-radius:50%;right:-75px;bottom:-100px}.cover span{font-size:10px;letter-spacing:.17em;z-index:1}.cover strong{font-size:23px;line-height:1.5;max-width:92%;z-index:1;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}.cover i{font-size:12px;opacity:.6;align-self:flex-end}.tone-1{background:#e8e4f2;color:#48405d}.tone-2{background:#f2e6d5;color:#6a5140}.card-copy{padding:24px}.card-copy h2{font-size:19px;line-height:1.55;margin:12px 0}.card-copy p{font-size:14px;color:#70817c;line-height:1.8}.read{display:block;margin-top:24px;font-size:13px;color:#328566}.notice{background:white;padding:48px;border-radius:16px;text-align:center;color:#70817c}.pagination{display:flex;justify-content:center;align-items:center;gap:24px;margin-top:32px}button{border:1px solid #c9dbd1;border-radius:8px;background:white;padding:10px 18px;cursor:pointer;color:#356e59}button:disabled{opacity:.4;cursor:default}.back{display:inline-block;color:#477b67;text-decoration:none;margin-bottom:24px}.detail{max-width:860px;margin:auto;background:white;border-radius:18px;overflow:hidden}.detail-cover{height:260px;padding:30px}.detail-cover strong{font-size:32px;max-width:650px}.article-copy{padding:36px 44px}.article-copy h1{line-height:1.5;font-size:30px;margin:18px 0}.article-copy p{line-height:2;white-space:pre-wrap;overflow-wrap:anywhere}.summary{font-size:18px;color:#648376;border-left:3px solid #a0cbb8;padding-left:20px;margin:28px 0}.article-copy footer{border-top:1px solid #e6ede8;margin-top:32px;padding-top:24px}.article-copy footer h2{font-size:16px}.article-copy footer a{display:block;color:#3c8064;margin:10px 0;overflow-wrap:anywhere}.article-copy small{display:block;color:#8c9b94;margin-top:24px}@media(max-width:950px){.news-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:600px){main{padding:24px 16px}.news-grid{grid-template-columns:1fr}.article-copy{padding:24px}.article-copy h1{font-size:24px}.detail-cover strong{font-size:26px}}
</style>
