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
          <div class="anime-kicker">
            <span>EP.{{ episodeNo }}</span>
            <span>SCENE MATCH</span>
            <span>{{ cardStatus }}</span>
          </div>
          <h1>巡礼对比图生成器</h1>
          <p>上传现实照片，对比动画截图，生成相似度、拍摄建议和巡礼记录。</p>
          <div class="danmaku-row">
            <span v-for="text in danmakuTexts" :key="text">{{ text }}</span>
          </div>
        </div>
        <div class="quick-data">
          <div v-for="item in quickStats" :key="item.label" class="quick-data-item">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
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
              <img v-if="realImage" :src="realImage" ref="previewImage" alt="现实照片" />
              <div v-else class="empty-image">
                <el-icon><UploadFilled /></el-icon>
                <span>点击上传现实照片</span>
                <small>JPG / PNG / GIF，最大 5MB</small>
              </div>
              <input class="file-input" type="file" accept="image/*" @change="handleRealUploading" />
            </label>
          </div>

          <div class="toolbar">
            <button class="primary-btn" type="button" :disabled="!canCompare || comparing" @click="compareImages">
              <el-icon><MagicStick /></el-icon>
              {{ comparing ? '分析中...' : '开始对比' }}
            </button>
            <button class="plain-btn" type="button" :disabled="!realImage" @click="resetComparison">
              <el-icon><Refresh /></el-icon>
              重置照片
            </button>
            <button class="plain-btn" type="button" :disabled="!showResults" @click="markAsCollected">
              <el-icon><CollectionTag /></el-icon>
              收藏结果
            </button>
          </div>

          <div class="cut-note">
            <span>{{ activeTip }}</span>
            <p>{{ activeShootingTip }}</p>
          </div>

          <div v-if="showResults" class="result-card">
            <div class="score-box">
              <strong>{{ scorePercent }}</strong>
              <span>%</span>
            </div>
            <div class="result-info">
              <div class="result-row">
                <span>评价</span>
                <strong>{{ matchLevel }}</strong>
              </div>
              <p>{{ comment }}</p>
              <div class="tag-row">
                <span v-for="tag in resultTags" :key="tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>

        <aside class="side-stack">
          <div class="panel-card">
            <div class="card-header compact">
              <div>
                <span class="mini-title">热度排行</span>
                <h2>巡礼榜单</h2>
              </div>
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="rank-tabs">
              <button
                v-for="tab in rankTabs"
                :key="tab"
                type="button"
                :class="{ active: activeRankTab === tab }"
                @click="activeRankTab = tab"
              >
                {{ tab }}
              </button>
            </div>
            <div class="rank-list">
              <button
                v-for="(item, index) in filteredRanking"
                :key="item.name"
                type="button"
                class="rank-item"
                @click="selectRanking(item)"
              >
                <span class="rank-no">{{ index + 1 }}</span>
                <span class="rank-text">
                  <strong>{{ item.name }}</strong>
                  <small>{{ item.area }} · {{ item.checkins }} 次打卡</small>
                </span>
                <span class="rank-score">{{ item.score }}</span>
              </button>
            </div>
          </div>

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
              <span class="mini-title">巡礼评价</span>
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
            <button type="button" class="primary-btn small" @click="submitReview">发布评价</button>
          </div>
          <div class="review-list">
            <div v-for="review in reviews" :key="review.user + review.text" class="review-item">
              <strong>{{ review.user }}</strong>
              <span>{{ review.text }}</span>
            </div>
          </div>
        </div>

        <div class="panel-card mission-card">
          <div class="card-header compact">
            <div>
              <span class="mini-title">巡礼小卡</span>
              <h2>报告完成度</h2>
            </div>
            <span class="progress-text">{{ missionProgress }}%</span>
          </div>
          <div class="report-preview">
            <div class="report-cover">
              <span>EP.{{ episodeNo }}</span>
              <strong>{{ reportTitle }}</strong>
              <small>{{ cardStatus }}</small>
            </div>
            <div class="report-lines">
              <p v-for="line in reportLines" :key="line">{{ line }}</p>
            </div>
          </div>
          <div class="progress-bar">
            <i :style="{ width: missionProgress + '%' }"></i>
          </div>
          <button
            v-for="task in missionList"
            :key="task.title"
            type="button"
            class="mission-item"
            :class="{ done: task.done }"
            @click="task.done = !task.done"
          >
            <span class="checkmark">{{ task.done ? '✓' : '' }}</span>
            <span>
              <strong>{{ task.title }}</strong>
              <small>{{ task.desc }}</small>
              <em>{{ task.done ? task.doneText : task.todoText }}</em>
            </span>
          </button>
        </div>

        <div class="panel-card history-card">
          <div class="card-header compact">
            <div>
              <span class="mini-title">记录</span>
              <h2>最近对比</h2>
            </div>
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div v-for="item in historyList" :key="item.time + item.title" class="history-item">
            <span>{{ item.time }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.result }}</small>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Camera,
  ChatDotRound,
  CollectionTag,
  Grid,
  MagicStick,
  Picture,
  Refresh,
  ScaleToOriginal,
  Star,
  TrendCharts,
  Trophy,
  UploadFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Headpage from '@/components/Headpage/index.vue'
