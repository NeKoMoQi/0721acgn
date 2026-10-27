<template>
  <div v-if="media" class="extras-grid">
    <section class="extra-card store-card">
      <div class="card-heading"><h2>版本与价格</h2><span>正版渠道</span></div>
      <div v-if="media.offers.length" class="offer-list">
        <a v-for="offer in media.offers" :key="offer.label" :href="offer.url" target="_blank" rel="noopener noreferrer" class="offer-row">
          <span><strong>{{ offer.platform }}<template v-if="offer.label === '免费试玩'"> · 试玩</template></strong><small>{{ offer.label }}</small></span>
          <b>{{ offer.priceLabel }} <span aria-hidden="true">↗</span></b>
        </a>
      </div>
      <a v-else-if="officialUrl" class="offer-row" :href="officialUrl" target="_blank" rel="noopener noreferrer"><strong>官方商品与版本</strong><b>前往查看 ↗</b></a>
      <p v-else class="fine-print">暂未收录销售渠道。</p>
      <p class="fine-print">{{ media.offers.length ? `中国区商店 · ${media.checkedAt} 采集，现价以商品页为准。` : '不同平台的版本、语言与内容可能有差异，请以具体商品页为准。' }}</p>
    </section>

    <section class="extra-card character-card">
      <div class="card-heading"><h2>角色速览</h2><span>登场人物</span></div>
      <button v-for="person in media.characters.slice(0, 2)" :key="person.name" type="button" class="character-row" @click="charactersOpen = true">
        <span class="portrait"><img v-if="person.image && !failedImages[person.image]" :src="person.image" :alt="person.name" :style="{ objectPosition: person.imagePosition || 'center 18%' }" loading="lazy" @error="failedImages[person.image] = true"><span v-else>{{ person.name.slice(0, 2) }}</span></span>
        <span class="character-copy"><strong>{{ person.name }}</strong><span>{{ person.summary }}</span></span>
        <span class="row-arrow" aria-hidden="true">↗</span>
      </button>
      <button v-if="media.characters.length" type="button" class="text-button" @click="charactersOpen = true">查看角色介绍（{{ media.characters.length }}） →</button>
    </section>

    <section class="extra-card gallery-card">
      <div class="card-heading"><h2>画面预览</h2><span>{{ media.screenshots.length ? `${media.screenshots.length} 张官方商店画面 · 点击放大` : '官方网站' }}</span></div>
      <div v-if="media.screenshots.length" class="preview-tabs" aria-label="预览类型">
        <button v-for="tab in previewTabs" :key="tab" type="button" :class="{ active: previewCategory === tab }" :aria-pressed="previewCategory === tab" @click="previewCategory = tab">{{ tab }}</button>
      </div>
      <div v-if="media.screenshots.length" class="preview-grid">
        <button v-for="shot in visibleShots" :key="shot.src" type="button" class="preview-button" :aria-label="`放大${shot.alt}`" @click="openPreview(media.screenshots.indexOf(shot))">
          <img :src="shot.src" :alt="shot.alt" loading="lazy"><span class="preview-number">{{ shot.category }} <span aria-hidden="true">⤢</span></span>
        </button>
      </div>
      <a v-else-if="media.officialGalleryUrl" class="gallery-fallback" :href="media.officialGalleryUrl" target="_blank" rel="noopener noreferrer"><span>在官网查看作品画面</span><span aria-hidden="true">↗</span><small>该作品画面未在本站转载。</small></a>
      <p v-if="media.screenshots.length" class="fine-print">图片来自<a :href="media.screenshots[0].sourceUrl" target="_blank" rel="noopener noreferrer">发行方 Steam 商品页 ↗</a>，对应所链接的商店版本，版权归原权利人。</p>
      <p v-if="media.galleryNote" class="fine-print">{{ media.galleryNote }}</p>
      <a v-if="media.pv" class="pv-card" :href="media.pv.watchUrl" target="_blank" rel="noopener noreferrer">
        <div class="pv-poster"><img :src="media.pv.poster" :alt="`${title} ${media.pv.title}封面`" loading="lazy"><span aria-hidden="true">▶</span></div>
        <div><span class="pv-kicker">OFFICIAL VIDEO</span><h3>{{ media.pv.title }}</h3><p>在发行方 Steam 商品页观看 ↗</p><small>按商品页提供的视频观看，可能包含剧情画面。</small></div>
      </a>
    </section>
  </div>

  <el-dialog v-model="charactersOpen" :title="`${title} · 角色介绍`" width="760px" class="galgame-characters-dialog" append-to-body>
    <div class="all-characters">
      <article v-for="person in media?.characters" :key="person.name" class="character-profile">
        <span class="portrait profile-portrait"><img v-if="person.image && !failedImages[person.image]" :src="person.image" :alt="person.name" :style="{ objectPosition: person.imagePosition || 'center 18%' }" @error="failedImages[person.image] = true"><span v-else>{{ person.name.slice(0, 2) }}</span></span>
        <div><h3>{{ person.name }}</h3><small v-if="person.originalName && person.originalName !== person.name">{{ person.originalName }}</small><p>{{ person.summary }}</p><a :href="person.sourceUrl" target="_blank" rel="noopener noreferrer">角色资料来源 ↗</a></div>
      </article>
    </div>
  </el-dialog>

  <el-dialog v-model="previewOpen" :title="`${title} · 画面预览 ${activePreview + 1}/${media?.screenshots.length || 0}`" width="1100px" class="galgame-preview-dialog" append-to-body @keydown.left.prevent="movePreview(-1)" @keydown.right.prevent="movePreview(1)">
    <template v-if="currentShot">
      <img class="full-preview" :src="currentShot.src" :alt="currentShot.alt">
      <p class="fine-print">{{ currentShot.category }}</p>
      <div class="preview-controls"><el-button @click="movePreview(-1)">← 上一张</el-button><a :href="currentShot.sourceUrl" target="_blank" rel="noopener noreferrer">查看图片来源 ↗</a><el-button @click="movePreview(1)">下一张 →</el-button></div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { GalgameMedia } from '@/data/galgameMedia'
