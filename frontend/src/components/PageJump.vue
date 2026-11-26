<template>
  <form class="page-jump" @submit.prevent="go">
    <label><span>{{ label === '目标页码' ? `第 ${page} 页` : label }}</span>
      <input v-model="input" :aria-label="label" inputmode="numeric" pattern="[0-9]+" :placeholder="String(page)" :disabled="busy" required>
    </label>
    <button type="submit" :disabled="busy">跳转</button>
    <span v-if="error" role="status">{{ error }}</span>
  </form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
const props = withDefaults(defineProps<{ page: number; max?: number; busy?: boolean; label?: string }>(), { label: '目标页码' })
const emit = defineEmits<{ jump: [page: number] }>()
const input = ref('')
const error = ref('')
watch(() => props.page, () => { input.value = ''; error.value = '' })
function go() {
  const target = Number(input.value)
  if (!Number.isSafeInteger(target) || target < 1 || target > (props.max ?? 10000)) {
    error.value = props.max ? `请输入 1–${props.max} 之间的页码` : '请输入有效的页码'
    return
  }
  error.value = ''
  if (target !== props.page) emit('jump', target)
}
</script>

<style scoped>
.page-jump, label { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 6px; }
input, button { min-height: 40px; padding: 3px 8px; border: 1px solid #a0bfb1; border-radius: 6px; background: #fff; color: #254b3a; }
input { width: 4.5rem; }
span { font-size: .875rem; }
</style>