import useUserStore from '@/stores/modules/users'
import { processFile } from '@/ts/picyanzheng'
import { updatePic } from '@/api/updatepic'
import { contrastpic } from '@/ts/contrastpic'

type RankingItem = {
  name: string
  area: string
  checkins: number
  score: string
  type: string
}

const router = useRouter()
const userStore = useUserStore()

const animeImage = ref('')
const previewImage = ref<HTMLImageElement | null>(null)
const realImage = ref<string | null>(null)
const canCompare = ref(false)
const comparing = ref(false)
const url2 = ref('')
const showResults = ref(false)
const similarity = ref(0)
const comment = ref('')
const userRating = ref(4)
const reviewText = ref('')
const activeMode = ref('side')
const activeRankTab = ref('综合')
const activeTip = ref('机位')
const episodeNo = ref('07')

const quickStats = [
  { value: 'CUT', label: '动画分镜' },
  { value: 'TAKE', label: '现实复拍' },
  { value: 'CARD', label: '巡礼小卡' },
]

const compareModes = [
  { key: 'side', label: '并排', icon: Grid },
  { key: 'focus', label: '聚焦', icon: ScaleToOriginal },
]

const rankTabs = ['综合', '本周', '高分']

const rankingList = ref<RankingItem[]>([
  { name: '镰仓高校前', area: '神奈川', checkins: 1836, score: '98', type: '综合' },
  { name: '须贺神社阶梯', area: '东京', checkins: 1522, score: '96', type: '综合' },
  { name: '飞驒古川站', area: '岐阜', checkins: 1043, score: '91', type: '本周' },
  { name: '伏见稻荷大社', area: '京都', checkins: 1287, score: '94', type: '本周' },
  { name: '下鸭神社', area: '京都', checkins: 876, score: '99', type: '高分' },
  { name: '秩父桥', area: '埼玉', checkins: 745, score: '97', type: '高分' },
])

const shootingTips = [
  { title: '机位', desc: '先对齐道路边缘、楼体轮廓和地平线' },
  { title: '焦段', desc: '手机建议 1x 起步，再微调到 0.8x 或 1.5x' },
  { title: '时间', desc: '截图偏暖时，优先选择清晨或傍晚复刻' },
]

const missionList = ref([
  {
    title: '补齐现实 TAKE',
    desc: '把现场照片放进对照框，生成小卡封面素材',
    done: false,
    todoText: '待解锁：现实照片缩略图',
    doneText: '已解锁：小卡封面素材',
  },
  {
    title: '生成相似度结论',
    desc: '得到匹配等级、建议标签和巡礼判断',
    done: false,
    todoText: '待解锁：相似度评分与标签',
    doneText: '已解锁：评分、等级、建议标签',
  },
  {
    title: '留下现场短评',
    desc: '把你的机位、光线或路线经验写进报告',
    done: false,
    todoText: '待解锁：用户巡礼备注',
    doneText: '已解锁：可分享巡礼备注',
  },
])

