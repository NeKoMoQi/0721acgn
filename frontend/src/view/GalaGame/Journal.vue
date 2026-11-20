<template>
  <div class="root-container">
    <Headpage class="journal-head" style="position: sticky !important; top: 0; z-index: 10" />
    <div class="background-layer" aria-hidden="true" />
    <main class="journal-container">
      <header class="journal-header">
        <div>
          <span class="eyebrow">GAME DIARY · 游戏里的小日常</span>
          <h1>游玩手记<span class="title-dot">.</span></h1>
          <p class="intro">故事可以慢慢读，喜欢的瞬间记得留下来。</p>
        </div>
        <el-button type="primary" :icon="EditPen" @click="openEditor()">写一篇手记</el-button>
      </header>
      <section class="status-overview" aria-label="游玩概况">
        <button v-for="status in statuses" :key="status.value" type="button" class="status-card"
          :class="[status.value, { selected: tab === status.value }]" :aria-pressed="tab === status.value"
          @click="tab = tab === status.value ? 'all' : status.value">
          <el-icon :size="22"><component :is="status.icon" /></el-icon>
          <span class="status-copy"><strong>{{ count(status.value) }}</strong><span>{{ status.label }}</span></span>
          <span class="status-caption">{{ status.caption }}</span>
        </button>
      </section>
      <section aria-labelledby="entries-title">
        <div class="section-heading">
          <h2 id="entries-title">我的游戏存档 <span>{{ entries.length }} 篇</span></h2>
          <span class="order-hint">按最近更新排列</span>
        </div>
        <div class="toolbar">
          <div class="filters" aria-label="按游玩状态筛选">
            <button v-for="filter in filters" :key="filter.value" type="button" class="seg"
              :class="{ on: tab === filter.value }" :aria-pressed="tab === filter.value"
              @click="tab = filter.value">{{ filter.label }}</button>
          </div>
          <el-input v-model="query" :prefix-icon="Search" clearable placeholder="找找游戏或感想" aria-label="搜索手记" class="journal-search" />
        </div>
        <div v-if="loadError" class="empty-state" role="alert">
          <h3>手记暂时没有读出来</h3>
          <p>本机存储无法读取，原有记录未被修改。请检查浏览器存储权限后刷新重试。</p>
        </div>
        <div v-else-if="!entries.length" class="empty-state">
          <div class="empty-icon"><el-icon :size="36"><Notebook /></el-icon></div>
          <span class="empty-eyebrow">故事，从第一篇开始</span>
          <h3>给刚刚玩过的故事，留个存档吧</h3>
          <p>正在攻略的路线、通关后的余韵，或是暂时按下暂停的理由。<br />不用写成长评，记下一点心情就好。</p>
          <el-button type="primary" plain :icon="Plus" @click="openEditor()">记录第一款游戏</el-button>
        </div>
        <div v-else-if="!shown.length" class="empty-state filtered-empty">
          <el-icon :size="32"><Search /></el-icon><h3>这里还没有匹配的手记</h3>
          <p>换个关键词，或者看看其他游玩状态。</p>
          <el-button text type="primary" @click="tab = 'all'; query = ''">查看全部手记</el-button>
        </div>
        <div v-else class="entry-list">
          <article v-for="entry in shown" :key="entry.id" class="entry-card">
            <div class="entry-mark" :class="entry.status" aria-hidden="true"><el-icon :size="26"><component :is="statusInfo(entry.status).icon" /></el-icon></div>
            <div class="entry-body">
              <div class="entry-heading"><h3>{{ entry.title }}</h3><span class="status-badge" :class="entry.status">{{ statusInfo(entry.status).label }}</span></div>
              <p v-if="entry.progress" class="progress-label">{{ entry.progress }}</p>
              <p class="entry-note" :class="{ muted: !entry.note }">{{ entry.note || '先存个档，感想下次再慢慢写。' }}</p>
              <div class="entry-footer">
                <time :datetime="new Date(entry.updatedAt).toISOString()">更新于 {{ formatDate(entry.updatedAt) }}</time>
                <div class="entry-actions">
                  <el-button text type="primary" :aria-label="`编辑${entry.title}的手记`" @click="openEditor(entry)">编辑</el-button>
                  <el-popconfirm title="确定删除这篇手记吗？" confirm-button-text="删除" cancel-button-text="保留" @confirm="removeEntry(entry.id)">
                    <template #reference><el-button text type="danger" :aria-label="`删除${entry.title}的手记`">删除</el-button></template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>
      <p class="storage-note"><el-icon><Lock /></el-icon> 手记保存在当前浏览器，刷新后仍在；暂不支持账号同步，清除浏览器数据会移除记录。</p>
    </main>
    <Behind class="bottom" />
    <el-dialog v-model="editorVisible" :title="editingId ? '编辑游玩手记' : '新的游玩手记'" width="min(540px, 92vw)" :close-on-click-modal="false" append-to-body>
      <el-form label-position="top" @submit.prevent="saveEntry">
        <el-form-item label="游戏名称 · 必填"><el-input v-model="draft.title" maxlength="80" show-word-limit placeholder="这次走进了哪个故事？" aria-label="游戏名称" /></el-form-item>
        <el-form-item label="游玩状态"><el-radio-group v-model="draft.status"><el-radio-button v-for="status in statuses" :key="status.value" :value="status.value">{{ status.label }}</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="当前进度"><el-input v-model="draft.progress" maxlength="80" placeholder="例如：共通线第 3 章 / 一周目通关" aria-label="当前进度" /></el-form-item>
        <el-form-item label="留一点感想"><el-input v-model="draft.note" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="喜欢的角色、难忘的台词，或是今天的游玩心情……" aria-label="游玩感想" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :disabled="loadError" @click="saveEntry">保存手记</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Search, Notebook, Plus, Lock, VideoPlay, CircleCheck, VideoPause } from '@element-plus/icons-vue'

