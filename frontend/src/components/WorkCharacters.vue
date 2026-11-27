<template>
  <section v-if="entries.length" class="work-characters" aria-label="登场人物">
    <h2>登场人物</h2>
    <ul>
      <li v-for="entry in visible" :key="entry.key">
        <RouterLink :to="entry.to" class="cast-card">
          <CharacterPortrait :image="entry.image" :alternate="entry.alternate" :fallback="entry.fallback" />
          <span>{{ entry.name }}</span>
        </RouterLink>
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
import CharacterPortrait from './CharacterPortrait.vue'
import { castForWork } from '@/data/characterAssociations'
import { archivePortrait, officialArtwork } from '@/data/characterPresentation'
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
  const siteArchive = new Map(imported.value.filter(({ person }) => person.siteId != null)
    .map(entry => [String(entry.person.siteId), entry]))
  const gameArchive = new Map(imported.value.filter(({ person }) => person.game?.[0] === gameSlug.value)
    .map(entry => [entry.person.game![1], entry]))
  const currentGameNames = new Set(gamePeople.value.map(person => person.name))
  const names = new Set([
    ...people.value.flatMap(person => [person.name, ...(person.aliases ?? [])]),
    ...gamePeople.value.flatMap(person => [person.name, person.originalName]),
  ].filter(Boolean))
  const earlier = people.value.map(person => {
    const matched = siteArchive.get(String(person.id))
    return {
      key: String(person.id), name: person.name, to: { name: 'Character', params: { id: person.id } },
      image: officialArtwork(matched?.person.id, person.id)?.image,
      alternate: undefined, fallback: archivePortrait(matched?.person.id), role: matched?.role ?? 2,
    }
  })
  const gallery = gamePeople.value.map(person => {
    const matched = gameArchive.get(person.name)
    return {
      key: `${gameSlug.value}:${person.name}`, name: person.name, to: galgameCharacterLocation(gameSlug.value, person.name),
      image: officialArtwork(matched?.person.id)?.image, alternate: person.image,
      fallback: archivePortrait(matched?.person.id), role: matched?.role ?? 2,
    }
  })
  const more = imported.value.filter(({ person }) =>
    !(person.siteId != null && people.value.some(entry => String(entry.id) === String(person.siteId))) &&
    !(person.game?.[0] === gameSlug.value && currentGameNames.has(person.game[1])) &&
    ![person.name, person.originalName].some(name => names.has(name)))
    .map(({ person, role }) => ({
      key: `bgm-${person.id}`, name: person.name, to: archiveCharacterLocation(person),
      image: officialArtwork(person.id, person.siteId)?.image, alternate: undefined, fallback: archivePortrait(person.id), role,
    }))
  const priority = (role: number) => role === 1 ? 0 : role === 3 ? 2 : 1
  return [...earlier, ...gallery, ...more].sort((a, b) => priority(a.role) - priority(b.role))
})
const visible = computed(() => expanded.value ? entries.value : entries.value.slice(0, previewCount))
</script>

<style scoped>
.work-characters { margin: 20px 0; padding: 18px; border: 1px solid #dcebe3; border-radius: 12px; background: #f7fbf8; }
h2 { margin: 0 0 10px; color: #28483b; font-size: 18px; }
ul { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 116px), 1fr)); gap: 12px; padding: 0; margin: 0; list-style: none; }
.cast-card { display: grid; height: 100%; align-content: start; gap: 8px; padding: 5px 5px 9px; border: 1px solid #dcebe3; border-radius: 12px; color: #235c44; background: #fff; text-align: center; text-decoration: none; overflow-wrap: anywhere; }
.cast-card > span:last-child { padding-inline: 4px; }
.cast-card:hover, button:hover { text-decoration: underline; }
.cast-card:focus-visible, button:focus-visible { outline: 2px solid #235c44; outline-offset: 2px; }
button { margin-top: 12px; padding: 8px 12px; border: 1px solid #c8dfd1; border-radius: 8px; color: #235c44; background: #fff; cursor: pointer; }
@media (max-width: 480px) {
  .work-characters { padding: 12px; }
  ul { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
