<template>
  <section class="explore-panel">
    <template v-if="related.length">
      <div class="explore-heading"><h2>接着看这些</h2><span>系列与作品关联</span></div>
      <div class="related-grid">
        <router-link v-for="item in related" :key="item.entry.slug" :to="detailLink(item.entry.slug)" class="related-card">
          <img :src="item.entry.cover" :alt="item.entry.name" loading="lazy" @error="onImgError">
          <div><span class="relation-reason">{{ item.reason }}</span><h3>{{ item.entry.name }}</h3><p>{{ item.entry.tagline }}</p></div>
        </router-link>
      </div>
    </template>
    <div class="explore-heading topics-heading"><h2>按专题逛逛</h2><span>本站选编</span></div>
    <div class="topic-grid">
      <button v-for="topic in discovery.topics" :key="topic.id" type="button" class="topic-button" @click="selectedTopic = topic.id"><strong>{{ topic.title }}</strong><span>{{ members(topic.id).length }} 部作品 →</span></button>
    </div>
  </section>
  <el-dialog v-model="topicOpen" :title="activeTopic?.title || '作品专题'" width="900px" class="galgame-topic-dialog" append-to-body>
    <p class="topic-description">{{ activeTopic?.description }}</p>
    <div class="topic-results">
      <article v-for="entry in members(selectedTopic)" :key="entry.slug" class="topic-result">
        <router-link :to="detailLink(entry.slug)" @click="selectedTopic = ''"><img :src="entry.cover" :alt="entry.name" loading="lazy" @error="onImgError"></router-link>
        <div><router-link :to="detailLink(entry.slug)" class="topic-title" @click="selectedTopic = ''">{{ entry.name }} →</router-link><p>{{ entry.tagline }}</p><a v-if="selectedTopic === 'chinese'" :href="entry.officialUrl" target="_blank" rel="noopener noreferrer">中文对应商品：{{ entry.versionLabel }} ↗</a><a v-else-if="selectedTopic === 'short'" href="https://key.visualarts.gr.jp/kinetic/about.html" target="_blank" rel="noopener noreferrer">短篇定位来源：Key 官方 ↗</a></div>
      </article>
    </div>
    <p class="topic-note">专题仅展示已收录且符合条件的作品；中文支持按具体商品核对。资料核对：{{ discovery.checkedAt }}。</p>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { discovery, relatedFor } from '@/data/galgameDiscovery'
const props = defineProps<{ slug?: string }>()
const selectedTopic = ref('')
const related = computed(() => relatedFor(props.slug))
const activeTopic = computed(() => discovery.topics.find(topic => topic.id === selectedTopic.value))
const topicOpen = computed({ get: () => Boolean(activeTopic.value), set: (open) => { if (!open) selectedTopic.value = '' } })
const members = (id: string) => discovery.entries.filter(entry => entry.topics.includes(id))
const detailLink = (slug: string) => ({ name: 'GalgameIntroduce', query: { slug } })
function onImgError(event: Event) { const image = event.target as HTMLImageElement; if (!image.src.endsWith('/galgame/placeholder.svg')) image.src = '/galgame/placeholder.svg' }
watch(() => props.slug, () => { selectedTopic.value = '' })
</script>

<style scoped>
.explore-panel{border:1px solid #e4e8e9;border-radius:16px;padding:24px;background:rgba(255,255,255,.96);margin:22px 0}.explore-heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:17px}.explore-heading h2{font-size:18px;color:#314c5d;margin:0}.explore-heading>span{font-size:12px;color:#87949d}.related-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:15px;margin-bottom:25px}.related-card{display:flex;gap:12px;padding:13px;border:1px solid #e8ecee;border-radius:12px;text-decoration:none;min-width:0;background:#fafcfc}.related-card img{width:62px;height:84px;object-fit:cover;flex:none;border-radius:6px}.related-card h3{font-size:14px;line-height:1.5;color:#344c5b;margin:6px 0}.relation-reason{font-size:10px;color:#638e85;line-height:1.6}.related-card p{font-size:11px;line-height:1.7;color:#8a98a1;margin:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.topic-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.topic-button{display:flex;flex-direction:column;align-items:flex-start;gap:9px;padding:17px;border:1px solid #e4e6e5;border-radius:10px;background:linear-gradient(120deg,#f4f9f6,#fff9f1);color:#4d7068;cursor:pointer;font:inherit;text-align:left}.topic-button strong{font-size:14px}.topic-button>span{font-size:12px;color:#8a9d94}.topic-button:hover,.related-card:hover{border-color:#8db9a8;background:#f0f8f3}.topic-description,.topic-note{font-size:13px;color:#85949b;line-height:1.8;margin:0 0 20px}.topic-note{margin:20px 0 0;font-size:11px}.topic-results{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.topic-result{display:flex;gap:14px;min-width:0}.topic-result img{width:76px;height:102px;object-fit:cover;border-radius:8px}.topic-result>div{min-width:0}.topic-title{font-size:14px;line-height:1.6;text-decoration:none;color:#3a6678;font-weight:600}.topic-result p{font-size:12px;color:#7f909c;line-height:1.8;margin:7px 0}.topic-result div>a:last-child:not(.topic-title){font-size:11px;color:#6c958d;line-height:1.7;overflow-wrap:anywhere}a:focus-visible,button:focus-visible{outline:2px solid #6b9e8b;outline-offset:3px}
@media(max-width:850px){.related-grid{grid-template-columns:1fr}.topic-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.topic-results{grid-template-columns:1fr}.explore-panel{padding:19px}.related-card img{width:58px;height:76px}}
</style>
<style>.galgame-topic-dialog{max-width:calc(100vw - 28px);border-radius:16px!important}</style>
