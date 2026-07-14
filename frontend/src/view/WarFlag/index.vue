<template>
  <Headpage />
  <div ref="mapcontainer" class="war-content"></div>

  <!-- 顶部回合横幅 -->
  <div v-if="battleState" class="turn-banner" :class="{ 'turn-self': isMyTurn, 'turn-enemy': !isMyTurn }">
    <div class="turn-info">
      <span class="round-no">第 {{ battleState.roundNo }} 回合</span>
      <span class="turn-label">{{ isMyTurn ? '我方行动' : '敌方行动' }}</span>
    </div>
    <button
      v-if="!isOver"
      class="end-turn-btn"
      :disabled="endTurnLoading || !isMyTurn"
      :title="!isMyTurn ? '当前为敌方回合，对方行动结束后才会轮到您' : ''"
      @click="onEndTurn"
    >
      结束回合
    </button>
    <button class="reset-btn" :disabled="resetLoading" @click="onResetBattle" title="从备份还原所有棋子状态">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M3 12a9 9 0 1 0 3-6.7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M3 4v5h5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>{{ resetLoading ? '重置中…' : '重置战局' }}</span>
    </button>
  </div>

  <!-- 玩法说明（可随时点开；首次进场自动提示一次） -->
  <button
    v-if="battleState"
    type="button"
    class="rules-help-btn"
    title="玩法说明"
    aria-label="打开玩法说明"
    @click="showRulesModal = true"
  >
    ?
  </button>

  <div
    v-if="showRulesModal"
    class="rules-modal-overlay"
    @click.self="showRulesModal = false"
  >
    <div class="rules-modal" role="dialog" aria-modal="true" aria-labelledby="rules-title">
      <div class="rules-modal-header">
        <h3 id="rules-title">战旗推演 · 玩法说明</h3>
        <button type="button" class="close-btn" aria-label="关闭" @click="showRulesModal = false">×</button>
      </div>
      <div class="rules-modal-body">
        <section class="rules-section">
          <h4>胜利条件</h4>
          <p>优先以<strong>指挥部</strong>判定：摧毁敌方指挥部即获胜；若关卡无指挥部设定，则按场上该阵营是否仍有存活单位判定。</p>
        </section>
        <section class="rules-section">
          <h4>先手与回合</h4>
          <p>您在开始页选择的阵营为<strong>我方</strong>；<strong>玩家一方始终先于本局开场行动</strong>。请点击「结束回合」结束当前阶段；轮到敌方时由 <strong>AI</strong> 自动行动并播放移动/攻击动画。</p>
        </section>
        <section class="rules-section">
          <h4>移动</h4>
          <p>在<strong>我方行动</strong>阶段，点击<strong>己方</strong>单位可查看可移动范围（由服务器按六角格规则计算）。点击高亮格子完成移动；每枚棋子每回合移动规则以后端校验为准。</p>
        </section>
        <section class="rules-section">
          <h4>攻击</h4>
          <p>选中单位后若仍可攻击，地图会标出可攻击目标。点击目标发起攻击；本回合<strong>已开火</strong>的单位通常不可再次攻击（具体以棋子状态为准）。可能存在回击等结算，血量会在动画中更新。</p>
        </section>
        <section class="rules-section">
          <h4>重置与存档</h4>
          <p>「重置战局」会从服务器<strong>备份</strong>恢复本关初始部署与回合，且仍由<strong>您当前阵营先手</strong>。开始页的「继续游戏」仅记录您上次所选阵营，进度以服务器数据为准。</p>
        </section>
        <p class="rules-foot">之后可随时点击右上角 <strong>?</strong> 再次打开本说明。</p>
        <button type="button" class="rules-ok-btn" @click="showRulesModal = false">知道了</button>
      </div>
    </div>
  </div>

  <!-- 胜负结算面板 -->
  <div v-if="isOver" class="victory-overlay">
    <div class="victory-modal" :class="victoryModalUi">
      <h2 class="victory-title">{{ winnerLabel }}</h2>
      <p class="victory-sub">第 {{ battleState?.roundNo }} 回合 · 战斗结束</p>
      <div class="victory-actions">
        <button
          type="button"
          class="victory-btn victory-btn-primary"
          :disabled="settleRestartLoading"
          @click="onRestartFromSettlement"
        >
          {{ settleRestartLoading ? '重置中…' : '重新开始' }}
        </button>
        <button type="button" class="victory-btn victory-btn-ghost" @click="goToWarStart">
          返回开始界面
        </button>
      </div>
    </div>
  </div>

  <!-- 底部信息按钮 -->
  <div v-if="showInfoButton" class="info-button-container" @click="openDetailModal">
    <div class="info-button">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
        <path d="M12 7V7.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M12 11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <span class="info-hint">点击查看详情</span>
  </div>

  <!-- 详情弹窗 -->
  <div v-if="showDetailModal" class="detail-modal-overlay" @click.self="closeDetailModal">
    <div
      class="detail-modal"
      :class="{ 'detail-modal--chess': selectedInfo?.type === 'chess' }"
      @click.stop
    >
      <template v-if="selectedInfo?.type === 'chess'">
        <div class="chess-pop">
          <div class="chess-pop-bar">
            <button
              type="button"
              class="chess-pop-x"
              aria-label="关闭"
              @click="closeDetailModal"
            >
              ×
            </button>
            <span class="chess-pop-title">单位信息</span>
          </div>
          <div class="chess-pop-body">
            <div class="chess-pop-name-row">
              <span class="chess-pop-name">{{ selectedInfo.data.chessname || '未命名单位' }}</span>
              <span :class="['chess-pop-side', Number(selectedInfo.data.player) === 0 ? 'sov' : 'ger']">
                {{ chessFactionShort(selectedInfo.data) }}
              </span>
            </div>
            <div class="chess-pop-art">
              <img
                v-if="chessDetailIconUrl"
                :src="chessDetailIconUrl"
                alt=""
              />
              <div v-else class="chess-pop-art-fallback">{{ chessDetailInitial }}</div>
            </div>
            <div class="chess-pop-hpstrip">
              <div
                class="chess-pop-hpfill"
                :class="{ low: hpPercent(selectedInfo.data) <= 35 }"
                :style="{ width: hpPercent(selectedInfo.data) + '%' }"
              ></div>
            </div>
            <div class="chess-pop-stats">
              <div
                class="chess-pop-stat"
                title="攻击力：对目标造成伤害时的基础攻击数值。"
              >
                <svg class="chess-pop-ico" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 19l4-8 3 4 4-10 3 14H5z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                </svg>
                <span class="chess-pop-num">{{ formatStat(selectedInfo.data.atk) }}</span>
              </div>
              <div
                class="chess-pop-stat"
                title="防御：本关卡未单独配置防御力，故显示为 —。"
              >
                <svg class="chess-pop-ico" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M12 3l8 4v5c0 5-3.5 9.5-8 11-4.5-1.5-8-6-8-11V7l8-4z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                </svg>
                <span class="chess-pop-num">—</span>
              </div>
              <div
                class="chess-pop-stat"
                title="移动力：本回合可移动的六角格步数上限（具体以后端校验为准）。"
              >
                <svg class="chess-pop-ico" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M13 4L4 14v5h5l9-9-5-5z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                  <path d="M16 7l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span class="chess-pop-num">{{ formatStat(selectedInfo.data.move) }}</span>
              </div>
              <div
                class="chess-pop-stat"
                title="射程：可发起攻击的最远六角格距离（格）。"
              >
                <svg class="chess-pop-ico" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M12 5v2M12 17v2M5 12h2M17 12h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span class="chess-pop-num">{{ formatStat(selectedInfo.data.range) }}</span>
              </div>
              <div
                class="chess-pop-stat"
                :title="`生命值：当前 ${selectedInfo.data.hp ?? '—'} / 上限 ${selectedInfo.data.maxHp ?? '—'}（降至 0 则单位被消灭）。`"
              >
                <svg class="chess-pop-ico" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M12 21s-6-4.35-6-10a6 6 0 1112 0c0 5.65-6 10-6 10z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                </svg>
                <span class="chess-pop-num">{{ formatStat(selectedInfo.data.hp) }}</span>
              </div>
            </div>
            <p class="chess-pop-meta">
              {{ chessTypeLabel(selectedInfo.data.type) }}
              ·
              {{ selectedInfo.data.moved ? '已移动' : '未移动' }}
              ·
              {{ selectedInfo.data.fired ? '已攻击' : '未攻击' }}
            </p>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="modal-header">
          <h3>{{ modalTitle }}</h3>
          <button class="close-btn" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body">
          <template v-if="selectedInfo?.type === 'grid'">
            <div class="grid-detail">
              <div class="info-row">
                <span class="label">格子 ID</span>
                <span class="value mono">{{ selectedInfo.data.id }}</span>
              </div>
              <div class="info-row">
                <span class="label">坐标 (q, r)</span>
                <span class="value mono">{{ selectedInfo.data.q }}, {{ selectedInfo.data.r }}</span>
              </div>
              <div class="info-row">
                <span class="label">高程</span>
                <span class="value">{{ selectedInfo.data.elevation }} m</span>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import router from '@/router'