const props = defineProps<{ media?: GalgameMedia; title: string; officialUrl: string }>()
const charactersOpen = ref(false)
const previewOpen = ref(false)
const activePreview = ref(0)
const failedImages = reactive<Record<string, boolean>>({})
const previewCategory = ref('全部')
function groupOf(category: string) {
  if (/对白|正文/.test(category)) return '对白与正文'
  if (/场景/.test(category)) return '场景'
  if (/CG|演出|叙事|剧情/.test(category)) return 'CG与演出'
  if (/界面|玩法|探索|调查|图表|菜单|地图|档案/.test(category)) return '玩法与系统'
  return '其他'
}
const previewTabs = computed(() => ['全部', ...new Set(props.media?.screenshots.map(shot => groupOf(shot.category)) || [])])
const visibleShots = computed(() => props.media?.screenshots.filter(shot => previewCategory.value === '全部' || groupOf(shot.category) === previewCategory.value) || [])
const currentShot = computed(() => props.media?.screenshots[activePreview.value])
function openPreview(index: number) { activePreview.value = index; previewOpen.value = true }
function movePreview(step: number) {
  const count = props.media?.screenshots.length || 0
  if (count) activePreview.value = (activePreview.value + step + count) % count
}
watch(() => props.media?.slug, () => { charactersOpen.value = false; previewOpen.value = false; activePreview.value = 0; previewCategory.value = '全部' })
</script>

