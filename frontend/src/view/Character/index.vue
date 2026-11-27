<template>
  <div class="character-root">
    <Headpage style="position: sticky !important; top: 0; z-index: 10" />
    <main>
      <RouterLink class="back" to="/">← 回到首页</RouterLink>
      <div v-if="loading" class="notice" role="status">正在读取角色…</div>
      <div v-else-if="error" class="notice" role="alert">
        <p>{{ error }}</p><RouterLink to="/">回首页换一个</RouterLink>
      </div>
      <article v-else-if="character" class="card" data-character>
        <header class="portrait">
          <CharacterPortrait :image="artwork?.image" :alternate="portraitImage" :fallback="archivePortrait(archivePerson?.id, 'large')" :alt="displayName" eager />
          <h1>{{ displayName }}</h1>
          <p v-if="originalName && originalName !== displayName" class="original-name">{{ originalName }}</p>
        </header>
        <div class="copy">
          <p v-if="workTitle" class="work-title">出自《{{ workTitle }}》</p>
          <section class="intro">
            <h2>角色简介</h2>
            <p v-for="(paragraph, index) in introParagraphs" :key="index">{{ paragraph }}</p>
            <p v-if="!introParagraphs.length" class="empty" role="status">{{ synopsisNotice }}</p>
            <dl v-if="facts.size" class="archive-facts" aria-label="角色资料">
              <div v-for="[label, value] in facts" :key="label"><dt>{{ label }}</dt><dd>{{ value }}</dd></div>
            </dl>
          </section>
          <section v-if="associatedWorks.length || extraWorks.length || selectedGame" class="character-works">
            <h2>登场作品</h2>
            <ul>
              <li v-for="work in associatedWorks" :key="`${work.type}:${work.id}`">
                <RouterLink :to="workLocation(work)">{{ work.title }} · {{ workTypeLabel(work.type) }}</RouterLink>
                <small v-if="creditFor(work)" class="archive-credit">{{ creditFor(work) }}</small>
              </li>
              <li v-for="work in extraWorks" :key="`archive:${work.type}:${work.id}`">
                <RouterLink :to="archiveWorkLocation(work)">{{ work.title }} · {{ archiveKindLabels[work.type] }}</RouterLink>
                <small v-if="creditFor(work)" class="archive-credit">{{ creditFor(work) }}</small>
              </li>
              <li v-if="selectedGame">
                <RouterLink :to="{ name: 'GalgameIntroduce', query: { slug: gameSlug } }">{{ selectedGame.title || '返回 Galgame 详情' }}</RouterLink>
                <small v-if="creditFor({ type: 'galgame', id: gameSlug })" class="archive-credit">{{ creditFor({ type: 'galgame', id: gameSlug }) }}</small>
              </li>
            </ul>
          </section>
          <section v-if="supplement?.relations?.length" class="character-works">
            <h2>人物关系</h2>
            <ul class="character-relations">
              <li v-for="[label, person, ended] in supplement.relations" :key="`${person.id}:${label}`">
                <RouterLink :to="archiveCharacterLocation(person)">{{ person.name }}</RouterLink>
                <small class="archive-credit">{{ ended ? `曾为${label}` : label }}</small>
              </li>
            </ul>
          </section>
          <template v-if="tags.length">
            <h2>萌点</h2>
            <ul class="tags"><li v-for="tag in tags" :key="tag">{{ tag }}</li></ul>
          </template>
          <footer v-if="sources.length" class="sources">
            <h2>资料来源</h2>
            <p v-if="showsArchiveSynopsis" class="synopsis-credit">
              简介由 Bangumi 条目贡献者编写，本站调整了排版。该简介依
              <a href="https://creativecommons.org/licenses/by-sa/3.0/" target="_blank" rel="noopener noreferrer">CC BY-SA 3.0</a>
              提供。
            </p>
            <ul class="profile-sources">
              <li v-for="source in sources" :key="source.url">
                <a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.title }}</a>
              </li>
            </ul>
          </footer>
          <div class="actions">
            <button type="button" @click="next">换一个角色</button>
            <RouterLink :to="{ name: 'SiteSearch', query: { mediaType: 'character' } }" class="ghost">搜索角色</RouterLink>
          </div>
        </div>
      </article>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import CharacterPortrait from '@/components/CharacterPortrait.vue'