import { useMapbox } from '@/view/WarFlag/composables/useMapbox.ts'
import { getWarGrid } from '@/api/warflag/index'
import { getWarChessing, initBattle } from '@/api/warflag/warChessing/index'

const BATTLE_ID = 1001
const RULES_SEEN_KEY = 'warflag:rulesSeen'

function resolveSideQuery(sideRaw) {
  const side = Array.isArray(sideRaw) ? sideRaw[0] : sideRaw
  if (side === 'german' || side === '1') return 1
  if (side === 'soviet' || side === '0') return 0
  return null
}

const route = useRoute()
const myPlayerIdRaw = resolveSideQuery(route.query.side)

if (myPlayerIdRaw === null) {
  router.replace('/warflag')
}

/** 与数据库 player 一致：0=苏军，1=德军 */
const myPlayerId = myPlayerIdRaw !== null ? myPlayerIdRaw : 0

const mapcontainer = ref(null)
const endTurnLoading = ref(false)
const resetLoading = ref(false)
const settleRestartLoading = ref(false)
const showRulesModal = ref(false)

const {
  init,
  selectedInfo,
  showInfoButton,
  showDetailModal,
  openDetailModal,
  closeDetailModal,

  battleState,
  isMyTurn,
  isOver,
  winnerLabel,
  endTurn,
  resetBattle
} = useMapbox(mapcontainer, {
  center: [36.2, 51.7],
  zoom: 9.5,
  minZoom: 9.2,
  maxZoom: 12,
  maxBounds: [[35.0, 51.35], [37.3, 52.0]],
  battleId: BATTLE_ID,
  myPlayerId
})

