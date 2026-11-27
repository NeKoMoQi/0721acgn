<template>
  <span class="character-portrait">
    <img v-if="src" :key="src" :src="src" :alt="alt" :loading="eager ? 'eager' : 'lazy'"
      decoding="async" referrerpolicy="no-referrer" @error="markFailed">
    <span v-else class="empty-portrait" aria-hidden="true">暂无图片</span>
  </span>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'

const props = withDefaults(defineProps<{ image?: string; alternate?: string; fallback?: string; alt?: string; eager?: boolean }>(), { alt: '' })
const failed = reactive(new Set<string>())
const src = computed(() => [props.image, props.alternate, props.fallback].find(url => url && !failed.has(url)))
function markFailed(event: Event) {
  const url = (event.currentTarget as HTMLImageElement).getAttribute('src')
  if (url) failed.add(url)
}
</script>

<style scoped>
.character-portrait { position: relative; display: grid; width: 100%; aspect-ratio: 3 / 4; place-items: center; overflow: hidden; background: #edf3ef; border-radius: 12px; }
img { position: absolute; inset: 0; display: block; width: 100%; height: 100%; object-fit: contain; object-position: center; }
.empty-portrait { color: #586d63; font-size: .85rem; }
</style>
