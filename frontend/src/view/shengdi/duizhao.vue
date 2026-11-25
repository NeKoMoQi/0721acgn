<template>
  <div class="duizhao-page">
    <Headpage class="duizhao-head" />

    <div class="duizhao-main">
      <section class="duizhao-title-card">
        <div>
          <button class="text-back" type="button" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回地图
          </button>
          <h1>巡礼图片对照</h1>
          <p>{{ spotTitle }}</p>
          <p v-if="ujiStop">第一季第 {{ ujiStop.ep }} 集 · 资料核对于 {{ UJI_SOURCES_CHECKED }}</p>
          <nav v-if="ujiStop" class="uji-compare-links" aria-label="宇治巡礼导航">
            <RouterLink :to="UJI_WALK">返回宇治半日巡礼</RouterLink>
            <RouterLink :to="ujiWorkLink">作品详情</RouterLink>
          </nav>
        </div>
      </section>

      <section class="tool-layout">
        <div class="compare-card">
          <div class="card-header">
            <div>
              <span class="mini-title">图片对照</span>
              <h2>{{ modeTitle }}</h2>
            </div>
            <div class="mode-tabs">
              <button
                v-for="mode in compareModes"
                :key="mode.key"
                type="button"
                :class="{ active: activeMode === mode.key }"
                @click="activeMode = mode.key"
              >
                <el-icon><component :is="mode.icon" /></el-icon>
                {{ mode.label }}
              </button>
            </div>
          </div>

          <div class="image-stage" :class="'stage-' + activeMode">
            <div class="image-box" data-label="动画截图">
              <div class="scene-tag">ANIME CUT</div>
              <img v-if="animeImage" :src="animeImage" alt="动画截图" />
              <div v-else class="empty-image">
                <el-icon><Picture /></el-icon>
                <span>暂无动画截图</span>
                <small>请从地图巡礼点进入本页</small>
              </div>
            </div>

            <label class="image-box upload-box" data-label="现实照片">
              <div class="scene-tag real">REAL TAKE</div>
              <img v-if="visiblePhoto" :src="visiblePhoto" @error="showPhotoError" :alt="realImage ? '本机照片' : spotTitle + '的实景参考照片'" />
              <div v-else class="empty-image">
                <el-icon><UploadFilled /></el-icon>
                <span>选择本机照片</span>
                <small>JPG / PNG / GIF，最大 5MB</small>
              </div>
              <input class="file-input" type="file" accept="image/jpeg,image/png,image/gif" aria-label="选择本机照片" @change="handleRealUploading" />
            </label>
          </div>

          <div class="toolbar">
            <button class="plain-btn" type="button" :disabled="!realImage" @click="resetComparison">
              <el-icon><Refresh /></el-icon>
              {{ ujiStop ? '恢复参考照片' : '重置照片' }}
            </button>
          </div>
          <p v-if="ujiStop && visiblePhoto && !realImage" class="photo-credit">
            实景参考可点击换成自己的照片。摄影 <a :href="ujiStop.photo.source" target="_blank" rel="noopener noreferrer">{{ ujiStop.photo.author }}</a>
            · <a href="https://creativecommons.org/licenses/by-sa/4.0/" target="_blank" rel="noopener noreferrer">CC BY-SA 4.0</a> · 缩放并转为 WebP
          </p>
          <p v-if="photoError" class="comparison-status" role="alert">{{ photoError }}</p>
          <p class="comparison-status">自动比对服务暂不可用。可在本机预览照片，手动对照截图。</p>

          <div class="cut-note">
            <span>{{ activeTip }}</span>
            <p>{{ activeShootingTip }}</p>
          </div>

        </div>

        <aside class="side-stack">
          <div class="panel-card">
            <div class="card-header compact">
              <div>
                <span class="mini-title">拍摄建议</span>
                <h2>复刻清单</h2>
              </div>
              <el-icon><Camera /></el-icon>
            </div>
            <div class="tips-list">
              <button
                v-for="tip in shootingTips"
                :key="tip.title"
                type="button"
                class="tip-item"
                :class="{ active: activeTip === tip.title }"
                @click="activeTip = tip.title"
              >
                <span>{{ tip.title }}</span>
                <small>{{ tip.desc }}</small>
              </button>
            </div>
          </div>
        </aside>
      </section>

      <section class="interactive-grid">
        <div class="panel-card review-card">
          <div class="card-header compact">
            <div>
              <span class="mini-title">本机巡礼备注</span>
              <h2>写下你的现场体验</h2>
            </div>
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="rating-line">
            <span>我的评分</span>
            <div class="stars" aria-label="巡礼评分">
              <button
                v-for="star in 5"
                :key="star"
                type="button"
                class="star-btn"
                :class="{ active: star <= userRating }"
                @click="userRating = star"
              >
                <el-icon><Star /></el-icon>
              </button>
            </div>
          </div>
          <textarea
            v-model="reviewText"
            maxlength="80"
            rows="4"
            placeholder="比如：站位、焦段、光线、交通路线..."
          />
          <div class="submit-row">
            <span>{{ reviewText.length }}/80</span>
            <button type="button" class="primary-btn small" @click="submitReview">保存到本机</button>
          </div>
          <div class="review-list">
            <div v-for="review in reviews" :key="review.user + review.text" class="review-item">
              <strong>{{ review.user }}</strong>
              <span>{{ review.text }}</span>
            </div>
          </div>
        </div>

      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Camera,
  ChatDotRound,
  Grid,
  Picture,
  Refresh,
  ScaleToOriginal,
  Star,
  UploadFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Headpage from '@/components/Headpage/index.vue'
