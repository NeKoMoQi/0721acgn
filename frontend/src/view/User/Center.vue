<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10" />
    <div class="sheet">
      <h1 class="h1">个人中心</h1>

      <el-tabs v-model="tab" type="border-card">
        <el-tab-pane label="资料" name="profile">
          <div class="profile-row">
            <img :src="avatarShow" alt="" class="big-av" />
            <div>
              <p class="uname">{{ store.username }}</p>
              <p class="muted">修为：{{ store.level }}</p>
              <p class="hint">昵称与等级来源于登录后端；头像可本机裁剪更换（仅存浏览器）。</p>
              <label class="file-label">
                <input type="file" accept="image/*" class="hidden" @change="onPickAvatar" />
                <span>选择头像</span>
              </label>
              <div v-if="preview" class="preview-row">
                <el-button type="primary" size="small" @click="saveAvatar">保存为当前头像</el-button>
                <el-button text size="small" @click="clearPreview">取消预览</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="收藏 / 足迹 / 订阅" name="lib">
          <div class="stats">
            <div class="card">
              <div class="n">{{ starredCount }}</div>
              <div class="lbl">收藏</div>
              <router-link class="lnk" :to="{ name: 'Bookshelf' }">去历史页 · 只看星标 »</router-link>
            </div>
            <div class="card">
              <div class="n">{{ historyCount }}</div>
              <div class="lbl">浏览足迹</div>
              <router-link class="lnk" :to="{ name: 'Bookshelf' }">打开历史 »</router-link>
            </div>
            <div class="card">
              <div class="n">{{ subCount }}</div>
              <div class="lbl">连载订阅</div>
              <router-link class="lnk" :to="{ name: 'Bookshelf' }">同设备同步 »</router-link>
            </div>
          </div>
          <h3 class="subh">最近在追</h3>
          <ul class="thumb-list">
            <li v-for="it in historySample" :key="it.type + '-' + it.id" class="trow" @click="go(it)">
              <img v-if="it.pic" class="thumb" :src="it.pic" alt="" />
              <div>
                <div class="tit">{{ it.name }}</div>
                <div class="muted2">{{ typ(it.type) }} · {{ it.starred ? '已收藏' : '足迹' }}</div>
              </div>
            </li>
          </ul>
          <h3 class="subh">订阅</h3>
          <ul class="thumb-list">
            <li v-for="s in subs" :key="'s-' + s.type + '-' + s.id" class="trow" @click="go(s)">
              <img v-if="s.pic" class="thumb" :src="s.pic" alt="" />
              <div>
                <div class="tit">{{ s.name }}</div>
                <div class="muted2">{{ typ(s.type) }} · {{ fmtAt(s.subscribedAt) }}</div>
              </div>
            </li>
          </ul>
          <div v-if="!subs.length" class="muted">暂无订阅条目，请在作品详情面板点「订阅更新」。 </div>
        </el-tab-pane>

        <el-tab-pane label="我的评论" name="reviews">
          <ul class="reviews">
            <li v-for="(r, i) in reviews" :key="i" class="review" @click="goRev(r)">
              <span class="ch">{{ channel(r.channel) }}</span>
              <div class="tit">{{ r.workName }}</div>
              <div class="txt">{{ r.text }}</div>
              <div class="muted">{{ fmtAt(r.at) }}</div>
            </li>
          </ul>
          <div v-if="!reviews.length" class="muted">还没留下短评——去任意简介页的互动区发条评论吧。</div>
        </el-tab-pane>

        <el-tab-pane label="上传记录" name="uploads">
          <router-link class="inline-link" :to="{ name: 'UserUpload' }">去投稿表单 »</router-link>
          <ul class="upl">
            <li v-for="u in uploads" :key="u.id" class="urow">
              <div class="tit">{{ uplLabel(u) }}</div>
              <div class="muted" v-if="u.relatedName">作品：{{ u.relatedName }}</div>
              <div class="muted" v-if="u.detail">{{ u.detail }}</div>
              <span class="st" :class="u.auditStatus">{{ auditLbl(u.auditStatus) }}</span>
              <div class="muted">{{ fmtAt(u.at) }}</div>
            </li>
          </ul>
          <div v-if="!uploads.length" class="muted">尚未记录上传——在番剧页上传 PV / 或通过「我要上传」投递。</div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <Behind class="bottom" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import Headpage from '@/components/Headpage/index.vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/stores/modules/users'
import {
  ensureAcgnUserHydrated,
  listHistory,
  listSubscriptions,
  type AcgnMediaType,
} from '@/utils/acgnLocal'
import { listMyPublishedComments, listUserUploadRecords, type UserUploadRecord } from '@/utils/acgnPersonalFeed'

const store = useUserStore()
const router = useRouter()
const tab = ref('profile')

