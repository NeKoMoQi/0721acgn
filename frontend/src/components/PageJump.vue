<template>
  <form class="page-jump" novalidate @submit.prevent="go">
    <input
      v-model="input"
      :aria-label="label"
      :aria-invalid="error ? 'true' : undefined"
      :placeholder="placeholder"
      :disabled="busy"
      title="输入数字后按回车跳转"
      inputmode="numeric"
      enterkeyhint="go"
      @input="error = ''"
    />
    <span v-if="error" role="status">{{ error }}</span>
  </form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
const props = withDefaults(defineProps<{ page: number; max?: number; busy?: boolean; label?: string; placeholder?: string }>(), {
  label: '目标页码',
  placeholder: '跳页',
})
const emit = defineEmits<{ jump: [page: number] }>()
const input = ref('')
const error = ref('')
watch(() => props.page, () => { input.value = ''; error.value = '' })
function go() {
  if (props.busy) return
  const target = Number(input.value)
  if (!/^[0-9]+$/.test(input.value.trim()) || !Number.isSafeInteger(target) || target < 1 || target > (props.max ?? 10000)) {
    error.value = props.max ? `请输入 1–${props.max} 之间的页码` : '请输入有效的页码'
    return
  }
  error.value = ''
  if (target !== props.page) emit('jump', target)
}
</script>

<style scoped>
.page-jump { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 6px; vertical-align: middle; }
input { box-sizing: border-box; width: 4.25rem; min-height: 36px; padding: 4px 6px; border: 1px solid #a0bfb1; border-radius: 6px; background: #fff; color: #254b3a; font: inherit; font-size: .875rem; text-align: center; }
span { font-size: .875rem; }
</style>