type PlayStatus = 'playing' | 'completed' | 'paused'
interface JournalEntry {
  id: string
  title: string
  status: PlayStatus
  progress: string
  note: string
  updatedAt: number
}
const storageKey = 'acgn_game_diary_v1'
const statuses = [
  { value: 'playing' as const, label: '在玩', caption: '故事正在继续', icon: VideoPlay },
  { value: 'completed' as const, label: '已通关', caption: '落幕也有回响', icon: CircleCheck },
  { value: 'paused' as const, label: '暂时搁置', caption: '等一个想玩的日子', icon: VideoPause },
]
const filters = [{ value: 'all' as const, label: '全部手记' }, ...statuses]
const entries = ref<JournalEntry[]>([])
const tab = ref<PlayStatus | 'all'>('all')
const query = ref('')
const loadError = ref(false)
const editorVisible = ref(false)
const editingId = ref<string | null>(null)
const draft = reactive({ title: '', status: 'playing' as PlayStatus, progress: '', note: '' })
const shown = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  return entries.value.filter(entry => (tab.value === 'all' || entry.status === tab.value) &&
    `${entry.title} ${entry.progress} ${entry.note}`.toLocaleLowerCase().includes(keyword),
  ).sort((a, b) => b.updatedAt - a.updatedAt)
})
const count = (status: PlayStatus) => entries.value.filter(entry => entry.status === status).length
const statusInfo = (status: PlayStatus) => statuses.find(item => item.value === status)!
const formatDate = (timestamp: number) => new Date(timestamp).toLocaleDateString('zh-CN')
onMounted(() => {
  try {
    const data: unknown = JSON.parse(localStorage.getItem(storageKey) || '[]')
    if (!Array.isArray(data) || !data.every(entry => entry &&
      typeof entry.id === 'string' && typeof entry.title === 'string' &&
      statuses.some(status => status.value === entry.status) &&
      typeof entry.progress === 'string' && typeof entry.note === 'string' &&
      typeof entry.updatedAt === 'number' && Number.isFinite(entry.updatedAt) &&
      !Number.isNaN(new Date(entry.updatedAt).getTime()))) throw new Error('Invalid diary')
    entries.value = data
  } catch { loadError.value = true }
})
function openEditor(entry?: JournalEntry) {
  editingId.value = entry?.id ?? null
  Object.assign(draft, { title: entry?.title ?? '', status: entry?.status ?? 'playing', progress: entry?.progress ?? '', note: entry?.note ?? '' })
  editorVisible.value = true
}
function persist(next: JournalEntry[]) {
  if (loadError.value) return false
  try {
    localStorage.setItem(storageKey, JSON.stringify(next))
    entries.value = next
    return true
  } catch {
    ElMessage.error('保存失败，请检查浏览器存储空间或权限，本次修改未保存')
    return false
  }
}
function saveEntry() {
  if (!draft.title.trim()) { ElMessage.warning('先写下游戏名称吧'); return }
  const entry: JournalEntry = {
    id: editingId.value ?? crypto.randomUUID(), title: draft.title.trim(), status: draft.status,
    progress: draft.progress.trim(), note: draft.note.trim(), updatedAt: Date.now(),
  }
  if (persist([entry, ...entries.value.filter(item => item.id !== entry.id)])) {
    editorVisible.value = false
    tab.value = 'all'
    query.value = ''
    ElMessage.success('手记已保存')
  }
}
function removeEntry(id: string) {
  if (persist(entries.value.filter(entry => entry.id !== id))) ElMessage.success('手记已删除')
}
</script>

