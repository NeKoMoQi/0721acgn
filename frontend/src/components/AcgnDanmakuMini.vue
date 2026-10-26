<template>
  <div class="dm-wrap">
    <h4 class="dm-title">弹幕演示（本机）</h4>
    <p class="dm-hint">发送后从右向左飘过，数据存浏览器；非直播、无审核。</p>
    <div class="dm-track" ref="trackRef">
      <TransitionGroup name="dm-fly">
        <span
          v-for="row in flying"
          :key="row.uid"
          class="dm-line"
          :style="{ top: row.top + '%', color: row.color, animationDuration: row.dur + 's' }"
        >
          {{ row.text }}
        </span>
      </TransitionGroup>
    </div>
    <div class="dm-input-row">
      <el-input v-model="draft" size="small" maxlength="72" placeholder="发条弹幕…" @keyup.enter="send" />
      <el-button type="primary" size="small" round @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { listDanmu, pushDanmu } from '@/utils/acgnLocal'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{ streamKey: string }>()

const draft = ref('')
const flying = ref<Array<{ uid: string; text: string; color: string; top: number; dur: number }>>([])
const trackRef = ref<HTMLElement | null>(null)
let uidSeq = 0

function send() {
  const k = props.streamKey
  if (!k) return
  if (!pushDanmu(k, draft.value)) {
    ElMessage.warning('内容为空')
    return
  }
  const last = listDanmu(k).slice(-1)[0]
  draft.value = ''
  if (!last) return
  uidSeq += 1
  const row = {
    uid: `${Date.now()}_${uidSeq}`,
    text: last.text,
    color: last.color,
    top: 15 + Math.random() * 55,
    dur: 7 + Math.random() * 5,
  }
  flying.value.push(row)
  setTimeout(() => {
    flying.value = flying.value.filter((x) => x.uid !== row.uid)
  }, row.dur * 1000 + 400)
}

onMounted(() => {
  /* 预载历史最后几条视觉演示可选，略 */
})
</script>

<style scoped>
.dm-wrap {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #dde4ea;
}
.dm-title {
  margin: 0 0 4px;
  font-size: 0.95rem;
}
.dm-hint {
  margin: 0 0 10px;
  font-size: 11px;
  color: #94a3b8;
}

.dm-track {
  position: relative;
  height: 120px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.78);
  overflow: hidden;
  margin-bottom: 10px;
}

.dm-line {
  position: absolute;
  left: 100%;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  text-shadow: 0 0 3px #000;
  animation-name: dm-slide;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

@keyframes dm-slide {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-220%);
  }
}

.dm-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.dm-fly-enter-active,
.dm-fly-leave-active {
  transition: opacity 0.25s ease;
}
.dm-fly-enter-from,
.dm-fly-leave-to {
  opacity: 0;
}
</style>