const reviews = ref([
  { user: '青空摄影部', text: '下午四点光线最像原片，逆光边缘很好看。' },
  { user: '路过的番剧党', text: '机位比想象中靠后，建议带广角镜头。' },
])

const historyList = ref([
  { time: '今天 14:20', title: '车站前街景', result: '相似度 87%' },
  { time: '昨天 18:42', title: '坡道转角', result: '相似度 73%' },
  { time: '05/07 09:15', title: '神社参道', result: '相似度 91%' },
])

const modeTitle = computed(() => (activeMode.value === 'side' ? '并排查看截图与实景' : '聚焦查看上传照片'))
const scorePercent = computed(() => Math.round(similarity.value * 100))
const finishedMissionCount = computed(() => missionList.value.filter((item) => item.done).length)
const missionProgress = computed(() => Math.round((finishedMissionCount.value / missionList.value.length) * 100))
const filteredRanking = computed(() => rankingList.value.filter((item) => item.type === activeRankTab.value).slice(0, 4))
const activeShootingTip = computed(
  () => shootingTips.find((tip) => tip.title === activeTip.value)?.desc || shootingTips[0].desc,
)
const cardStatus = computed(() => {
  if (missionProgress.value === 100) return '小卡完成'
  if (missionProgress.value >= 67) return '待补短评'
  if (missionProgress.value >= 34) return '待生成结论'
  return '待上传实景'
})
const reportTitle = computed(() => readCurrentSpot().name || '未命名巡礼场景')
const danmakuTexts = computed(() => {
  if (showResults.value) return [`相似度 ${scorePercent.value}%`, matchLevel.value, activeTip.value + '优先']
  if (realImage.value) return ['实景已就位', '可以开始对比', activeTip.value + '参考中']
  return ['截图待复刻', '寻找同款机位', '上传实景 TAKE']
})
const reportLines = computed(() => [
  missionList.value[0].done ? '封面：已放入现实 TAKE' : '封面：等待现实照片',
  missionList.value[1].done ? `结论：${scorePercent.value}% · ${matchLevel.value}` : '结论：等待 AI 对比',
  missionList.value[2].done ? `短评：${reviews.value[0]?.text || '已记录'}` : '短评：等待现场备注',
])

const matchLevel = computed(() => {
  if (similarity.value < 0.3) return '差异较大'
  if (similarity.value < 0.6) return '有巡礼价值'
  if (similarity.value < 0.85) return '高度相似'
  return '神还原取景地'
})

const resultTags = computed(() => {
  if (similarity.value < 0.3) return ['建议换角度', '低匹配', activeTip.value]
  if (similarity.value < 0.6) return ['可继续复拍', '中等匹配', activeTip.value]
  if (similarity.value < 0.85) return ['推荐打卡', '高匹配', activeTip.value]
  return ['巡礼名场面', '极高匹配', activeTip.value]
})

const readCurrentSpot = () => {
  const legacySpot = (useUserStore as unknown as { xunlilist?: Record<string, unknown> }).xunlilist
  return (legacySpot && Object.keys(legacySpot).length > 0 ? legacySpot : userStore.xunlilist) as {
    gaoqingpic?: string
    name?: string
  }
}

const handleRealUploading = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const processfile = processFile(file)
  if (!processfile) return

  const reader = new FileReader()
  reader.onload = (readerEvent) => {
    realImage.value = String(readerEvent.target?.result || '')
  }
  reader.readAsDataURL(file)

  canCompare.value = true
  showResults.value = false
  missionList.value[0].done = true

  const result = await updatePic(processfile)
  url2.value = result?.url || ''
}

