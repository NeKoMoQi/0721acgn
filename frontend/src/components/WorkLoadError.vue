<template>
  <section class="work-error" role="status">
    <h1>暂时读不到这部作品</h1>
    <p>可重试读取，或回频道找其他作品。</p>
    <button type="button" @click="$emit('retry')">重试</button>
    <RouterLink :to="catalog">返回频道</RouterLink>
    <RouterLink :to="{ name: 'SiteSearch' }">搜索作品</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
defineProps<{ catalog: string }>()
defineEmits<{ retry: [] }>()
</script>

<style scoped>
.work-error { width: min(640px, calc(100% - 32px)); margin: 40px auto; padding: 24px; border-radius: 12px; background: white; color: #254438; }
.work-error p { margin: 12px 0; }
.work-error a, .work-error button { display: inline-block; margin: 4px 12px 4px 0; color: #246048; }
.work-error a { text-decoration: underline; }
.work-error button { cursor: pointer; }
</style>
