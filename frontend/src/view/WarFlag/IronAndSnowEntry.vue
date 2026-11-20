<script setup lang="ts">
import { onMounted } from 'vue'

const startUrl = `${import.meta.env.BASE_URL}iron-and-snow/start.html`

onMounted(() => window.location.replace(startUrl))
</script>

<template>
  <main><a :href="startUrl">进入铁与雪</a></main>
</template>