const goBack = () => {
  router.push({ path: '/shengdi' })
  userStore.xunlilist = {}
  ;(useUserStore as unknown as { xunlilist?: unknown }).xunlilist = null
}

const compareImages = async () => {
  if (!animeImage.value || !url2.value || comparing.value) return

  comparing.value = true
  try {
    const result = await contrastpic(animeImage.value, url2.value)
    const rawSimilarity = Number(result?.similarity ?? 0)
    similarity.value = Math.min(1, Math.max(0, rawSimilarity + 0.3))

    if (similarity.value < 0.3) {
      comment.value = '差异较大，可能不是同一个场景。建议先对齐道路边缘和建筑轮廓。'
    } else if (similarity.value < 0.6) {
      comment.value = '部分相似，有巡礼价值。可以继续调整机位、焦段和拍摄高度。'
    } else if (similarity.value < 0.85) {
      comment.value = '非常相似，很可能是取景地。适合加入巡礼记录并分享给同好。'
    } else {
      comment.value = '几乎一模一样，取景地特征非常明确。可以作为高质量巡礼对比图。'
    }

    showResults.value = true
    missionList.value[1].done = true
    historyList.value.unshift({
      time: '刚刚',
      title: readCurrentSpot().name || '当前巡礼点',
      result: `相似度 ${scorePercent.value}%`,
    })
  } finally {
    comparing.value = false
  }
}

const resetComparison = () => {
  realImage.value = null
  canCompare.value = false
  showResults.value = false
  similarity.value = 0
  comment.value = ''
  url2.value = ''
  missionList.value[0].done = false
  missionList.value[1].done = false
}

const markAsCollected = () => {
  ElMessage.success('已收藏本次巡礼对比')
}

const submitReview = () => {
  const text = reviewText.value.trim()
  if (!text) {
    ElMessage.warning('先写一点巡礼体验吧')
    return
  }
  reviews.value.unshift({ user: '我', text })
  reviewText.value = ''
  missionList.value[2].done = true
  ElMessage.success('评价已发布')
}

const selectRanking = (item: RankingItem) => {
  activeTip.value = item.score === '99' ? '时间' : '机位'
  ElMessage.info(`已切换到 ${item.name} 的巡礼参考`)
}

onMounted(() => {
  const spot = readCurrentSpot()
  animeImage.value = spot?.gaoqingpic || ''
})
</script>

<style scoped>
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
  grid-template-columns: minmax(0, 1fr) 360px;
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

.anime-kicker,
.danmaku-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.anime-kicker {
  margin-top: 12px;
}

.anime-kicker span,
.danmaku-row span {
  border-radius: 14px;
  font-size: 12px;
  font-weight: 700;
}

.anime-kicker span {
  padding: 4px 9px;
  color: #fff;
  background: #18191c;
}

.anime-kicker span:nth-child(2) {
  background: #00aeec;
}

.anime-kicker span:nth-child(3) {
  background: #fb7299;
}

.danmaku-row {
  margin-top: 14px;
}

.danmaku-row span {
  padding: 5px 10px;
  color: #00aeec;
  background: #eaf8ff;
  border: 1px solid rgba(0, 174, 236, 0.16);
}

