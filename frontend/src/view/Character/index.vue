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
        <div class="portrait">
          <img
            v-if="avatarOk && safeAvatar(character.avatarurl)"
            :src="safeAvatar(character.avatarurl)"
            :alt="displayName"
            @error="avatarOk = false"
          />
          <div v-else class="portrait-fallback">{{ displayName }}</div>
        </div>
        <div class="copy">
          <span class="eyebrow">0721 ACGN · 角色介绍</span>
          <h1>{{ displayName }}</h1>
          <p v-if="workTitle" class="work-title">出自《{{ workTitle }}》</p>
          <p v-if="selectedGame?.person.originalName && selectedGame.person.originalName !== character.name" class="original-name">{{ selectedGame.person.originalName }}</p>
          <dl v-if="!selectedGame && (character.sex || character.birthday)" class="facts">
            <div><dt>性别</dt><dd>{{ character.sex || '未填写' }}</dd></div>
            <div><dt>生日</dt><dd>{{ character.birthday || '未填写' }}</dd></div>
          </dl>
          <section v-if="introParagraphs.length" class="intro">
            <h2>简介</h2>
            <p v-for="(paragraph, index) in introParagraphs" :key="index">{{ paragraph }}</p>
            <ul v-if="profile" class="profile-sources" aria-label="角色资料来源">
              <li v-for="source in profile.sources" :key="source.url">
                <a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.title }}</a>
              </li>
            </ul>
            <small v-else-if="selectedGame">
              <a :href="selectedGame.person.sourceUrl" target="_blank" rel="noopener noreferrer">角色资料来源</a>
            </small>
            <small v-else-if="sourceUrl">
              内容摘自
              <a :href="sourceUrl" target="_blank" rel="noopener noreferrer">萌娘百科</a>
              ，依 CC BY-NC-SA 3.0 CN 使用。
            </small>
          </section>
          <section v-else-if="imported" class="intro">
            <h2>简介</h2>
            <p>已收录 {{ imported.works.length }} 个登场作品。各版本的戏份和配音资料见下方。</p>
            <dl v-if="imported.person.originalName || imported.person.birthday || imported.person.aliases?.length || imported.person.facts?.length" class="archive-facts">
              <div v-if="imported.person.originalName"><dt>原名</dt><dd>{{ imported.person.originalName }}</dd></div>
              <div v-if="imported.person.birthday"><dt>生日</dt><dd>{{ imported.person.birthday }}</dd></div>
              <div v-if="imported.person.aliases?.length"><dt>别名</dt><dd>{{ imported.person.aliases.join('、') }}</dd></div>
              <div v-for="[label, value] in imported.person.facts" :key="label"><dt>{{ label }}</dt><dd>{{ value }}</dd></div>
            </dl>
            <ul class="profile-sources" aria-label="角色资料来源">
              <li><a :href="`https://bgm.tv/character/${imported.person.id}`" target="_blank" rel="noopener noreferrer">Bangumi 番组计划 · 角色资料</a></li>
            </ul>
          </section>
          <p v-else class="empty">这个角色还没有录入简介。</p>
          <section v-if="associatedWorks.length || extraWorks.length || selectedGame" class="character-works">
            <h2>登场作品</h2>
            <ul>
              <li v-for="work in associatedWorks" :key="`${work.type}:${work.id}`">
                <RouterLink :to="workLocation(work)">{{ work.title }} · {{ workTypeLabel(work.type) }}</RouterLink>
              </li>
              <li v-for="work in extraWorks" :key="`archive:${work.type}:${work.id}`">
                <RouterLink :to="archiveWorkLocation(work)">{{ work.title }} · {{ archiveKindLabels[work.type] }}</RouterLink>
                <small v-if="creditFor(work)" class="archive-credit">{{ creditFor(work) }}</small>
              </li>
              <li v-if="selectedGame">
                <RouterLink :to="{ name: 'GalgameIntroduce', query: { slug: gameSlug } }">{{ selectedGame.title || '返回 Galgame 详情' }}</RouterLink>
              </li>
            </ul>
          </section>
          <template v-if="tags.length">
            <h2>萌点</h2>
            <ul class="tags"><li v-for="tag in tags" :key="tag">{{ tag }}</li></ul>
          </template>
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
import request from '@/utils/request'
import { getCharacterProfile } from '@/data/characterProfiles'
import { editorialCharacter, workLocation, workTypeLabel, worksForCharacter } from '@/data/characterAssociations'
import { animationWorksForGalgame, galgameCharacter } from '@/data/galgameMedia'
import { archiveProfile, archiveRoleLabel, archiveWorkLocation, archiveWorksForSiteCharacter, type ArchiveWork } from '@/data/characterArchive'

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
const avatarOk = ref(true)
let loadGeneration = 0
const imported = shallowRef<Awaited<ReturnType<typeof archiveProfile>>>()
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
const extraWorks = computed(() => {
  const seen = new Set(associatedWorks.value.map(work => `${work.type}:${work.id}`))
  const works = (imported.value?.works ?? linkedArchiveWorks.value).filter(work => {
    const key = `${work.type}:${work.id}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
  return works
})
const workTitle = computed(() => selectedGame.value?.title || profile.value?.work || associatedWorks.value[0]?.title || extraWorks.value[0]?.title)
function creditFor(work: ArchiveWork) {
  const entry = work.cast.find(([id]) => id === imported.value?.person.id)
  if (!entry) return ''
  const role = archiveRoleLabel(entry[1])
  const voices = entry[2]?.join('、')
  return [role, voices ? `配音 ${voices}` : ''].filter(Boolean).join(' · ')
}
const introParagraphs = computed(() => profile.value?.paragraphs ??
  (character.value?.intro || '').split(/\n{2,}/).map(paragraph => paragraph.trim()).filter(Boolean),
)
const sourceUrl = computed(() =>
  /^https:\/\/zh\.moegirl\.org\.cn\//.test(character.value?.introsource || '') ? character.value?.introsource : '',
)
// 头像只接受站内路径或 https，避免把外部字段直接塞进 img。
const safeAvatar = (url = '') => /^\/(?!\/)/.test(url || '') || /^https:\/\//i.test(url || '') ? url : ''

async function load() {
  const current = ++loadGeneration
  const id = String(route.params.id || '')
  loading.value = false
  error.value = ''
  character.value = null
  avatarOk.value = true
  imported.value = undefined
  linkedArchiveWorks.value = []
  const game = selectedGame.value?.person
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
</script>

<style scoped>
.original-name { color: #526f64; }
.character-works { margin: 20px 0; }
.character-works ul { padding-left: 18px; }
.character-works a { color: #246048; text-decoration: underline; }
.work-title { margin: -12px 0 20px; color: #506e62; }
.archive-facts div { display: flex; gap: 10px; margin: 6px 0; }
.archive-facts dt { font-weight: 600; flex-shrink: 0; }
.archive-facts dd { margin: 0; }
.archive-credit { margin-left: 8px; color: #506e62; }
.profile-sources { padding-left: 18px; font-size: 14px; }
.profile-sources a { color: #246048; }
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
.portrait img,
.portrait-fallback {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 16px;
  object-fit: cover;
  display: block;
  background: #e2efe9;
}
.portrait-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
  color: #35564b;
  font-size: 18px;
  text-align: center;
}
.eyebrow {
  font-size: 12px;
  letter-spacing: .08em;
  color: #658078;
}
.copy h1 {
  font-size: clamp(26px, 3vw, 38px);
  margin: 12px 0 22px;
}
.facts {
  display: flex;
  gap: 48px;
  margin: 0 0 26px;
}
.facts div {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.facts dt {
  color: #728a80;
  font-size: 14px;
}
.facts dd {
  margin: 0;
  font-size: 17px;
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
.intro small {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: #93a49d;
}
.intro small a {
  color: #328566;
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
.empty {
  color: #8a9a93;
  margin: 0 0 28px;
}
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
  .portrait img {
    max-width: 220px;
  }
}
</style>
