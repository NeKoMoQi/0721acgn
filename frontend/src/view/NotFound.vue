<template>
  <div class="missing">
    <Headpage />
    <main>
      <h1>找不到这个页面</h1>
      <p>链接可能已经变更。可以回首页，或用作品名重新搜索。</p>
      <nav aria-label="找回内容">
        <RouterLink to="/">返回首页</RouterLink>
        <RouterLink :to="{ name: 'SiteSearch' }">搜索作品</RouterLink>
      </nav>
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
</script>

<style scoped>
.missing { min-height: 100vh; background: #eef5f0; color: #203a30; }
main { width: min(560px, calc(100% - 32px)); margin: 64px auto; padding: 24px; border-radius: 12px; background: white; }
p { margin: 12px 0 20px; }
nav { display: flex; flex-wrap: wrap; gap: 12px 24px; }
a { color: #246048; text-decoration: underline; }
</style>
