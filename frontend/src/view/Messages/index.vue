<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10" />
    <div class="shell">
      <h1 class="h1">消息中心</h1>
      <p class="muted">订阅更新、评论回复、投稿审核结果与系统公告会以时间线聚合在此（演示站内本地存储）。</p>

      <div class="toolbar">
        <div class="seg-wrap">
          <button type="button" class="seg" :class="{ on: filter === '' }" @click="filter = ''">全部</button>
          <button type="button" class="seg" :class="{ on: filter === 'subscribe_update' }" @click="filter = 'subscribe_update'">
            订阅更新
          </button>
          <button type="button" class="seg" :class="{ on: filter === 'comment_reply' }" @click="filter = 'comment_reply'">
            评论回复
          </button>
          <button type="button" class="seg" :class="{ on: filter === 'review_result' }" @click="filter = 'review_result'">
            投稿审核
          </button>
          <button type="button" class="seg" :class="{ on: filter === 'system' }" @click="filter = 'system'">系统公告</button>
        </div>
        <el-button text type="primary" @click="markAllRead">全部标为已读</el-button>
      </div>

      <div v-if="!shown.length" class="empty">暂无此类消息。</div>
      <ul v-else class="list">
        <li v-for="n in shown" :key="n.id" class="row" @click="openOne(n)">
          <div class="badge" :class="n.kind">{{ kindLabel(n.kind) }}</div>
          <div class="mid">
            <div class="tit">{{ n.title }} <span v-if="!n.read" class="dot">未读</span></div>
            <div class="body">{{ n.body }}</div>
            <div class="time">{{ fmt(n.at) }}</div>
          </div>
        </li>
      </ul>
    </div>
    <Behind class="bottom" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Headpage from '@/components/Headpage/index.vue'
import {
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  type AcgnNotice,
} from '@/utils/acgnNotifications'

const filter = ref('')
const notices = ref<AcgnNotice[]>([])
const shown = computed(() => {
  if (!filter.value) return notices.value
  return notices.value.filter((x) => x.kind === filter.value)
})

function refresh() {
  notices.value = listNotifications()
}

function kindLabel(kind: AcgnNotice['kind']) {
  const m: Record<AcgnNotice['kind'], string> = {
    subscribe_update: '订阅',
    comment_reply: '回复',
    review_result: '审核',
    system: '系统',
  }
  return m[kind] ?? kind
}

function fmt(ms: number) {
  try {
    return new Intl.DateTimeFormat('zh-CN', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(ms)
  } catch {
    return String(ms)
  }
}

function openOne(n: AcgnNotice) {
  markNotificationRead(n.id)
  refresh()
}

function markAllRead() {
  markAllNotificationsRead()
  refresh()
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}
.shell {
  flex: 1;
  max-width: 760px;
  margin: 100px auto 40px;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}
.h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
.muted {
  color: #909399;
  font-size: 14px;
  margin: 0 0 20px;
  line-height: 1.6;
}
.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 16px;
  justify-content: space-between;
}
.seg-wrap {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.seg {
  border: 1px solid #e4e7ed;
  background: #fff;
  padding: 6px 14px;
  border-radius: 18px;
  cursor: pointer;
  font-size: 13px;
}
.seg.on {
  border-color: #00aeec;
  color: #00aeec;
}
.list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.row {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #eef0f3;
  cursor: pointer;
}
.row:hover .tit {
  color: #00aeec;
}
.badge {
  flex: 0 0 auto;
  height: fit-content;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}
.badge.subscribe_update {
  background: #eaf8ff;
  color: #00aeec;
}
.badge.comment_reply {
  background: #fce8ea;
  color: #fb7299;
}
.badge.review_result {
  background: #eaf9f0;
  color: #13a05d;
}
.badge.system {
  background: #f4f5f8;
  color: #606266;
}
.mid {
  flex: 1;
  min-width: 0;
}
.tit {
  font-weight: 600;
}
.dot {
  font-size: 12px;
  color: #f56c6c;
}
.body {
  margin-top: 6px;
  color: #303133;
  font-size: 14px;
  line-height: 1.58;
}
.time {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #909399;
}
.bottom {
  margin-top: auto;
}
</style>
