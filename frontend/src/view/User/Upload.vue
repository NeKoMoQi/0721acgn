<template>
  <div class="root-container">
    <Headpage style="position: sticky !important; top: 0px; z-index: 10" />
    <div class="sheet">
      <h1 class="h1">我要上传</h1>
      <p class="muted">以下为演示占位表单：将把记录写入浏览器并推送一条审核类消息通知。</p>
      <el-form label-width="100px" class="form">
        <el-form-item label="类型">
          <el-radio-group v-model="media">
            <el-radio-button label="animation">番剧 PV / 先导</el-radio-button>
            <el-radio-button label="comic">漫画资源</el-radio-button>
            <el-radio-button label="novel">轻小说条目</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="title" placeholder="作品标题" maxlength="80" />
        </el-form-item>
        <el-form-item label="链接 / 备注">
          <el-input v-model="detail" type="textarea" :rows="3" placeholder="可填写网盘/OSS链接或简短说明（演示不向服务器投递）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提交（演示）</el-button>
          <router-link class="back" :to="{ name: 'UserCenter' }">返回个人中心</router-link>
        </el-form-item>
      </el-form>
    </div>
    <Behind class="bottom" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Headpage from '@/components/Headpage/index.vue'
import { ElMessage } from 'element-plus'
import { appendUserUploadRecord } from '@/utils/acgnPersonalFeed'
import { pushNotification } from '@/utils/acgnNotifications'

const media = ref<'animation' | 'comic' | 'novel'>('animation')
const title = ref('')
const detail = ref('')

function submit() {
  const t = title.value.trim()
  const d = detail.value.trim()
  if (!t) {
    ElMessage.warning('请先填写标题')
    return
  }
  appendUserUploadRecord({
    kind: 'resource_submit',
    label:
      media.value === 'animation' ? '番剧先导 / PV' : media.value === 'comic' ? '漫画资源' : '轻小说条目',
    detail: d || undefined,
    relatedName: t,
    auditStatus: 'pending',
  })
  pushNotification({
    id: `up_${Date.now()}`,
    kind: 'review_result',
    title: '投稿已收到（演示）',
    body: `「${t}」将进入人工审核占位流程，可随时在「个人中心 · 上传记录」查看状态。`,
    at: Date.now(),
  })
  ElMessage.success('已记录并开始排队（演示），消息页可查看推送')
  title.value = ''
  detail.value = ''
}
</script>

<style scoped>
.root-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #eef1f6;
}
.sheet {
  flex: 1;
  max-width: 620px;
  margin: 100px auto 40px;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}
.h1 {
  margin: 0 0 8px;
}
.muted {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}
.form {
  max-width: 520px;
}
.back {
  margin-left: 12px;
  font-size: 14px;
  color: #00aeec;
  text-decoration: none;
}
.bottom {
  margin-top: auto;
}
</style>