import useUserStore from '@/stores/modules/users'
import { processFile } from '@/ts/picyanzheng'
import { getXunliPointById } from '@/api/xunli'
import { UJI_SOURCES_CHECKED, UJI_WALK, UJI_WORK_ID, ujiMapLink, ujiStops, ujiWorkLink } from '@/data/ujiPilgrimage'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
type CompareSpot = { id?: number; gaoqingpic?: string; name?: string; pointXunliname?: string; xunliname?: string }
const currentSpot = ref<CompareSpot>({})
const spotId = Number(route.query.spot ?? (userStore.xunlilist as CompareSpot)?.id)
const workId = Number(route.query.work)
const ujiStop = workId === UJI_WORK_ID ? ujiStops.find(stop => stop.id === spotId) : undefined
const reviewKey = Number.isSafeInteger(spotId) && spotId > 0 ? `0721acgn:pilgrimage-note:${spotId}` : ''

const animeImage = ref('')
const realImage = ref<string | null>(null)
const referencePhotoFailed = ref(false)
const visiblePhoto = computed(() => realImage.value || (!referencePhotoFailed.value ? ujiStop?.photo.src : ''))
const photoError = ref('')
const userRating = ref(4)
const reviewText = ref('')
const activeMode = ref('side')
const activeTip = ref('机位')
let photoReader: FileReader | null = null

const compareModes = [
  { key: 'side', label: '并排', icon: Grid },
  { key: 'focus', label: '聚焦', icon: ScaleToOriginal },
]

const shootingTips = [
  { title: '机位', desc: '先对齐道路边缘、楼体轮廓和地平线' },
  { title: '焦段', desc: '手机建议 1x 起步，再微调到 0.8x 或 1.5x' },
  { title: '时间', desc: '截图偏暖时，优先选择清晨或傍晚复刻' },
]

const reviews = ref<{ user: string; text: string }[]>([])

const modeTitle = computed(() => (activeMode.value === 'side' ? '并排查看截图与实景' : '聚焦查看本机照片'))
const activeShootingTip = computed(
  () => shootingTips.find((tip) => tip.title === activeTip.value)?.desc || shootingTips[0].desc,
)
const spotTitle = computed(() =>
  currentSpot.value.pointXunliname || currentSpot.value.name || currentSpot.value.xunliname || '未命名巡礼场景',
)

function handleRealUploading(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !processFile(file, { announceSuccess: false })) return

  resetComparison()
  const reader = new FileReader()
  photoReader = reader
  reader.onload = () => {
    if (photoReader !== reader) return
    photoReader = null
    if (typeof reader.result === 'string' && reader.result) realImage.value = reader.result
    else showPhotoError()
  }
  reader.onerror = () => {
    if (photoReader !== reader) return
    photoReader = null
    showPhotoError()
  }
  reader.readAsDataURL(file)
}

function resetComparison() {
  photoReader?.abort()
  photoReader = null
  realImage.value = null
  photoError.value = ''
}

function showPhotoError(event?: Event) {
  if (event && !realImage.value) {
    referencePhotoFailed.value = true
    photoError.value = '实景参考照片暂时无法读取，可以选择本机照片。'
    return
  }
  realImage.value = null
  photoError.value = '照片无法预览，请尝试选择其他图片。'
}

onBeforeUnmount(resetComparison)

const goBack = () => {
  router.push(ujiStop ? ujiMapLink : { path: '/shengdi' })
  userStore.xunlilist = {}
  ;(useUserStore as unknown as { xunlilist?: unknown }).xunlilist = null
}