const victoryModalUi = computed(() => {
  const s = battleState.value?.status
  const w = battleState.value?.winner
  if (s === 'DRAW') return 'draw'
  if (w === myPlayerId) return 'win'
  if (w != null) return 'lose'
  return ''
})

const modalTitle = computed(() => {
  if (!selectedInfo.value || selectedInfo.value.type !== 'grid') return ''
  return '格子信息'
})

function chessFactionShort(row) {
  const n = Number(row.player)
  if (n === 0) return '苏'
  if (n === 1) return '德'
  return '?'
}

function formatStat(v) {
  if (v === undefined || v === null || v === '') return '—'
  return v
}

function hpPercent(row) {
  const max = Number(row.maxHp)
  const hp = Number(row.hp)
  if (!Number.isFinite(max) || max <= 0) return 0
  if (!Number.isFinite(hp)) return 0
  return Math.min(100, Math.max(0, Math.round((hp / max) * 100)))
}

function chessTypeLabel(typeRaw) {
  if (!typeRaw && typeRaw !== 0) return '单位'
  const t = String(typeRaw)
  const s = t.toLowerCase()
  if (s === 'infantry') return '步兵'
  if (s === 'tank') return '坦克'
  if (s === 'artillery') return '炮兵'
  if (s === 'germancommand' || t === '10') return '德军指挥部'
  if (s === 'sovcommand' || t === '11') return '苏军指挥部'
  return t
}

const chessDetailIconUrl = computed(() => {
  if (selectedInfo.value?.type !== 'chess') return ''
  const u = selectedInfo.value.data.chessurl
  if (!u) return ''
  return String(u)
})

const chessDetailInitial = computed(() => {
  if (selectedInfo.value?.type !== 'chess') return '?'
  const name = selectedInfo.value.data.chessname
  const s = name != null ? String(name).trim() : ''
  if (!s) return '棋'
  return s.slice(0, 1)
})

async function onEndTurn() {
  endTurnLoading.value = true
  try {
    await endTurn()
  } finally {
    endTurnLoading.value = false
  }
}

