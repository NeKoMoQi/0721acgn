<template>
  <section v-if="people.length" class="work-characters" aria-label="登场人物">
    <h2>登场人物</h2>
    <ul>
      <li v-for="person in people" :key="person.id">
        <RouterLink :to="{ name: 'Character', params: { id: person.id } }">{{ person.name }} <span aria-hidden="true">→</span></RouterLink>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { castForWork, type CharacterWork } from '@/data/characterAssociations'

const props = defineProps<{ type: CharacterWork['type']; id: number }>()
const people = computed(() => castForWork(props.type, props.id))
</script>

<style scoped>
.work-characters { margin: 20px 0; padding: 18px; border: 1px solid #dcebe3; border-radius: 12px; background: #f7fbf8; }
h2 { margin: 0 0 10px; color: #28483b; font-size: 18px; }
ul { display: flex; flex-wrap: wrap; gap: 8px; padding: 0; margin: 0; list-style: none; }
a { display: inline-block; padding: 8px 12px; border: 1px solid #c8dfd1; border-radius: 8px; color: #235c44; background: #fff; text-decoration: none; }
a:hover { text-decoration: underline; }
</style>