const submitReview = () => {
  const text = reviewText.value.trim()
  if (!text) {
    ElMessage.warning('先写一点巡礼体验吧')
    return
  }
  if (!reviewKey) {
    ElMessage.warning('请从地图选定巡礼地点后保存备注')
    return
  }
  try {
    localStorage.setItem(reviewKey, JSON.stringify({ text, rating: userRating.value }))
  } catch {
    ElMessage.error('本机存储不可用，备注尚未保存')
    return
  }
  reviews.value = [{ user: '我的备注', text }]
  reviewText.value = ''
  ElMessage.success('备注已保存到此浏览器')
}

onMounted(async () => {
  if (reviewKey) {
    try {
      const saved = JSON.parse(localStorage.getItem(reviewKey) || 'null')
      if (typeof saved?.text === 'string' && saved.text.trim()) {
        reviews.value = [{ user: '我的备注', text: saved.text }]
        if (Number.isInteger(saved.rating) && saved.rating >= 1 && saved.rating <= 5) userRating.value = saved.rating
            }
    } catch {
      /* 本机记录损坏或存储不可用时仍可打开对照页 */
    }
  }
  const selected = userStore.xunlilist as CompareSpot
  if (selected && (!route.query.spot || Number(selected.id) === spotId)) {
    currentSpot.value = selected
  } else if (reviewKey && Number.isSafeInteger(workId) && workId > 0) {
    try {
      const result = await getXunliPointById(workId)
      if (result?.code === 200 && Array.isArray(result.data)) {
        currentSpot.value = result.data.find((spot: CompareSpot) => Number(spot.id) === spotId) || {}
      }
    } catch {
      /* 地标资料读取失败时仍可查看已保存的备注 */
    }
  }
  animeImage.value = currentSpot.value.gaoqingpic || ''
})
</script>

<style scoped>
.uji-compare-links { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 6px; }
.uji-compare-links a { color: #245a47; text-decoration: underline; }
.comparison-status { margin: 12px 0; }
.photo-credit { font-size: 13px; }
.photo-credit a { color: #245a47; text-decoration: underline; }
* {
  box-sizing: border-box;
}

.duizhao-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(246, 251, 255, 0.9) 0%, rgba(246, 247, 251, 0.88) 42%, rgba(246, 247, 251, 0.96) 100%),
    url('/xunli/duizhao-anime-bg.png') center top / cover fixed no-repeat,
    #f6f7fb;
  color: #18191c;
  font-family: PingFang SC, Microsoft YaHei, Arial, sans-serif;
}

.duizhao-head {
  position: sticky;
  top: 0;
  z-index: 30;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.duizhao-main {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 24px 0 54px;
}

.duizhao-title-card,
.compare-card,
.panel-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(229, 233, 239, 0.95);
  border-radius: 6px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.08),
    inset 0 1px 2px rgba(255, 255, 255, 0.5);
  transition: all 0.25s ease;
  backdrop-filter: blur(10px);
}

.duizhao-title-card:hover,
.compare-card:hover,
.panel-card:hover {
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.12),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px);
}

.duizhao-title-card {
  min-height: 148px;
  padding: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
  align-items: center;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.96) 0%, rgba(255, 255, 255, 0.9) 62%, rgba(234, 248, 255, 0.8) 100%),
    url('/xunli/duizhao-anime-bg.png') center 34% / cover no-repeat;
}

.duizhao-title-card::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -48px;
  width: 230px;
  height: 150px;
  opacity: 0.12;
  background:
    repeating-linear-gradient(135deg, #00aeec 0 8px, transparent 8px 16px),
    linear-gradient(135deg, #00aeec, #fb7299);
  transform: rotate(-8deg);
  pointer-events: none;
}

.text-back {
  border: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #00aeec;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
}

.duizhao-title-card h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.2;
}

.duizhao-title-card p {
  margin: 0;
  color: #61666d;
  font-size: 15px;
}

.tool-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  margin-top: 18px;
}

.compare-card,
.panel-card {
  padding: 20px;
}

