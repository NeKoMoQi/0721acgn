<template>
<div v-if="loading_page == 1" class="overlay"></div>
<div v-if="loading_page == 1" class="loading_page" role="dialog" aria-modal="true" aria-label="登录" @keydown.esc.stop="close">
  <div class="chaijun">
    <div class="login-modes" style="position: relative; top: 18%; text-align: center; padding: 0 18px;">
      <p class="erwei">0721ACGN</p>
      <p class="weixin">游客也可以浏览作品和资讯。</p>
    </div>
      <div>
      <img v-if="chaijun == true" src="/tantou.png" alt="" style="width: 100px;  left:0%; top: 84.3%; position: fixed;">
      <img v-if="chaijun == false" src="/tantou2.png" alt="" style="width: 100px;  left:0%; top: 90.6%; position: fixed;">
    </div>
  </div>
  <div class="login-divider" style="width: 1px; height: 90%; background-color: #cccccc; margin: 20px 0;"></div>
  <div class="load" @keydown.enter.prevent="entering">
    <button type="button" class="login-close" aria-label="关闭登录" @click="close" @keydown.enter.stop>×</button>
    <div class="login-modes" style="display: flex; justify-content: center; top: 10%; position: relative; margin-bottom: 40px;">
      <h2 style="font-weight: 500; font-size: 18px; line-height: 20px;">账号密码登录</h2>
    </div>
    <div class="login-field" style="top: 15%; position: relative; justify-content: center; display: flex;">
    <el-input
      ref="usernameInput"
      v-model="loginForm.username"
      autocomplete="username"
      style="max-width: 400px;height: 50px; "
      placeholder="请输入账号"
    >
      <template #prepend>账号</template>
    </el-input>
  </div>
   <div class="login-field" style="display: flex;justify-content: center;  position: relative;margin-top: 43px; top: 4.5%;">
    <el-input
       @focus="handleFocus1"
       @blur = "handleFocus2"
       v-model="loginForm.password"
       type="password"
       autocomplete="current-password"
      style="max-width: 400px;height: 50px;"
      placeholder="请输入密码"

    >
      <template #prepend>密码</template>
    </el-input>

   </div>
   <div class="login-actions" style="position: relative; top: 12%; justify-content: center; display: flex;">
    <button type="button" class="denglu" :disabled="submitting" @click="entering">{{ submitting ? '正在登录…' : '登录' }}</button>
   </div>
   <div class="login-note" style="position: relative; top: 15%; justify-content: center; display: flex;">
    <p class="weixin">自助注册暂未开放。已有账号可直接登录。</p>
   </div>
    <div>
      <img v-if="chaijun == true" src="/tantou2 left.png" alt="" style="width: 100px;  left: 88%; top: 84.3%; position: fixed;">
      <img v-if="chaijun == false" src="/tantou left.png" alt="" style="width: 100px;  left: 88%; top: 90.6%; position: fixed;">
    </div>
  </div>

</div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import useUserStore from '@/stores/modules/users'
import { getTime } from '@/utils/time'

const userStore = useUserStore()
const router = useRouter()
const loading_page = computed(() => userStore.loginVisible ? 1 : 0)
const usernameInput = ref<{ focus: () => void } | null>(null)
let returnFocus: HTMLElement | null = null
const chaijun = ref(true)
const loginForm = reactive({ username: '', password: '' })
const submitting = ref(false)

watch(() => userStore.loginVisible, async (visible) => {
  if (visible) {
    if (document.activeElement instanceof HTMLElement) returnFocus = document.activeElement
    await nextTick()
    usernameInput.value?.focus()
  } else {
    loginForm.password = ''
    chaijun.value = true
    await nextTick()
    if (returnFocus?.isConnected) returnFocus.focus()
    returnFocus = null
  }
}, { immediate: true })

function close() {
  if (!submitting.value) userStore.closeLogin()
}
function handleFocus1() { chaijun.value = false }
function handleFocus2() { chaijun.value = true }