const preview = ref('')

const history = ref(listHistory())
const subs = ref(listSubscriptions())

const reviews = computed(() => listMyPublishedComments())
const uploads = computed(() => listUserUploadRecords())

const starredCount = computed(() => history.value.filter((x) => x.starred).length)
const historyCount = computed(() => history.value.length)
const subCount = computed(() => subs.value.length)

const historySample = computed(() =>
  [...history.value].sort((a, b) => (b.at || 0) - (a.at || 0)).slice(0, 10),
)

const avatarShow = computed(() => preview.value || store.avatar || '/enter.jpg')

function fmtAt(ms: number) {
  try {
    return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(ms)
  } catch {
    return ''
  }
}

function typ(t: AcgnMediaType | string) {
  if (t === 'animation') return '动漫'
  if (t === 'comic') return '漫画'
  return '小说'
}

function go(it: { type: AcgnMediaType | string; id: number }) {
  if (it.type === 'animation')
    router.push({ name: 'Animationintroduce', query: { animationid: String(it.id) } })
  else if (it.type === 'comic')
    router.push({ name: 'ComicIntroduce', query: { comicid: String(it.id) } })
  else router.push({ name: 'NovelIntroduce', query: { novelid: String(it.id) } })
}

function goRev(r: ReturnType<typeof listMyPublishedComments>[0]) {
  go({ type: r.type, id: r.mediaId })
}

function channel(c: string) {
  return c === 'server' ? '云端' : '本地'
}

function onPickAvatar(e: Event) {
  const inp = e.target as HTMLInputElement
  const f = inp.files?.[0]
  if (!f) return
  const r = new FileReader()
  r.onload = () => {
    if (typeof r.result === 'string') preview.value = r.result
  }
  r.readAsDataURL(f)
  inp.value = ''
}

function saveAvatar() {
  if (!preview.value) return
  store.setLocalAvatar(preview.value)
  preview.value = ''
}

function clearPreview() {
  preview.value = ''
}

function uplLabel(u: UserUploadRecord) {
  if (u.kind === 'preview_video') return '番剧 PV / 试看视频'
  return u.label || '投稿'
}

function auditLbl(s?: UserUploadRecord['auditStatus']) {
  if (s === 'pending') return '待审核'
  if (s === 'rejected') return '未通过'
  return '已通过'
}

onMounted(async () => {
  await ensureAcgnUserHydrated()
  history.value = listHistory()
  subs.value = listSubscriptions()
})
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f5f7;
}
.sheet {
  flex: 1;
  max-width: 960px;
  margin: 100px auto 40px;
  padding: 0 16px 40px;
}
.h1 {
  margin: 0 0 16px;
  font-size: 26px;
}
.profile-row {
  display: flex;
  gap: 28px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.big-av {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}
.uname {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px;
}
.muted {
  color: #909399;
  font-size: 14px;
  margin: 4px 0;
}
.hint {
  color: #606266;
  font-size: 13px;
  max-width: 520px;
  line-height: 1.6;
}
.hidden {
  display: none;
}
.file-label span {
  display: inline-block;
  margin-top: 12px;
  padding: 8px 16px;
  background: #00aeec;
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}
.preview-row {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.card {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
  border: 1px solid #ebeef5;
}
.card .n {
  font-size: 28px;
  font-weight: 800;
  color: #00aeec;
}
.card .lbl {
  font-size: 13px;
  color: #606266;
}
.lnk {
  display: inline-block;
  margin-top: 8px;
  font-size: 12px;
  color: #00aeec;
  text-decoration: none;
}
.subh {
  margin: 20px 0 10px;
  font-size: 16px;
}
.thumb-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.trow {
  display: flex;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}
.trow:hover {
  background: #ecf5ff;
}
.thumb {
  width: 48px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
}
.tit {
  font-weight: 600;
}
.muted2 {
  font-size: 12px;
  color: #909399;
}
.reviews {
  list-style: none;
  padding: 0;
  margin: 0;
}
.review {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  cursor: pointer;
}
.review .ch {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 6px;
  background: #f0f2f5;
}
.review .txt {
  margin-top: 8px;
  color: #303133;
  line-height: 1.5;
}
.inline-link {
  display: inline-block;
  margin-bottom: 12px;
  color: #00aeec;
}
.upl {
  list-style: none;
  padding: 0;
  margin: 0;
}
.urow {
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 0;
}
.urow .st {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  margin-top: 6px;
  display: inline-block;
}
.urow .st.pending {
  background: #fff7e6;
  color: #d48806;
}
.urow .st.approved {
  background: #f0f9eb;
  color: #52c41a;
}
.urow .st.rejected {
  background: #fff1f0;
  color: #cf1322;
}
.bottom {
  margin-top: auto;
}
</style>