async function onResetBattle() {
  if (resetLoading.value) return
  if (!window.confirm('确定要重置整场战局吗？所有棋子位置 / 血量 / 回合都会从备份还原。')) {
    return
  }
  resetLoading.value = true
  try {
    await resetBattle()
  } finally {
    resetLoading.value = false
  }
}

async function onRestartFromSettlement() {
  if (settleRestartLoading.value) return
  settleRestartLoading.value = true
  try {
    const ok = await resetBattle()
    if (!ok) {
      window.alert('重新开始失败，请检查网络或后端服务')
    }
  } finally {
    settleRestartLoading.value = false
  }
}

function goToWarStart() {
  router.push('/warflag')
}

;(async () => {
  if (myPlayerIdRaw === null) return

  const sideForLs = myPlayerIdRaw === 0 ? 'soviet' : 'german'
  try {
    localStorage.setItem('warflag:lastSide', sideForLs)
  } catch {
    /* ignore */
  }

  try {
    await initBattle(BATTLE_ID, myPlayerId)
  } catch (e) {
    console.warn('initBattle 调用失败，继续按现有数据加载', e)
  }

  const [gridRes, chessRes] = await Promise.all([
    getWarGrid(),
    getWarChessing(BATTLE_ID)
  ])
  if (gridRes?.code !== 200 || chessRes?.code !== 200) {
    console.warn('战棋地图或棋子数据加载失败', gridRes, chessRes)
    return
  }
  await init(gridRes.data, chessRes.data)

  try {
    if (!localStorage.getItem(RULES_SEEN_KEY)) {
      showRulesModal.value = true
      localStorage.setItem(RULES_SEEN_KEY, '1')
    }
  } catch {
    /* ignore */
  }
})()
</script>

<style scoped>
.war-content {
  position: absolute;
  inset: 80px 0 0 0;
}

/* ===== 顶部回合横幅 ===== */
.turn-banner {
  position: fixed;
  top: 96px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 18px;
  border-radius: 999px;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: bannerDrop 0.35s ease;
}

@keyframes bannerDrop {
  from { opacity: 0; transform: translate(-50%, -10px); }
  to   { opacity: 1; transform: translate(-50%, 0); }
}

.turn-banner.turn-self {
  border: 2px solid #3b82f6;
}

.turn-banner.turn-enemy {
  border: 2px solid #ef4444;
}

.turn-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.round-no {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.turn-label {
  font-size: 16px;
  font-weight: 700;
  color: #222;
}

.turn-banner.turn-self .turn-label { color: #1d4ed8; }
.turn-banner.turn-enemy .turn-label { color: #b91c1c; }

.end-turn-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #f97316 0%, #ef4444 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35);
}

.end-turn-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.5);
}

.end-turn-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.reset-btn svg {
  width: 16px;
  height: 16px;
}

.reset-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.55);
}

.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 玩法说明 ===== */
.rules-help-btn {
  position: fixed;
  top: 96px;
  right: 20px;
  z-index: 1001;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 50%;
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(145deg, #0ea5e9, #6366f1);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.45);
  transition: transform 0.15s, box-shadow 0.15s;
  line-height: 1;
}

.rules-help-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.5);
}

.rules-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2500;
  padding: 20px;
  animation: fadeIn 0.2s ease;
}

.rules-modal {
  width: 100%;
  max-width: 520px;
  max-height: min(88vh, 720px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.28);
  animation: scaleIn 0.2s ease;
}

.rules-modal-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.rules-modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.rules-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px 20px;
  -webkit-overflow-scrolling: touch;
}

.rules-section {
  margin-top: 16px;
}

.rules-section:first-of-type {
  margin-top: 8px;
}

.rules-section h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
}

.rules-section p {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #475569;
}

.rules-foot {
  margin: 20px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}

.rules-ok-btn {
  margin-top: 16px;
  width: 100%;
  padding: 12px 20px;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(90deg, #0ea5e9, #6366f1);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.35);
  transition: filter 0.15s, transform 0.15s;
}

.rules-ok-btn:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

/* ===== 胜负面板 ===== */
.victory-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  animation: fadeIn 0.3s ease;
}

.victory-modal {
  padding: 36px 48px;
  border-radius: 24px;
  background: #fff;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  animation: scaleIn 0.35s ease;
}