async function entering() {
  if (submitting.value) return
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  submitting.value = true
  try {
    await userStore.userLogin({ username: loginForm.username.trim(), password: loginForm.password })
    try { await userStore.userinfo() } catch { /* 鉴权失败由请求层处理，网络故障保留凭证。 */ }
    if (!userStore.token) return
    const target = userStore.loginRedirect
    userStore.closeLogin()
    if (target) await router.push(target)
    ElNotification({ type: 'success', message: '欢迎回来', title: `Hi,${getTime()}`, duration: 3000 })
  } catch (error) {
    ElNotification({ type: 'error', message: error instanceof Error ? error.message : '登录失败，请稍后重试' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.weixin{
  margin: 0;
    font-family: PingFang SC;
    font-style: normal;
    font-weight: 400;
    font-size: 13px;
    line-height: 19px;
    text-align: center;
    color: #505050;
}
.erwei{
  font-family: PingFang SC;
    font-style: normal;
    font-weight: 500;
    font-size: 18px;
    line-height: 16px;
    color: #505050;
    margin-bottom: 26px;
}


.denglu{
  -webkit-box-sizing: border-box;
    box-sizing: border-box;
    width: 194px;
    height: 40px;
    cursor: pointer;
    color: #fff;
    background: #00aeec;
    border-radius: 8px;
    border: null;
}
.chaijun{
  background-color: white;
  height: 100%;
  width: 40%;
  z-index: 1002;
}
.load{
  background-color: #fff;
  height: 100%;
  width: 60%;
  z-index: 1002;
}
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5); /* 半透明黑色 */
  z-index: 31000; /* 确保遮罩层在其他元素之上 */
}
.loading_page{
  width: 820px;
  height: 430px;
  position: fixed;
  background-color: white;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 31001; /* 确保加载页面在遮罩层之上 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2); /* 添加阴影效果 */
  display: flex;
  align-items: center; /* 垂直居中对齐 */
  justify-content: center;
}

.el-input {
  border: none;
}

:deep(.el-input_wrapper){
    box-shadow: none;
    border: none;
}

button:hover {
  transform: scale(1.02);
  background: linear-gradient(145deg, #ff8ad0 0%, #ff5ab8 100%);
  box-shadow:
    0 6px 12px rgba(255, 110, 199, 0.4),
    inset 2px 2px 4px rgba(255, 255, 255, 0.3);
}

button:active {
  transform: scale(0.98);
}

input:focus{
    border-color: #66afe9;
    outline: 0;
    -webkit-box-shadow: inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6);
    box-shadow: inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6)
}
::v-deep(.el-input__inner) {
  border: none !important; /* 确保覆盖默认边框 */
  background-color: #fff; /* 自定义背景色 */
  color: #333; /* 自定义字体颜色 */
}
::v-deep(.el-input__inner:focus) {
  border-color: #66afe9;
  outline: 0;
  box-shadow: 0 0 8px rgba(102, 175, 233, 0.6);
}

.login-close {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 1003;
  width: 44px;
  height: 44px;
  border: 0;
  background: transparent;
  color: #303133;
  font-size: 26px;
}

@media (max-width: 850px), (max-height: 500px) {
  .overlay { z-index: 31000; }
  .loading_page {
    z-index: 31001;
    width: min(440px, calc(100% - 24px));
    height: auto;
    max-height: calc(100dvh - 24px);
    overflow: auto;
    display: block;
    border-radius: 14px;
  }
  .chaijun, .login-divider, .load img { display: none; }
  .load {
    display: grid;
    gap: 12px;
    width: 100%;
    height: auto;
    padding: 50px 16px 20px;
  }
  .load .login-modes, .login-field, .login-actions, .login-note {
    top: auto !important;
    margin: 0 !important;
  }
  .login-actions { gap: 8px; }
  .login-actions button {
    flex: 1;
    width: auto !important;
    height: 44px !important;
    margin: 0 !important;
  }
  :deep(.el-input-group__prepend), :deep(.el-input-group__append) {
    padding-inline: 8px;
  }
}
</style>