<style scoped>
.root-container { position: relative; min-height: 100vh; display: flex; flex-direction: column; color: #303133; font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: -1; background: url('/BCPHOTO/acgnbc4.png') no-repeat center / cover; pointer-events: none; }
.journal-container { box-sizing: border-box; width: min(1060px, 86%); margin: 64px auto 48px; padding: 36px; background: rgba(255, 255, 255, .96); border: 1px solid rgba(211, 211, 211, .65); border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, .08); }
.journal-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.eyebrow { color: #80929d; font-size: 11px; letter-spacing: 1.5px; }
h1 { font-size: 30px; font-weight: 600; margin: 12px 0; letter-spacing: 2px; }
.title-dot { color: #00aeec; }
.intro { font-size: 14px; color: #7f8c8d; line-height: 1.8; margin: 0; }
.status-overview { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin: 30px 0 36px; }
.status-card { display: flex; align-items: center; flex-wrap: wrap; gap: 14px; border: 1px solid #e9eef2; border-radius: 10px; padding: 20px; background: #f8fbfd; cursor: pointer; text-align: left; transition: border-color .2s, background .2s; font: inherit; }
.playing { --status-color: #00a2d9; --status-bg: #edf9ff; }
.completed { --status-color: #429b80; --status-bg: #eef8f2; }
.paused { --status-color: #b18a51; --status-bg: #fbf5e9; }
.status-card > .el-icon, .entry-mark { color: var(--status-color); }
.status-card:hover, .status-card.selected { border-color: var(--status-color); background: var(--status-bg); }
.status-copy { display: flex; align-items: baseline; gap: 10px; }
.status-copy strong { font-size: 26px; font-weight: 600; color: #303133; }
.status-copy > span { font-size: 13px; color: #606266; }
.status-caption { width: 100%; font-size: 12px; color: #8c959f; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2 { font-size: 18px; font-weight: 600; margin: 0; }
h2 span { margin-left: 8px; font-size: 12px; font-weight: 400; color: #909399; }
.order-hint { font-size: 12px; color: #909399; }
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 14px; padding: 20px 0; border-bottom: 1px solid #eef2f5; }
.filters { display: flex; flex-wrap: wrap; gap: 6px; }
.seg { padding: 8px 13px; border: 1px solid transparent; border-radius: 999px; background: transparent; color: #666; font: inherit; font-size: 13px; cursor: pointer; }
.seg.on { background: #edf9ff; color: #009acf; border-color: #d9f0fa; }
.journal-search { width: 210px; }
.empty-state { padding: 56px 12px; text-align: center; }
.empty-icon { display: grid; place-items: center; width: 78px; height: 78px; margin: 0 auto 22px; border-radius: 24px; background: #edf9ff; color: #00aeec; transform: rotate(-6deg); }
.empty-eyebrow { color: #00a2d9; font-size: 12px; letter-spacing: 2px; }
.empty-state h3 { margin: 15px 0 12px; font-size: 18px; font-weight: 500; }
.empty-state p { color: #8b939c; font-size: 13px; line-height: 1.9; margin-bottom: 24px; }
.filtered-empty { color: #909399; }
.entry-list { display: grid; gap: 16px; margin-top: 22px; }
.entry-card { display: flex; align-items: flex-start; gap: 18px; padding: 22px; border: 1px solid #e9eef2; border-radius: 10px; background: #fff; }
.entry-mark { display: grid; place-items: center; width: 58px; height: 72px; background: var(--status-bg); border-radius: 8px; flex-shrink: 0; }
.entry-body { flex: 1; min-width: 0; }
.entry-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.entry-heading h3 { margin: 0; font-size: 17px; line-height: 1.5; overflow-wrap: anywhere; }
.status-badge { color: var(--status-color); background: var(--status-bg); border-radius: 5px; padding: 4px 8px; font-size: 11px; }
.progress-label { color: #8593a1; font-size: 12px; margin: 10px 0; overflow-wrap: anywhere; }
.entry-note { margin: 12px 0; font-size: 14px; color: #606266; line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.entry-note.muted { color: #a1a7ae; }
.entry-footer { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; }
.entry-footer time { color: #a1a7ae; font-size: 12px; }
.entry-actions { display: flex; }
.storage-note { display: flex; align-items: center; justify-content: center; gap: 6px; color: #909399; font-size: 12px; line-height: 1.7; border-top: 1px dashed #e9eef2; padding-top: 22px; margin: 16px 0 0; }
.storage-note .el-icon { flex-shrink: 0; }
.bottom { margin-top: auto; position: relative; box-sizing: border-box; }
.root-container :deep(.journal-head) { box-sizing: border-box; }
.root-container :deep(.journal-head .kuang) { width: 100%; height: 44px; padding: 4px; }
.root-container :deep(.journal-head .acgn) { white-space: nowrap; }
@media (max-width: 1100px) {
  .root-container :deep(.journal-head) { flex-wrap: wrap; gap: 8px; padding: 8px 12px; }
  .root-container :deep(.journal-head .left-group) { width: 100%; min-width: 0; gap: 0; }
  .root-container :deep(.journal-head .logo) { flex-shrink: 0; }
  .root-container :deep(.journal-head .left-group > div:last-child) { overflow-x: auto; margin-left: 8px !important; padding: 12px 0; }
  .root-container :deep(.journal-head .search) { min-width: 100px; max-width: none; }
  .root-container :deep(.journal-head .user-wrap) { max-width: none; gap: 8px; }
  .root-container :deep(.journal-head .head-quick) { display: none; }
}
@media (max-width: 760px) {
  .journal-container { width: 94%; padding: 22px 18px; margin-top: 30px; }
  .journal-header { align-items: flex-start; flex-direction: column; gap: 18px; }
  .status-overview { gap: 8px; }
  .status-card { padding: 14px 10px; gap: 8px; }
  .status-copy { gap: 5px; flex-wrap: wrap; }
  .status-copy strong { font-size: 23px; }
  .status-caption { line-height: 1.5; }
  .journal-search { width: 100%; }
  .order-hint { display: none; }
  .entry-card { padding: 16px 12px; gap: 12px; }
  .entry-mark { width: 38px; height: 48px; }
  .empty-state { padding: 42px 4px; }
  .storage-note { align-items: flex-start; }
}
</style>
