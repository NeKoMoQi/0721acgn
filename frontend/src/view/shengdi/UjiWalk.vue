<template>
  <div class="uji-page">
    <Headpage />
    <main>
      <nav class="uji-actions" aria-label="宇治巡礼导航">
        <RouterLink :to="ujiWorkLink">← 作品详情</RouterLink>
        <RouterLink :to="ujiMapLink">在地图上打开步行路线</RouterLink>
      </nav>
      <h1>吹响吧！上低音号 · 宇治半日巡礼</h1>
      <p>从宇治桥西侧出发，沿宇治川步行到大吉山。四个停靠点都可以打开站内截图对照，大吉山最后一段需要上坡。</p>
      <ol class="uji-stops">
        <li v-for="stop in ujiStops" :key="stop.id">
          <h2>{{ stop.name }}</h2>
          <p class="uji-episode">第一季第 {{ stop.ep }} 集</p>
          <p>{{ stop.note }}</p>
          <RouterLink :to="ujiStopLink(stop.id)">查看场景对照</RouterLink>
        </li>
      </ol>
      <p class="uji-sources">
        点位与集数按站内资料核对，路线参考资料核对日期 {{ UJI_SOURCES_CHECKED }}。
        <a v-for="source in ujiSources" :key="source.url" :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.title }}</a>
      </p>
    </main>
    <Behind />
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import { UJI_SOURCES_CHECKED, ujiSources, ujiMapLink, ujiStopLink, ujiStops, ujiWorkLink } from '@/data/ujiPilgrimage'
</script>

<style scoped>
.uji-page { min-height: 100vh; background: #eef5f0; color: #203a30; }
main { max-width: 860px; margin: auto; padding: 40px 22px 64px; }
h1 { margin: 24px 0 12px; font-size: clamp(26px, 5vw, 42px); }
p { line-height: 1.65; }
.uji-actions { display: flex; flex-wrap: wrap; gap: 12px 24px; }
.uji-stops { display: grid; gap: 12px; padding-left: 38px; margin: 24px 0; }
.uji-stops li { padding: 16px 20px; border-radius: 12px; background: #fff; }
.uji-stops h2 { font-size: 21px; margin: 0 0 6px; }
.uji-episode, .uji-sources { color: #4c655c; font-size: 14px; }
.uji-sources a { display: inline-block; margin: 0 12px 4px 0; }
a { color: #246048; text-decoration: underline; text-underline-offset: 3px; }
</style>