.quick-data {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.quick-data-item {
  min-height: 74px;
  padding: 12px;
  border-radius: 6px;
  background: #f6fbff;
  border: 1px solid rgba(0, 174, 236, 0.16);
}

.quick-data-item strong {
  display: block;
  color: #00aeec;
  font-size: 22px;
}

.quick-data-item span {
  display: block;
  margin-top: 6px;
  color: #9499a0;
  font-size: 13px;
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

.mode-tabs,
.rank-tabs {
  display: inline-flex;
  padding: 4px;
  gap: 4px;
  border-radius: 6px;
  background: #f1f2f3;
}

.mode-tabs button,
.rank-tabs button {
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

.mode-tabs button.active,
.rank-tabs button.active {
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

.result-card {
  margin-top: 16px;
  padding: 16px;
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 14px;
  border-radius: 6px;
  background: #f6fbff;
  border: 1px solid rgba(0, 174, 236, 0.18);
}

.score-box {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: baseline;
  justify-content: center;
  padding-top: 25px;
  color: #00aeec;
  background: #fff;
  border: 4px solid #00aeec;
}

.score-box strong {
  font-size: 30px;
  line-height: 1;
}

.result-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-row span {
  color: #9499a0;
}

.result-row strong {
  color: #fb7299;
  font-size: 18px;
}

.result-info p {
  margin: 10px 0;
  color: #61666d;
  line-height: 1.7;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  padding: 5px 10px;
  border-radius: 14px;
  color: #00aeec;
  background: #fff;
  border: 1px solid rgba(0, 174, 236, 0.18);
  font-size: 13px;
}

.rank-tabs {
  margin-bottom: 12px;
}

.rank-list,
.tips-list,
.review-list {
  display: grid;
  gap: 10px;
}

.rank-item,
.tip-item,
.mission-item {
  width: 100%;
  border: 1px solid #e3e5e7;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.rank-item:hover,
.tip-item:hover,
.mission-item:hover {
  border-color: #00aeec;
  transform: translateY(-1px);
}

.rank-item {
  min-height: 66px;
  padding: 10px;
  display: grid;
  grid-template-columns: 32px 1fr 42px;
  gap: 10px;
  align-items: center;
}

.rank-no {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #00aeec;
  font-weight: 700;
}

.rank-text strong,
.mission-item strong,
.review-item strong,
.history-item strong {
  display: block;
  color: #18191c;
}

.rank-text small,
.mission-item small,
.history-item small {
  display: block;
  margin-top: 4px;
  color: #9499a0;
}

.rank-score {
  color: #fb7299;
  font-weight: 800;
  text-align: right;
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
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.9fr) minmax(260px, 0.9fr);
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

.review-item,
.history-item {
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

.progress-text {
  color: #00aeec;
  font-weight: 800;
}

.report-preview {
  margin-bottom: 12px;
  padding: 12px;
  display: grid;
  grid-template-columns: 104px 1fr;
  gap: 12px;
  border-radius: 6px;
  background: #f6fbff;
  border: 1px solid rgba(0, 174, 236, 0.18);
}

.report-cover {
  min-height: 116px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 6px;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(0, 174, 236, 0.94), rgba(251, 114, 153, 0.9)),
    repeating-linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0 6px, transparent 6px 12px);
}

.report-cover span,
.report-cover small {
  font-size: 12px;
  font-weight: 700;
}

.report-cover strong {
  display: block;
  font-size: 15px;
  line-height: 1.35;
}

.report-lines {
  display: grid;
  align-content: center;
  gap: 8px;
}

.report-lines p {
  margin: 0;
  padding: 7px 9px;
  border-radius: 6px;
  color: #61666d;
  background: #fff;
  font-size: 13px;
  line-height: 1.45;
}

.progress-bar {
  height: 8px;
  margin-bottom: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #f1f2f3;
}

.progress-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #00aeec, #fb7299);
  transition: width 0.25s ease;
}

.mission-item {
  min-height: 64px;
  margin-top: 10px;
  padding: 10px;
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 10px;
  align-items: center;
}

.checkmark {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #c9ccd0;
  font-weight: 800;
}

.mission-item.done .checkmark {
  background: #00aeec;
}

.mission-item em {
  display: inline-block;
  margin-top: 6px;
  color: #fb7299;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.history-item + .history-item {
  margin-top: 10px;
}

.history-item span {
  display: block;
  color: #00aeec;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 1020px) {
  .duizhao-title-card,
  .tool-layout,
  .interactive-grid {
    grid-template-columns: 1fr;
  }

  .quick-data {
    grid-template-columns: repeat(3, 1fr);
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

  .quick-data,
  .image-stage,
  .image-stage.stage-focus,
  .result-card {
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
