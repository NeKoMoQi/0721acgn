<template>
  <div class="media-resource">
    <span v-if="!reportOnly" class="resource-label" :class="{ ready: !unavailable && end && end > 0 }">
      {{ unavailable ? '暂不可用' : resourceLabel(type, end) }}
    </span>
    <button type="button" class="report-toggle" :aria-expanded="open" @click="open = !open">资源报错</button>
    <form v-if="open" class="report-form" @submit.prevent="send">
      <label>问题
        <select v-model="reason">
          <option value="unavailable">无法播放或阅读</option>
          <option value="missing">缺集、缺章或缺页</option>
          <option value="incorrect">作品资料有误</option>
        </select>
      </label>
      <label>集、章或页码
        <input v-model="location" maxlength="80" placeholder="例如第 4 章第 12 页" />
      </label>
      <label>补充说明
        <textarea v-model="detail" maxlength="500" rows="2" />
      </label>
      <button type="submit" :disabled="busy">{{ busy ? '提交中…' : '提交报错' }}</button>
    </form>
    <p v-if="feedback" class="report-feedback" role="status">{{ feedback }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '@/utils/request'
import type { AcgnMediaType } from '@/utils/acgnLocal'
import { resourceLabel } from '@/utils/mediaAvailability'

const props = defineProps<{
  type: AcgnMediaType
  id: number
  end?: number | null
  position?: string
  reportOnly?: boolean
  unavailable?: boolean
}>()
const open = ref(false)
const busy = ref(false)
const reason = ref('unavailable')
const location = ref('')
const detail = ref('')
const feedback = ref('')
watch(open, (show) => {
  if (show) {
    location.value = props.position || ''
    feedback.value = ''
  }
})
async function send() {
  if (busy.value) return
  busy.value = true
  feedback.value = ''
  try {
    const response = await request.post<unknown, { code: number }>('/media/report', {
      mediaType: props.type, mediaId: props.id, reason: reason.value,
      position: location.value, detail: detail.value,
    })
    if (response?.code !== 200) throw new Error('报错提交失败')
    open.value = false
    detail.value = ''
    feedback.value = '报错已提交。'
  } catch {
    feedback.value = '报错提交失败，请重试。'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.media-resource { margin: 12px 0; color: #283f35; }
.resource-label { display: inline-block; margin: 0 8px 6px 0; padding: 4px 10px; border-radius: 12px; background: #edf1ef; font-size: 14px; }
.resource-label.ready { background: #def4e7; color: #215b43; }
.report-toggle, .report-form button { padding: 6px 10px; border: 1px solid #a9c3b4; border-radius: 6px; background: #fff; color: #225743; cursor: pointer; }
.report-form { display: grid; gap: 8px; max-width: 420px; padding: 12px; margin-block: 8px; background: #f4f9f6; border-radius: 8px; }
.report-form label { display: grid; gap: 4px; font-size: 14px; }
.report-form input, .report-form select, .report-form textarea { width: 100%; padding: 7px; border: 1px solid #a9c3b4; border-radius: 5px; color: #283f35; background: white; }
.report-form button { justify-self: start; }
.report-feedback { margin: 6px 0; font-size: 14px; }
</style>
