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
            :alt="character.name"
            @error="avatarOk = false"
          />
          <div v-else class="portrait-fallback">{{ character.name }}</div>
        </div>
        <div class="copy">
          <span class="eyebrow">0721 ACGN · 角色介绍</span>
          <h1>{{ character.name }}</h1>
          <dl class="facts">
            <div><dt>性别</dt><dd>{{ character.sex || '未填写' }}</dd></div>
            <div><dt>生日</dt><dd>{{ character.birthday || '未填写' }}</dd></div>
          </dl>
          <section v-if="introParagraphs.length" class="intro">
            <h2>简介</h2>
            <p v-for="(paragraph, index) in introParagraphs" :key="index">{{ paragraph }}</p>
            <small v-if="sourceUrl">
              内容摘自
              <a :href="sourceUrl" target="_blank" rel="noopener noreferrer">萌娘百科</a>
              ，依 CC BY-NC-SA 3.0 CN 使用。
            </small>
          </section>
          <h2>萌点</h2>
          <ul v-if="tags.length" class="tags">
            <li v-for="tag in tags" :key="tag">{{ tag }}</li>
          </ul>
          <p v-else class="empty">这个角色还没有录入萌点。</p>
          <div class="actions">
            <button type="button" @click="next">换一个角色</button>
            <RouterLink to="/news" class="ghost">看看资讯</RouterLink>
          </div>
        </div>
      </article>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Headpage from '@/components/Headpage/index.vue'
import request from '@/utils/request'

interface Character {
  id: number | string
  name: string
  sex: string
  birthday: string
  talent: string
  avatarurl: string
  intro?: string
  introsource?: string
}

const route = useRoute()
const router = useRouter()
const character = ref<Character | null>(null)
const loading = ref(false)
const error = ref('')
const avatarOk = ref(true)

const tags = computed(() =>
  (character.value?.talent || '').split(/[、,，]/).map((tag) => tag.trim()).filter(Boolean),
)
const introParagraphs = computed(() =>
  (character.value?.intro || '').split(/\n{2,}/).map((paragraph) => paragraph.trim()).filter(Boolean),
)
const sourceUrl = computed(() =>
  /^https:\/\/zh\.moegirl\.org\.cn\//.test(character.value?.introsource || '') ? character.value?.introsource : '',
)
// 头像只接受站内路径或 https，避免把外部字段直接塞进 img。
const safeAvatar = (url: string) => /^\/(?!\/)/.test(url || '') || /^https:\/\//i.test(url || '') ? url : ''

async function load() {
  const id = String(route.params.id || '')
  if (!/^\d+$/.test(id)) {
    error.value = '角色编号不对。'
    return
  }
  loading.value = true
  error.value = ''
  character.value = null
  avatarOk.value = true
  try {
    const result: any = await request.get('/acgn/user/byid', { params: { id } })
    if (result?.code !== 200 || !result.data) throw new Error('empty')
    character.value = result.data
  } catch {
    error.value = '这个角色暂时读不到，可能是编号不存在。'
  } finally {
    loading.value = false
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

watch(() => route.params.id, () => { void load() }, { immediate: true })
</script>

<style scoped>
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