.side-stack {
  display: grid;
  gap: 18px;
  align-content: start;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.card-header.compact {
  margin-bottom: 14px;
}

.mini-title {
  color: #00aeec;
  font-size: 13px;
  font-weight: 700;
}

.card-header h2 {
  margin: 5px 0 0;
  font-size: 20px;
  line-height: 1.2;
}

.mode-tabs {
  display: inline-flex;
  padding: 4px;
  gap: 4px;
  border-radius: 6px;
  background: #f1f2f3;
}

.mode-tabs button {
  min-height: 32px;
  border: 0;
  border-radius: 5px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #61666d;
  background: transparent;
  cursor: pointer;
}

.mode-tabs button.active {
  color: #00aeec;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 174, 236, 0.15);
}

.image-stage {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.image-stage.stage-focus {
  grid-template-columns: minmax(220px, 0.8fr) minmax(0, 1.2fr);
}

.image-box {
  position: relative;
  min-height: 330px;
  overflow: hidden;
  border-radius: 6px;
  background: #f1f2f3;
  border: 1px solid #e3e5e7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-box::after {
  content: '';
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  box-shadow: inset 0 0 0 1px rgba(24, 25, 28, 0.12);
  pointer-events: none;
}

.image-box::before {
  content: attr(data-label);
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  padding: 5px 10px;
  border-radius: 14px;
  color: #fff;
  background: rgba(24, 25, 28, 0.66);
  font-size: 13px;
}

.scene-tag {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 2;
  padding: 6px 10px;
  border-radius: 6px;
  color: #fff;
  background: rgba(0, 174, 236, 0.88);
  font-size: 12px;
  font-weight: 800;
}

.scene-tag.real {
  background: rgba(251, 114, 153, 0.9);
}

.upload-box {
  cursor: pointer;
}

.upload-box:hover {
  border-color: #00aeec;
}

.image-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty-image {
  min-height: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #9499a0;
  text-align: center;
}

.empty-image .el-icon {
  width: 42px;
  height: 42px;
  color: #00aeec;
}

.empty-image span {
  color: #18191c;
  font-weight: 700;
}

.empty-image small {
  font-size: 13px;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.cut-note {
  margin-top: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 6px;
  color: #61666d;
  background: linear-gradient(90deg, #f6fbff, #fff7fa);
  border: 1px dashed rgba(0, 174, 236, 0.32);
}

.cut-note span {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 14px;
  color: #fff;
  background: #00aeec;
  font-weight: 700;
}

.cut-note p {
  margin: 0;
  line-height: 1.6;
}

.primary-btn,
.plain-btn {
  min-height: 38px;
  border-radius: 6px;
  border: 0;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  font-weight: 600;
}

.primary-btn {
  color: #fff;
  background: #00aeec;
  box-shadow: 0 4px 10px rgba(0, 174, 236, 0.24);
}

.primary-btn:hover:not(:disabled) {
  background: #18bff8;
}

.plain-btn {
  color: #00aeec;
  background: #eaf8ff;
}

.primary-btn:disabled,
.plain-btn:disabled {
  cursor: not-allowed;
  color: #9499a0;
  background: #e3e5e7;
  box-shadow: none;
}

.primary-btn.small {
  min-height: 34px;
  padding: 0 14px;
}

.tips-list,
.review-list {
  display: grid;
  gap: 10px;
}

.tip-item {
  width: 100%;
  border: 1px solid #e3e5e7;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.tip-item:hover {
  border-color: #00aeec;
  transform: translateY(-1px);
}

.review-item strong {
  display: block;
  color: #18191c;
}

.tip-item {
  min-height: 58px;
  padding: 10px 12px;
}

.tip-item span {
  display: block;
  color: #18191c;
  font-weight: 700;
}

.tip-item small {
  display: block;
  margin-top: 5px;
  color: #61666d;
}

.tip-item.active {
  border-color: #00aeec;
  background: #f6fbff;
}

.interactive-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.rating-line,
.submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.rating-line span,
.submit-row span {
  color: #61666d;
}

.stars {
  display: flex;
  gap: 4px;
}

.star-btn {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 6px;
  color: #c9ccd0;
  background: #f1f2f3;
  cursor: pointer;
}

.star-btn.active {
  color: #fb7299;
  background: #fff0f5;
}

.review-card textarea {
  width: 100%;
  margin: 12px 0 10px;
  padding: 10px 12px;
  resize: vertical;
  border: 1px solid #e3e5e7;
  border-radius: 6px;
  outline: none;
  color: #18191c;
  font-family: inherit;
}

.review-card textarea:focus {
  border-color: #00aeec;
}

.review-list {
  margin-top: 14px;
}

.review-item {
  padding: 10px 12px;
  border-radius: 6px;
  background: #f6f7fb;
}

.review-item span {
  display: block;
  margin-top: 5px;
  color: #61666d;
  line-height: 1.6;
}

@media (max-width: 1020px) {
  .duizhao-title-card,
  .tool-layout,
  .interactive-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .duizhao-main {
    width: min(100% - 24px, 1180px);
    padding-top: 14px;
  }

  .duizhao-title-card,
  .compare-card,
  .panel-card {
    padding: 14px;
  }

  .image-stage,
  .image-stage.stage-focus {
    grid-template-columns: 1fr;
  }

  .card-header,
  .rating-line,
  .submit-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .image-box {
    min-height: 260px;
  }
}
</style>