.victory-modal.win  { border-top: 8px solid #10b981; }
.victory-modal.lose { border-top: 8px solid #ef4444; }
.victory-modal.draw { border-top: 8px solid #64748b; }

.victory-title {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
}

.victory-modal.win  .victory-title { color: #047857; }
.victory-modal.lose .victory-title { color: #b91c1c; }
.victory-modal.draw .victory-title { color: #334155; }

.victory-sub {
  margin: 0 0 24px;
  color: #666;
  font-size: 14px;
}

.victory-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.victory-btn {
  min-width: 140px;
  padding: 10px 20px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
  border: none;
}

.victory-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.victory-btn-primary {
  color: #fff;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
}

.victory-btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.5);
}

.victory-btn-ghost {
  color: #475569;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
}

.victory-btn-ghost:hover {
  background: #e2e8f0;
  border-color: #94a3b8;
}

/* ===== 底部信息按钮 ===== */
.info-button-container {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateX(-50%) translateY(20px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.info-button {
  width: 48px; height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.info-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.info-button svg {
  width: 24px; height: 24px;
  color: white;
}

.info-hint {
  font-size: 12px; color: #666;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 12px;
  border-radius: 12px;
  backdrop-filter: blur(4px);
}

/* ===== 详情弹窗 ===== */
.detail-modal-overlay {
  position: fixed;
  inset: 0;
  padding: 16px;
  box-sizing: border-box;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.detail-modal {
  background: linear-gradient(180deg, #fafbfc 0%, #fff 48%);
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  max-height: min(90vh, 680px);
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: scaleIn 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.detail-modal--chess {
  max-width: 300px;
  padding: 0;
  max-height: none;
  background: #3d454d;
  border: 1px solid #2a3036;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.45);
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.9); }
  to   { opacity: 1; transform: scale(1); }
}

.modal-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 20px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}

.close-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover { background: #e2e8f0; }

.modal-body {
  padding: 16px 20px 22px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.grid-detail .info-row:last-child {
  border-bottom: none;
}

.mono {
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
}

/* --- 棋子信息（紧凑型） --- */
.chess-pop {
  color: #e2e8f0;
}

.chess-pop-bar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 40px;
  background: linear-gradient(180deg, #5b9bd5 0%, #3e7cc4 55%, #2d6aab 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.chess-pop-x {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: #d32f2f;
  color: #fff;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  transition: filter 0.15s;
}

.chess-pop-x:hover {
  filter: brightness(1.08);
}

.chess-pop-title {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.chess-pop-body {
  padding: 12px 14px 14px;
}

.chess-pop-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.chess-pop-name {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #f8fafc;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chess-pop-side {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
}

.chess-pop-side.sov {
  background: #15803d;
  color: #ecfccb;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.15);
}

.chess-pop-side.ger {
  background: #57534e;
  color: #fafaf9;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.chess-pop-art {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  max-height: 160px;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(160deg, #1e293b, #0f172a);
  border: 1px solid #1f2937;
}

.chess-pop-art img {
  width: 100%;
  height: 100%;
  max-height: 160px;
  object-fit: contain;
}

.chess-pop-art-fallback {
  font-size: 40px;
  font-weight: 800;
  color: #475569;
}

.chess-pop-hpstrip {
  height: 5px;
  border-radius: 3px;
  background: #1a1f24;
  margin-top: 8px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.4);
}

.chess-pop-hpfill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #16a34a, #4ade80);
  transition: width 0.25s ease;
}

.chess-pop-hpfill.low {
  background: linear-gradient(90deg, #ea580c, #dc2626);
}

.chess-pop-stats {
  display: flex;
  margin-top: 10px;
  padding: 8px 4px 10px;
  background: #2f363d;
  border-radius: 8px;
  border: 1px solid #23292f;
  justify-content: space-between;
  gap: 2px;
}

.chess-pop-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 0;
  cursor: help;
}

.chess-pop-ico {
  width: 20px;
  height: 20px;
  color: #9ca3af;
  flex-shrink: 0;
}

.chess-pop-num {
  font-size: 12px;
  font-weight: 700;
  color: #f9fafb;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.chess-pop-meta {
  margin: 10px 0 0;
  font-size: 11px;
  color: #8b9aad;
  text-align: center;
  line-height: 1.45;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child { border-bottom: none; }

.label { color: #666; font-size: 14px; }

.value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hp-bar {
  width: 60px;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.hp-fill {
  height: 100%;
  background: linear-gradient(to right, #ff4d4f, #ffa940);
  border-radius: 4px;
  transition: width 0.3s ease;
}
</style>