import request from '@/utils/request'
import { getCharacterProfile } from '@/data/characterProfiles'
import { editorialCharacter, workLocation, workTypeLabel, worksForCharacter } from '@/data/characterAssociations'
import { animationWorksForGalgame, galgameCharacter } from '@/data/galgameMedia'
import { archiveCanonicalProfile, archiveCharacterLocation, archiveProfile, archiveRoleLabel, archiveSupplement, archiveWorkLocation, archiveWorksForSiteCharacter, type ArchiveWork } from '@/data/characterArchive'
import { archiveBiography, archivePortrait, officialArtwork, safePortrait } from '@/data/characterPresentation'

interface Character {
  id: number | string
  name: string
  sex?: string
  birthday?: string
  talent?: string
  avatarurl?: string
  intro?: string
  introsource?: string
}

const route = useRoute()
const router = useRouter()
const character = ref<Character | null>(null)
const loading = ref(false)
const error = ref('')
let loadGeneration = 0
const imported = shallowRef<Awaited<ReturnType<typeof archiveProfile>>>()
const canonical = shallowRef<Awaited<ReturnType<typeof archiveCanonicalProfile>>>()
const supplement = shallowRef<Awaited<ReturnType<typeof archiveSupplement>>>()
const synopsisNotice = ref('简介待补')
const linkedArchiveWorks = shallowRef<ArchiveWork[]>([])
const archiveKindLabels = { animation: '动漫', comic: '漫画', novel: '轻小说', galgame: 'Galgame' }