<style scoped>
.extras-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin:22px 0}
.extra-card{min-width:0;background:#fff;border:1px solid #e1e7ec;border-radius:16px;padding:24px}
.card-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:18px}
.card-heading h2{font-size:18px;color:#263b4d;margin:0}.card-heading>span{color:#82919e;font-size:12px}
.offer-row{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:13px 0;border-bottom:1px dashed #e1e7ec;text-decoration:none;color:#324d5d}
.offer-row:last-child{border-bottom:0}.offer-row strong{font-size:14px}.offer-row small{display:block;margin-top:6px;color:#778b98;font-size:12px;overflow-wrap:anywhere}.offer-row b{white-space:nowrap;color:#277d77;font-size:17px}.offer-row b span{font-size:12px}
.fine-print{font-size:12px;line-height:1.7;color:#85939d;margin:18px 0 0}.fine-print a{color:#547a91;margin:0 3px}
.character-row{width:100%;border:0;padding:0;margin:0 0 16px;display:flex;align-items:center;gap:13px;text-align:left;background:transparent;cursor:pointer;color:inherit;font:inherit}
.portrait{flex:none;display:flex;align-items:center;justify-content:center;width:56px;height:64px;border-radius:11px;background:linear-gradient(145deg,#e7edf6,#cfdfeb);overflow:hidden;color:#607b8e;font-size:14px}
.portrait img{width:100%;height:100%;object-fit:cover;object-position:center 18%}.character-copy{min-width:0;flex:1}.character-copy strong{display:block;color:#30495c;font-size:15px;margin-bottom:5px}.character-copy>span{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;font-size:12px;line-height:1.6;color:#7a8b98}.row-arrow{color:#9eafba}
.text-button{border:0;background:#f3f8fa;border-radius:8px;padding:10px 13px;width:100%;text-align:left;cursor:pointer;color:#53758c;font:inherit;font-size:13px}
.gallery-card{grid-column:1/-1}.preview-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.preview-button{border:0;border-radius:10px;overflow:hidden;padding:0;background:#dce5ec;position:relative;cursor:zoom-in;aspect-ratio:16/9}.preview-button img{width:100%;height:100%;object-fit:cover;display:block;transition:transform .2s}.preview-button:hover img{transform:scale(1.04)}.preview-number{position:absolute;bottom:0;left:0;right:0;background:linear-gradient(transparent,rgba(12,24,40,.7));padding:18px 12px 9px;display:flex;justify-content:space-between;color:#fff;font-size:12px}
.gallery-fallback{display:flex;flex-wrap:wrap;gap:12px;justify-content:space-between;background:#f1f6fa;border-radius:10px;padding:24px;color:#50768f;text-decoration:none}.gallery-fallback small{flex-basis:100%;color:#8698a4}
.all-characters{display:grid;gap:22px}.character-profile{display:flex;align-items:flex-start;gap:20px;border-bottom:1px solid #edf0f2;padding-bottom:20px}.character-profile:last-child{border:0}.profile-portrait{width:100px;height:132px}.character-profile h3{margin:0 0 6px;color:#304c60}.character-profile small{color:#8798a4}.character-profile p{font-size:14px;line-height:1.9;color:#5e7280;margin:12px 0}.character-profile a,.preview-controls a{font-size:12px;color:#5a8b9e}
.full-preview{display:block;width:100%;max-height:70vh;object-fit:contain;background:#15232f;border-radius:8px}.preview-controls{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:16px}
.preview-tabs{display:flex;gap:8px;flex-wrap:wrap;margin:-3px 0 16px}.preview-tabs button{border:1px solid #e1e8e5;padding:6px 12px;background:#f8faf9;border-radius:20px;color:#7d928a;font:inherit;font-size:12px;cursor:pointer}.preview-tabs button.active{background:#e8f4ee;color:#477b68;border-color:#a9cdbb}.pv-card{display:flex;gap:22px;align-items:center;padding:18px;margin-top:22px;border:1px solid #e5e9e9;border-radius:12px;text-decoration:none;background:#f8fbfa}.pv-poster{position:relative;flex:0 0 220px;aspect-ratio:16/9;border-radius:9px;overflow:hidden;background:#233744}.pv-poster img{width:100%;height:100%;object-fit:cover;display:block}.pv-poster>span{position:absolute;inset:0;display:grid;place-items:center;color:#fff;font-size:32px;background:#12263225;text-shadow:0 2px 10px #0008}.pv-kicker{font-size:10px;color:#8da99a;letter-spacing:1px}.pv-card h3{font-size:18px;color:#34596a;margin:8px 0}.pv-card p{font-size:13px;color:#5d8e7d;margin:9px 0}.pv-card small{font-size:11px;color:#94a49d}
@media(max-width:680px){.pv-card{flex-direction:column;align-items:stretch;gap:14px;padding:13px}.pv-poster{flex:auto;width:100%}}
button:focus-visible,a:focus-visible{outline:2px solid #398c91;outline-offset:4px}
@media(max-width:680px){.extras-grid{grid-template-columns:1fr;gap:14px}.extra-card{padding:19px}.preview-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.card-heading>span{font-size:11px}.profile-portrait{width:70px;height:100px}.character-profile{gap:14px}.preview-controls{flex-wrap:wrap}.preview-controls a{order:3;flex-basis:100%;text-align:center}}
</style>
<style>
.galgame-characters-dialog,.galgame-preview-dialog{max-width:calc(100vw - 28px);border-radius:16px!important}.galgame-preview-dialog .el-dialog__body{padding-top:6px}
</style>
