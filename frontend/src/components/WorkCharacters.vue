<template>
  <section v-if="entries.length" class="work-characters" aria-label="登场人物">
    <h2>{{ type === 'galgame' && gamePeople.length ? '更多登场人物' : '登场人物' }}</h2>
    <ul>
      <li v-for="entry in visible" :key="entry.key">
        <RouterLink :to="entry.to">{{ entry.name }} <span aria-hidden="true">→</span></RouterLink>
      </li>
    </ul>
    <button v-if="entries.length > previewCount" type="button" :aria-expanded="expanded" @click="expanded = !expanded">
      {{ expanded ? '收起' : `查看全部 ${entries.length} 位角色` }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { castForWork } from '@/data/characterAssociations'
import { galgameForAnimation, mediaFor } from '@/data/galgameMedia'
import { archiveCast, archiveCharacterLocation, type ArchiveWorkType } from '@/data/characterArchive'
import { galgameCharacterLocation } from '@/utils/searchResultLocation'

const props = defineProps<{ type: ArchiveWorkType; id: number | string }>()
const people = computed(() => props.type === 'galgame' ? [] : castForWork(props.type, Number(props.id)))
const gameSlug = computed(() => {
  if (props.type === 'galgame') return String(props.id)
  return props.type === 'animation' ? galgameForAnimation(Number(props.id)) || '' : ''
})
const gamePeople = computed(() => mediaFor(gameSlug.value)?.characters ?? [])
const imported = shallowRef<Awaited<ReturnType<typeof archiveCast>>>([])
const expanded = ref(false)
const previewCount = 12

watch(() => [props.type, props.id] as const, ([type, id], _, onCleanup) => {
  let current = true
  onCleanup(() => { current = false })
  imported.value = []
  expanded.value = false
  void archiveCast(type, id).then(value => {
    if (current) imported.value = value
  }).catch(() => {})
}, { immediate: true })

const entries = computed(() => {
  const keys = new Set(people.value.map(person => String(person.id)))
  const currentGameNames = new Set(gamePeople.value.map(person => person.name))
  const names = new Set([
    ...people.value.flatMap(person => [person.name, ...(person.aliases ?? [])]),
    ...gamePeople.value.flatMap(person => [person.name, person.originalName]),
  ].filter(Boolean))
  const earlier = people.value.map(person => ({
    key: String(person.id), name: person.name, to: { name: 'Character', params: { id: person.id } },
  }))
  const gallery = props.type === 'galgame' ? [] : gamePeople.value.map(person => ({
    key: `${gameSlug.value}:${person.name}`, name: person.name, to: galgameCharacterLocation(gameSlug.value, person.name),
  }))
  const more = imported.value.filter(({ person }) =>
    !(person.siteId != null && keys.has(String(person.siteId))) &&
    !(person.game?.[0] === gameSlug.value && currentGameNames.has(person.game[1])) &&
    ![person.name, person.originalName].some(name => names.has(name)))
    .map(({ person }) => ({ key: `bgm-${person.id}`, name: person.name, to: archiveCharacterLocation(person) }))
  return [...earlier, ...gallery, ...more]
})
const visible = computed(() => expanded.value ? entries.value : entries.value.slice(0, previewCount))
</script>

<style scoped>
.work-characters { margin: 20px 0; padding: 18px; border: 1px solid #dcebe3; border-radius: 12px; background: #f7fbf8; }
h2 { margin: 0 0 10px; color: #28483b; font-size: 18px; }
ul { display: flex; flex-wrap: wrap; gap: 8px; padding: 0; margin: 0; list-style: none; }
a, button { display: inline-block; padding: 8px 12px; border: 1px solid #c8dfd1; border-radius: 8px; color: #235c44; background: #fff; text-decoration: none; }
a:hover, button:hover { text-decoration: underline; }
button { margin-top: 12px; cursor: pointer; }
</style>