const tags = computed(() =>
  (character.value?.talent || '').split(/[、,，]/).map((tag) => tag.trim()).filter(Boolean),
)
const gameSlug = computed(() => String(route.params.slug || ''))
const selectedGame = computed(() => galgameCharacter(gameSlug.value, String(route.params.person || '')))
const associatedWorks = computed(() => selectedGame.value ? animationWorksForGalgame(gameSlug.value) : worksForCharacter(character.value?.id))
const profile = computed(() => selectedGame.value || imported.value ? undefined : getCharacterProfile(character.value?.name, character.value?.id))
const displayName = computed(() => profile.value?.displayName || character.value?.name || '')
const archivePerson = computed(() => (imported.value ?? canonical.value)?.person)
const originalName = computed(() => selectedGame.value?.person.originalName || archivePerson.value?.originalName)
const artwork = computed(() => officialArtwork(archivePerson.value?.id, character.value?.id))
const portraitImage = computed(() => safePortrait(selectedGame.value?.person.image || character.value?.avatarurl || ''))
const archiveWorks = computed(() => (imported.value ?? canonical.value)?.works ?? linkedArchiveWorks.value)
const extraWorks = computed(() => {
  const seen = new Set(associatedWorks.value.map(work => `${work.type}:${work.id}`))
  if (selectedGame.value) seen.add(`galgame:${gameSlug.value}`)
  const works = archiveWorks.value.filter(work => {
    const key = `${work.type}:${work.id}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
  return works
})
const workTitle = computed(() => selectedGame.value?.title || profile.value?.work || associatedWorks.value[0]?.title || extraWorks.value[0]?.title)
function creditFor(work: Pick<ArchiveWork, 'type' | 'id'>) {
  const version = archiveWorks.value.find(entry => entry.type === work.type && entry.id === work.id)
  const entry = version?.cast.find(([id]) => id === archivePerson.value?.id)
  if (!entry) return ''
  const role = archiveRoleLabel(entry[1])
  const voices = entry[2]?.join('、')
  return [role, voices ? `配音 ${voices}` : ''].filter(Boolean).join(' · ')
}
const biography = computed(() => archiveBiography(archivePerson.value?.id))
const editorialParagraphs = computed(() => profile.value?.paragraphs ?? biography.value?.paragraphs ??
  (character.value?.intro || '').split(/\n{2,}/).map(paragraph => paragraph.trim()).filter(Boolean),
)
const introParagraphs = computed(() => editorialParagraphs.value.length ? editorialParagraphs.value : supplement.value?.paragraphs ?? [])
const showsArchiveSynopsis = computed(() => !editorialParagraphs.value.length && introParagraphs.value.length > 0)
const facts = computed(() => {
  const values = new Map(archivePerson.value?.facts ?? [])
  const aliases = archivePerson.value?.aliases
  if (aliases?.length) values.set('别名', aliases.join('、'))
  const birthday = character.value?.birthday || archivePerson.value?.birthday
  if (birthday) values.set('生日', birthday)
  if (character.value?.sex) values.set('性别', character.value.sex)
  return values
})
const sources = computed(() => {
  const links = [...(profile.value?.sources ?? [])]
  const game = selectedGame.value?.person
  if (game) links.push({ title: '作品官网 · 角色资料', url: game.sourceUrl })
  const legacySource = character.value?.introsource
  if (biography.value && !profile.value) links.push(biography.value.source)
  else if (!profile.value && legacySource && /^https:\/\/zh\.moegirl\.org\.cn\//.test(legacySource)) {
    links.push({ title: '萌娘百科 · CC BY-NC-SA 3.0 CN', url: legacySource })
  }
  if (artwork.value) links.push({ title: '作品官网 · 角色立绘', url: artwork.value.source })
  if (archivePerson.value) {
    const original = `https://bgm.tv/character/${archivePerson.value.id}`
    links.push({ title: 'Bangumi 番组计划 · 角色原条目', url: original })
    if (showsArchiveSynopsis.value) links.push({ title: 'Bangumi · 条目编辑页和贡献记录', url: `${original}/edit` })
  }
  return links.filter((link, index) => links.findIndex(other => other.url === link.url) === index)
})

async function load() {
  const current = ++loadGeneration
  const id = String(route.params.id || '')
  loading.value = false
  error.value = ''
  character.value = null
  imported.value = undefined
  canonical.value = undefined
  linkedArchiveWorks.value = []
  const game = selectedGame.value?.person
  const canonicalKey = game ? `${gameSlug.value}:${game.name}` : id
  if (canonicalKey && !canonicalKey.startsWith('bgm-')) {
    void archiveCanonicalProfile(canonicalKey).then(result => {
      if (current === loadGeneration) canonical.value = result
    }).catch(() => {})
  }
  if (game) {
    character.value = { id: game.name, name: game.name, intro: game.summary, avatarurl: game.image }
    return
  }
  const sourceId = /^bgm-(\d+)$/.exec(id)
  if (sourceId) {
    loading.value = true
    try {
      const result = await archiveProfile(Number(sourceId[1]))
      if (current !== loadGeneration) return
      if (!result) {
        error.value = '没有找到这个角色。'
        return
      }
      imported.value = result
      character.value = { id, name: result.person.name }
    } catch {
      if (current === loadGeneration) error.value = '角色资料暂时无法加载，请稍后重试。'
    } finally {
      if (current === loadGeneration) loading.value = false
    }
    return
  }
  if (id) {
    void archiveWorksForSiteCharacter(id).then(works => {
      if (current === loadGeneration) linkedArchiveWorks.value = works
    }).catch(() => {})
  }
  const editorial = editorialCharacter(id)
  if (editorial) {
    character.value = { id, name: editorial.name }
    return
  }
  if (!/^\d+$/.test(id)) {
    error.value = '没有找到这个角色。'
    return
  }
  loading.value = true
  try {
    const result: any = await request.get('/acgn/user/byid', { params: { id } })
    if (result?.code !== 200 || !result.data) throw new Error('empty')
    if (current === loadGeneration) character.value = result.data
  } catch {
    if (current === loadGeneration) error.value = '这个角色暂时读不到，可能是编号不存在。'
  } finally {
    if (current === loadGeneration) loading.value = false
  }
}

async function next() {
  try {
    const result: any = await request.get('/acgn/user')
    const id = result?.data?.id
    if (id) void router.push(`/character/${id}`)
  } catch {
    // 随机接口失败就停在当前页，不弹错误。
  }
}

watch(() => [route.params.id, route.params.slug, route.params.person], () => { void load() }, { immediate: true })
watch(() => [Boolean(character.value), archivePerson.value?.id] as const,
  ([hasCharacter, id], _, onCleanup) => {
    supplement.value = undefined
    synopsisNotice.value = '简介待补'
    if (!hasCharacter || !id) return
    let current = true
    onCleanup(() => { current = false })
    synopsisNotice.value = '正在读取简介…'
    void archiveSupplement(id).then(details => {
      if (!current) return
      supplement.value = details
      synopsisNotice.value = '简介待补'
    }).catch(() => {
      if (current) synopsisNotice.value = '简介暂时无法读取'
    })
  }, { flush: 'sync' })
</script>

<style scoped>
.original-name { margin: 0; color: #526f64; }
.character-works { margin: 20px 0; }
.character-works ul { padding-left: 18px; }
.character-works a { color: #246048; text-decoration: underline; }
.character-relations li { margin: 4px 0; }
.work-title { margin: 0 0 20px; color: #506e62; }
.archive-facts div { display: flex; gap: 10px; margin: 6px 0; }
.archive-facts dt { font-weight: 600; flex-shrink: 0; }
.archive-facts dd { margin: 0; }
.archive-credit { margin-left: 8px; color: #506e62; }
.profile-sources { padding-left: 18px; font-size: 14px; }
.profile-sources a { color: #246048; }
.sources { margin: 24px 0; padding-top: 14px; border-top: 1px solid #dcebe3; }
.synopsis-credit { color: #506e62; font-size: 14px; line-height: 1.7; }
.synopsis-credit a { color: #246048; }
.profile-sources li { margin: 4px 0; overflow-wrap: anywhere; }
.character-root {
  min-height: 100vh;
  background: #f5f7f5;
  color: #213a33;
}
main {
  max-width: 980px;
  margin: 0 auto;
  padding: 48px 24px 80px;
}
.back {
  display: inline-block;
  margin-bottom: 24px;
  color: #477b67;
  text-decoration: none;
}
.notice {
  background: #fff;
  padding: 48px;
  border-radius: 16px;
  text-align: center;
  color: #70817c;
}
.notice a {
  color: #328566;
}
.card {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 36px;
  background: #fff;
  border-radius: 18px;
  padding: 36px;
  box-shadow: 0 8px 28px #203c3214;
}
.portrait { align-self: start; text-align: center; }
.portrait h1 {
  font-size: clamp(26px, 3vw, 38px);
  margin: 12px 0 4px;
  overflow-wrap: anywhere;
}
.copy h2 {
  font-size: 15px;
  color: #57736a;
  margin: 0 0 12px;
  font-weight: 600;
}
.intro {
  margin-bottom: 26px;
}
.intro p {
  margin: 0 0 14px;
  font-size: 15px;
  line-height: 1.95;
  color: #3d5349;
  text-align: justify;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  list-style: none;
  padding: 0;
  margin: 0 0 28px;
}
.tags li {
  background: #eef6f2;
  color: #356e59;
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 14px;
  line-height: 1.3;
}
.intro .empty { color: #5c7268; }
.actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
button {
  border: 1px solid #c9dbd1;
  background: #fff;
  color: #356e59;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
}
button:hover {
  background: #f2f8f5;
}
.ghost {
  color: #328566;
  text-decoration: none;
  font-size: 14px;
}
@media (max-width: 760px) {
  .card {
    grid-template-columns: 1fr;
    padding: 24px;
    gap: 24px;
  }
  .portrait { width: min(240px, 100%); margin-inline: auto; }
}
</style>
