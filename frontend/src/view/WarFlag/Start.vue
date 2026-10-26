<template>
  <Headpage />
  <div class="war-start-page" :style="{ '--war-start-bg': `url(${START_BG_URL})` }">
    <div class="war-start-panel">
      <h1 class="war-title">战旗推演 ✧</h1>
      <p class="war-sub">
        库尔斯克六角格战场～(≧ω≦)ﾉ 摧毁对方司令部即胜利！
      </p>

      <div class="action-row">
        <button
          type="button"
          class="btn btn-continue"
          :disabled="!hasSave"
          :title="hasSave ? '接着上次的进度' : '还没有在本机选过阵营进过战场哦'"
          @click="continueGame"
        >
          <span class="btn-label">继续游戏</span>
          <span class="btn-hint">{{ hasSave ? '接着上次的阵营与进度' : '暂无本地记录' }}</span>
        </button>
        <button
          type="button"
          class="btn btn-new"
          :class="{ active: flow === 'new' }"
          @click="flow = 'new'"
        >
          <span class="btn-label">开始新游戏</span>
          <span class="btn-hint">重置为初始部署后入场</span>
        </button>
      </div>

      <transition name="fade">
        <div v-if="flow === 'new'" class="new-game-block">
          <p class="block-title">请选择扮演阵营</p>
          <div class="faction-cards">
            <button
              type="button"
              class="faction-card"
              :class="{ selected: picked === 'soviet' }"
              @click="picked = 'soviet'"
            >
              <span class="emoji">☆</span>
              <h2>沃罗涅日方面军</h2>
              <p>苏军 · 玩家先手 · 东侧</p>
            </button>
            <button
              type="button"
              class="faction-card"
              :class="{ selected: picked === 'german' }"
              @click="picked = 'german'"
            >
              <span class="emoji">✦</span>
              <h2>中央集团军群</h2>
              <p>德军 · 玩家先手 · 西侧</p>
            </button>
          </div>
          <button
            type="button"
            class="btn-submit"
            :disabled="!picked || resetting"
            @click="startNewGame"
          >
            {{ resetting ? '正在重置战场…' : '确认开局（重置棋盘）' }}
          </button>
        </div>
      </transition>

      <p class="war-tip">提示：「继续」不会清空棋子；「新游戏」会调用服务器备份还原全场。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import { resetBattle } from '@/api/warflag/warChessing/index'

const LS_KEY = 'warflag:lastSide'
/** 本机记录的关卡 id，与战场页保持一致时可扩展 */
const BATTLE_ID = 1001

/** 开始页背景（宽幅插画，人物在两侧，中央留给 UI） */
const START_BG_URL =
  'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/zqbj.png'

const router = useRouter()
const picked = ref(null)
const flow = ref(null)
const hasSave = ref(false)
const resetting = ref(false)

onMounted(() => {
  hasSave.value = !!localStorage.getItem(LS_KEY)
})

function continueGame() {
  const side = localStorage.getItem(LS_KEY)
  if (!side) return
  router.push({ path: '/warflag/battle', query: { side } })
}

async function startNewGame() {
  if (!picked.value || resetting.value) return
  const humanPlayer = picked.value === 'german' ? 1 : 0
  resetting.value = true
  try {
    const res = await resetBattle(BATTLE_ID, humanPlayer)
    if (res?.code !== 200) {
      window.alert(res?.msg || '重置失败，请检查后端与数据库')
      return
    }
    localStorage.setItem(LS_KEY, picked.value)
    await router.push({ path: '/warflag/battle', query: { side: picked.value } })
  } catch (e) {
    console.error(e)
    window.alert('重置请求失败')
  } finally {
    resetting.value = false
  }
}
</script>

<style scoped>
.war-start-page {
  --war-start-bg: none;
  position: relative;
  margin-top: 80px;
  min-height: calc(100vh - 80px);
  box-sizing: border-box;
  padding: 24px 20px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  background-color: #1a1520;
  background-image: var(--war-start-bg);
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  overflow-x: hidden;
}

/* 压暗四周、略提亮中央，让中间操作区与插画里的「空地」对齐 */
.war-start-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background: radial-gradient(
    ellipse 75% 85% at 50% 45%,
    rgba(18, 14, 22, 0.2) 0%,
    rgba(18, 14, 22, 0.52) 62%,
    rgba(12, 10, 18, 0.68) 100%
  );
}

.war-start-page > .war-start-panel {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: min(720px, 94vw);
  margin: 0 auto;
  padding: clamp(28px, 4vw, 40px) clamp(24px, 4vw, 36px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: saturate(150%) blur(18px);
  -webkit-backdrop-filter: saturate(150%) blur(18px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.12),
    0 24px 56px rgba(20, 10, 35, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.war-title {
  margin: 0 0 12px;
  font-family: 'Hachi Maru Pop', cursive, 'Comic Sans MS', 幼圆, sans-serif;
  font-size: clamp(24px, 4vw, 32px);
  text-align: center;
  background: linear-gradient(45deg, #ff99cc, #ff69b4);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
}

.war-sub {
  margin: 0 0 28px;
  text-align: center;
  font-size: 15px;
  color: #4a4d5c;
  line-height: 1.65;
  max-width: 42em;
  margin-left: auto;
  margin-right: auto;
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 8px;
}

@media (max-width: 560px) {
  .action-row {
    grid-template-columns: 1fr;
  }
}

.btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 18px 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
  border: 2px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 174, 236, 0.15);
  border-color: #00aeec;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-new.active {
  border-color: #fb7299;
  background: #fff5f8;
  box-shadow: 0 0 0 2px rgba(251, 114, 153, 0.2);
}

.btn-label {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.btn-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.new-game-block {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px dashed #e0e0e0;
}

.block-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  text-align: center;
}

.faction-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

@media (max-width: 520px) {
  .faction-cards {
    grid-template-columns: 1fr;
  }
}

.faction-card {
  padding: 20px 16px;
  border-radius: 12px;
  border: 2px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
  text-align: center;
}

.faction-card:hover {
  border-color: #00aeec;
}

.faction-card.selected {
  border-color: #00aeec;
  box-shadow: 0 4px 16px rgba(0, 174, 236, 0.2);
}

.faction-card .emoji {
  display: block;
  font-size: 20px;
  color: #fb7299;
  margin-bottom: 8px;
}

.faction-card h2 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.faction-card p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.btn-submit {
  width: 100%;
  padding: 14px 20px;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(90deg, #fb7299, #ff8eb4);
  box-shadow: 0 6px 20px rgba(251, 114, 153, 0.35);
  transition: filter 0.15s, transform 0.15s;
}

.btn-submit:hover:not(:disabled) {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.btn-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.war-tip {
  margin: 24px 0 0;
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  line-height: 1.5;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
