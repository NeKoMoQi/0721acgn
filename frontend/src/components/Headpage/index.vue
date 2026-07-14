<template>
<div class="heads" v-bind="$attrs">
    <div class="left-group">
    <div class="logo" @click="back">
        <img src="/LOGO.png" alt="" style="height: 60px; width: 100px; padding: 4px;">
    </div>
    <div style="display: flex; gap: 20px; margin-left: 20px;">
      <div @click="animation" :style="{ color: route.path === '/animation' ? '#00aeec' : '' }" class="acgn">番剧</div>
      <div @click="comic"  :style="{ color: route.path === '/comic' ? '#00aeec' : '' }" class="acgn">漫画</div>
      <div @click="novel" :style="{ color: route.path === '/novel' ? '#00aeec' : '' }" class="acgn">轻小说</div>
      <div @click="zhanqi" :style="{ color: route.path.startsWith('/warflag') ? '#00aeec' : '' }" class="acgn">战旗</div>
      <div @click="galagame" :style="{ color: route.path === '/galagame' ? '#00aeec' : '' }" class="acgn">未知功能</div>
      <div @click="gotosd" :style="{ color: route.path === '/shengdi' ? '#00aeec' : '' }" class="acgn">圣地巡礼</div>
    </div>
    </div>
    <div class="search" >
      <el-input
      v-model="searchText"
      type="text"
      placeholder="您要搜索什么喵？(๑•̀ㅂ•́)و✧"
      class="kuang"
      :suffix-icon="Search"
      @input = "allsearch(searchText)"
      @keydown.enter.prevent="goFullSearch"
      clearable
    />
    </div>
    <div 
          v-show="showResults && searchText"
          class="search-results-dropdown"
        >
          <div 
            class="search-more"
            @mousedown.prevent
            @click="goFullSearch"
          >
            在「全站搜索」中打开高级筛选与结果高亮 »
          </div>
          <div v-if="!searchResults.length" class="search-empty-pill">暂无快速匹配</div>
          <div 
            v-for="(item, index) in searchResults" 
            :key="index"
            class="result-item"
            @click="selectResult(item)"
          >
            <!-- 左侧图片 -->
            <img :src="item.picurl" alt="" class="result-img">
            
            <!-- 中间名字 -->
            <div class="result-name">{{ item.name }}</div>
            
            <!-- 右侧类型 -->
            <div class="result-type">{{ item.sourcetype }}</div>
          </div>
    </div>
    <div class="user-wrap">
      <div class="avatar-wrapper">
        <img
          v-if="useStore.avatar"
          :src="useStore.avatar"
          class="avatar"
          @mouseenter="userenter"
          @mouseleave="userleave"
          :style="{
            transform: isUserHovering ? 'scale(2) translate(-20%, 20%)' : 'scale(1)',
            transition: 'transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94)'
          }"
        />
        <img
          v-else
          src="/enter.jpg"
          class="avatar"
          @click="Loading"
          @mouseenter="loadenter"
          @mouseleave="loadleave"
        />
      <div @mouseenter="userenter" @mouseleave="userleave" v-if="isUserHovering == true" class="usertip">
        <div style="color: #fb7299; font-size: 18px; margin-top: 30px; font-weight: bold; font-family: 'PingFang SC';">
          {{useStore.username}}
        </div>
        <div>
          修为：{{ useStore.level }}
        </div>
        <div>
          -----------------------------------------------
        </div>
        <div class="profile-box" @click.stop="goUserCenter">
  <!-- 左侧文字 -->
  <div style="
    font-family: 'PingFang SC';
    font-size: 14px;
    color: #303133;
    font-weight: 500;
    cursor: pointer;
  ">
    个人中心
  </div>

  <!-- 右侧箭头 -->
  <span class="arrow-icon" style="
    color: #909399;
    font-size: 12px;
    transition: transform 0.2s ease;
  ">
    ❯
  </span>
</div>
<div 
  class="profile-box"
  @click.stop="goUserUpload"
>
  <!-- 左侧文字 -->
  <div style="
    font-family: 'PingFang SC';
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  ">
    我要上传
  </div>

  <!-- 右侧箭头 -->
  <span class="arrow-icon" style="
    color: #909399;
    font-size: 12px;
    transition: transform 0.2s ease;
  ">
    ❯
  </span>
</div>
<div 
  class="profile-box"
  @click.stop="goUserActivity"
>
  <!-- 左侧文字 -->
  <div style="
    font-family: 'PingFang SC';
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  ">
    我的动态
  </div>

  <!-- 右侧箭头 -->
  <span class="arrow-icon" style="
    color: #909399;
    font-size: 12px;
    transition: transform 0.2s ease;
  ">
    ❯
  </span>
</div>
<div 
  class="profile-box"
  @click.stop="gotoHistoryShelf"
>
  <!-- 左侧文字 -->
  <div style="
    font-family: 'PingFang SC';
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  ">
    历史记录
  </div>

  <!-- 右侧箭头 -->
  <span class="arrow-icon" style="
    color: #909399;
    font-size: 12px;
    transition: transform 0.2s ease;
  ">
    ❯
  </span>
</div>
        <div>
          -----------------------------------------------
        </div>
        <div 
  class="profile-box"
  @click="logout"
>
  <!-- 左侧文字 -->
  <div style="
    font-family: 'PingFang SC';
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  ">
    登出账号
  </div>

  <!-- 右侧箭头 -->
  <span class="arrow-icon" style="
    color: #909399;
    font-size: 12px;
    transition: transform 0.2s ease;
  ">
    ❯
  </span>
</div>
      </div>
      <div 
      v-if="showBox == true"
      @mouseenter="loadenter" @mouseleave="loadleave"
      class="tip"
    >
      <p style="
      margin-bottom: 15px;
    color: #18191c;
    font-family: 'PingFang SC';
    font-size: 14px;
    line-height: 20px;">登录后你可以：</p>
<div style="
  margin-left: 15px;
  width: 360px;
  height: 60px;
  background-color: white;
  display: grid;               
  grid-template-rows: 1fr 1fr; 
  grid-template-columns: 1fr 1fr;              
">
<div style="display: flex; align-items: center; text-align: left; font-family: 'Comic Sans MS', cursive; color: #ff69b4; font-size: 14px;">
  你可以下腐竹开发的战旗
</div>
<div style="display: flex; align-items: center; text-align: left; font-family: 'Comic Sans MS', cursive; color: #4caf50; font-size: 14px;">
 你可以看腐竹推荐的漫画鸭
</div>
<div style="display: flex; align-items: center; text-align: left; font-family: 'Comic Sans MS', cursive; color: #2196f3; font-size: 14px;">
  你可以拿腐竹上传的资源喵
</div>
<div style="display: flex; align-items: center; text-align: left; font-family: 'Comic Sans MS', cursive; color: #ff9800; font-size: 14px;">
 腐竹也想不出来要啥了啦
</div>
</div>
<button
@click="Loading"
style="
  justify-items: center;
  top: 17%;
  left: 7%;
  width: 319px;
  height: 40px;
  border: none;
  border-radius: 20px;
  background: skyblue;
  box-shadow: 
    0 4px 8px skyblue,
    inset 2px 2px 4px rgba(255, 255, 255, 0.2);
  color: white;
  font-family: 'Comic Sans MS', cursive;
  font-size: 18px;
  letter-spacing: 2px;
  text-shadow: 1px 1px 2px #ff1493;
  transition: all 0.2s;
  cursor: pointer;
  position: relative;
">
   ✧⁺⸜(●˙▾˙●)⸝⁺✧ 立即登录喵～ ♡
  <!-- 添加bling bling小星星 -->
</button>
      </div>
    </div>

      <div class="head-quick" aria-label="快捷入口">
        <el-tooltip content="回到顶部" placement="bottom">
          <span class="quick-btn" role="button" tabindex="0" @click="scrollTop" @keydown.enter.prevent="scrollTop">
            <el-icon :size="18"><CaretTop /></el-icon>
          </span>
        </el-tooltip>
        <el-tooltip content="随机逛逛 · 跳入任意频道首页" placement="bottom">
          <span
            class="quick-btn"
            role="button"
            tabindex="0"
            @click="randomBrowse"
            @keydown.enter.prevent="randomBrowse"
          >
            <el-icon :size="18"><Grid /></el-icon>
          </span>
        </el-tooltip>
        <el-tooltip content="许可协议与著作权说明" placement="bottom">
          <span
            class="quick-btn"
            role="button"
            tabindex="0"
            @click="aboutLegalVisible = true"
            @keydown.enter.prevent="aboutLegalVisible = true"
          >
            <el-icon :size="18"><InfoFilled /></el-icon>
          </span>
        </el-tooltip>
      </div>

      <div class="quick-item msg-quick" @click="goMessages">
        消息<span v-if="msgUnread > 0" class="msg-badge">{{ msgUnread > 99 ? '99+' : msgUnread }}</span>
      </div>
      <div
        class="nav-history quick-item"
        @click="gotoHistoryShelf"
        :style="{ color: route.path === '/bookshelf' ? '#00aeec' : '' }"
      >
        历史记录
      </div>
    </div>
</div>

  <el-dialog
    v-model="aboutLegalVisible"
    title="关于本站与非商业许可"
    width="min(520px, 92vw)"
    destroy-on-close
    append-to-body
    class="legal-about-dialog"
  >
    <p class="legal-about-p">
      本站为非商业同人向演示站点。正文与评论等若无另行声明，适用
      CC BY-NC-SA 3.0 CN；转载请保留署名并以相同方式共享。媒资版权归原作者所有。
    </p>
    <p class="legal-about-p muted">备案：尚未办理 ICP 备案。</p>
    <template #footer>
      <el-button type="primary" @click="scrollToLegalBlock">跳到页脚全文</el-button>
      <el-button @click="aboutLegalVisible = false">关闭</el-button>
    </template>
  </el-dialog>

<div v-if="loading_page == 1" class="overlay"></div>
<div v-if="loading_page == 1" class="loading_page">
  <div class="chaijun">
    <div style="display: flex;justify-content: center; top: 10%; position: relative; margin-bottom: 40px;">
      <div class="erwei">扫描二维码登录</div>
      <div style="top: 20%; position: fixed;">
        <img src="/qer.jpg" alt="" style="width: 168px; height: 168px;">
      </div>
      <p style="top: 65%; position: fixed;" class="weixin">请使用微信扫码登录</p>
      <p style="top: 70%; position: fixed;" class="weixin">我的微信扫码登录果然有问题</p>
      </div>
      <div>
      <img v-if="chaijun == true" src="/tantou.png" alt="" style="width: 100px;  left:0%; top: 84.3%; position: fixed;">
      <img v-if="chaijun == false" src="/tantou2.png" alt="" style="width: 100px;  left:0%; top: 90.6%; position: fixed;">
    </div>
  </div>
  <div style="width: 1px; height: 90%; background-color: #cccccc; margin: 20px 0;"></div>
  <div class="load">
    <div @click="close">
      <img src="/关闭.png" alt="" style="width: 20px; height: 20px; position: fixed; left: 97%;top: 2%;">
    </div>
    <div style="display: flex;justify-content: center; top: 10%; position: relative; margin-bottom: 40px;">
      <div style="font-weight: 500;font-size: 18px;line-height: 20px; margin-right: 12px;" @click="phones1">密码登录</div>
      <div>|</div>
      <div style="font-weight: 500;font-size: 18px;line-height: 20px; margin-left: 12px;" @click="phones">手机登录</div>
    </div>
    <div v-if="phone == 0" style="top: 15%; position: relative; justify-content: center; display: flex;">
    <el-input
      v-model="loginForm.username"
      style="max-width: 400px;height: 50px; "
      placeholder="请输入账号"
    >
      <template #prepend>账号</template>
    </el-input>
  </div>
  <div v-if="phone == 1" style="top: 15%; position: relative; justify-content: center; display: flex;">
    <el-input
    v-model="telephone"
      style="max-width: 400px;height: 50px; "
      placeholder="请输入手机号"
    >
      <template #prepend>手机号</template>
      <template #append >
        <div @click="yanzheng">
          发送验证码
        </div>
      </template>
    </el-input>
  </div>
   <div v-if="phone == 0" style="display: flex;justify-content: center;  position: relative;margin-top: 43px; top: 4.5%;">
    <el-input
       @focus="handleFocus1"
       @blur = "handleFocus2"
       v-model="loginForm.password"
      style="max-width: 400px;height: 50px;"
      placeholder="请输入密码"
      
    >
      <template #prepend>密码</template>
    </el-input>
    
   </div>
   <div v-if="phone == 1" style="display: flex;justify-content: center;  position: relative;margin-top: 43px; top: 4.5%;">
    <el-input
       v-model="yanzheng1"
       @focus="handleFocus1"
       @blur = "handleFocus2"
      style="max-width: 400px;height: 50px;"
      placeholder="请输入验证码"
    >
      <template #prepend>验证码</template>
    </el-input>
   </div>
   
   <div style="position: relative; top: 12%; justify-content: center; display: flex;">
    <button style="width: 194px; height: 40px; z-index: 1002; margin-right: 5px;" class="zhuce" @click="phones">注册</button>
    <button  class="denglu" @click="entering">登录</button>
   </div>
   <div  style="position: relative; top: 15%; justify-content: center; display: flex;">
    <p class="weixin"> 未注册过的手机号，我们将自动帮你注册账号</p>
   </div>
    <div>
      <img v-if="chaijun == true" src="/tantou2 left.png" alt="" style="width: 100px;  left: 88%; top: 84.3%; position: fixed;">
      <img v-if="chaijun == false" src="/tantou left.png" alt="" style="width: 100px;  left: 88%; top: 90.6%; position: fixed;">
    </div>
  </div>
  
</div>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })
import { Search, CaretTop, Grid, InfoFilled } from '@element-plus/icons-vue';
import { onMounted, onUnmounted, ref, reactive, watch, nextTick } from 'vue';
import useUserStore from '@/stores/modules/users';
import { ElNotification } from 'element-plus'
import { getTime } from '@/utils/time'
import { useRouter,useRoute } from 'vue-router'
import { getAllSearch } from '@/api/AllSearch'
import { unreadNotificationCount } from '@/utils/acgnNotifications'
let $router = useRouter()
let times = getTime()
const showResults = ref(false)
const searchResults = ref([])
const searchText = ref("")
const account = ref("")
const password = ref("")
const telephone = ref("")
const yanzheng1 = ref()
const loading_page = ref(0)
const phone = ref(0)
const route = useRoute()
let useStore = useUserStore()
let loginForm = reactive({ username: 'moqi', password: '123456' })
const showBox = ref<boolean>(false)
const chaijun = ref<boolean>(true)
const isUserHovering = ref(false)
const aboutLegalVisible = ref(false)
const msgUnread = ref(0)
function refreshMsgUnread() {
  msgUnread.value = useStore.token ? unreadNotificationCount() : 0
}
let unreadPoll: ReturnType<typeof setInterval> | null = null

watch(
  () => useStore.loginWallTrigger,
  (v) => {
    if (v > 0) {
      loading_page.value = 1
      showBox.value = false
    }
  }
)
watch(() => useStore.token, () => refreshMsgUnread())
const allsearch = async (search: string) => {
  if (!search?.trim()) {
    searchResults.value = []
    showResults.value = false
    return
  }
  const result: any = await getAllSearch(search.trim())
  console.log(result?.data)
  searchResults.value =
    result && result.code === 200 && Array.isArray(result.data) ? result.data : []
  showResults.value = true
}
const selectResult = (item: any) => {
  console.log("选择了:", item);
  if (item.sourcetype === "轻小说") {
    $router.push({
      name:'NovelIntroduce',
      query: {
        novelid:item.id
    }
  })
  }
  if (item.sourcetype === "漫画") {
    $router.push({
      name:'ComicIntroduce',
      query: {
        comicid:item.id
    }
  })
  }
  if (item.sourcetype === "动漫") {
    $router.push({
      name:'Animationintroduce',
      query: {
        animationid:item.id
    }
  })
  }
  showResults.value = false;
  searchText.value = "";
};
const animation = () => {
  $router.push('/animation')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const comic = () => {
  $router.push('/comic')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const novel = () => {
  $router.push('/novel')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const gotoHistoryShelf = () => {
  $router.push('/bookshelf')
  window.scrollTo({ top: 0, behavior: 'smooth' })
  isUserHovering.value = false
}
function goFullSearch() {
  const q = searchText.value.trim()
  if (!q) return
  $router.push({ path: '/search', query: { q } })
  window.scrollTo({ top: 0, behavior: 'smooth' })
  showResults.value = false
}
function goMessages() {
  refreshMsgUnread()
  $router.push('/messages')
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function goUserCenter() {
  isUserHovering.value = false
  $router.push('/user/center')
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function goUserUpload() {
  isUserHovering.value = false
  $router.push('/user/upload')
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function goUserActivity() {
  isUserHovering.value = false
  $router.push('/user/activity')
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function randomBrowse() {
  const paths = ['/animation', '/comic', '/novel', '/galagame', '/shengdi', '/warflag'] as const
  const p = paths[Math.floor(Math.random() * paths.length)]
  $router.push(p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
async function scrollToLegalBlock() {
  aboutLegalVisible.value = false
  await nextTick()
  document.querySelector('.bottom')?.scrollIntoView({ behavior: 'smooth', block: 'end' })
}
const zhanqi = () => {
  $router.push('/warflag')
}
const galagame = () => {
  $router.push('/galagame')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const gotosd = () => {
  $router.push('/shengdi')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const  back=()=>{
  $router.push('/')
  window.scrollTo({
    top: 0,
    behavior: 'smooth' // 平滑滚动效果，可根据需要改为 'auto' 实现瞬间滚动
  });
}
const loadenter = () => {
  showBox.value = true
  console.log(showBox.value)
}
const loadleave = () => {
  showBox.value = false
  console.log(showBox.value)
}
const Loading = () => {
loading_page.value = 1
showBox.value = false
}
const entering = async() => {
try{
  await useStore.userLogin(loginForm)
  ElNotification({
      type: 'success',
      message: '欢迎回来',
      title: `Hi,${times}`,
      duration: 3000,
    })
    await useStore.userinfo()
    loading_page.value = 0
    console.log(useStore.avatar)
}catch (error) {
    console.log(error)
    ElNotification({
      type: 'error',
      message: (error as Error).message,
    })
  }

}


const userenter = () => {
  isUserHovering.value = true
}
const userleave = () => {
  isUserHovering.value = false
}
const yanzheng = () => {
  console.log(111)
}
const phones1 = () => {
phone.value = 0
}
const phones = () => {
phone.value = 1
}
const close = () => {
  loading_page.value = 0
  console.log(loading_page.value)
}
const handleFocus1 = () => {
  chaijun.value = false
}
const handleFocus2 = () => {
  chaijun.value = true
}
const logout  = async() => {
  await useStore.userLogout()
  // window.location.reload();
  $router.replace('/')
}
const handleClickOutside = (event: MouseEvent) => {
  const searchEl = document.querySelector('.search');
  if (searchEl && !searchEl.contains(event.target as Node)) {
    showResults.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  refreshMsgUnread()
  unreadPoll = setInterval(refreshMsgUnread, 9000)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (unreadPoll) clearInterval(unreadPoll)
})
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
.zhuce{
   top: 10%;
  -webkit-box-sizing: border-box;
    box-sizing: border-box;
    width: 194px;
    height: 40px;
    cursor: pointer;
    color: #18191c;
    background: #fff;
    border-radius: 8px;
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
  z-index: 1000; /* 确保遮罩层在其他元素之上 */
}
.loading_page{
  width: 820px;
  height: 430px;
  position: fixed;
  background-color: white;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1001; /* 确保加载页面在遮罩层之上 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2); /* 添加阴影效果 */
  display: flex;
  align-items: center; /* 垂直居中对齐 */
  justify-content: center;
}
.headfont{
    height: 60px; 
    width: 100px; 
    padding: 4px;
}
.el-input {
  border: none;
}
:deep(.el-input_wrapper){
    box-shadow: none;
    border: none;
}
.user-wrap{
  position: relative;
  flex: 0 0 auto;
  max-width: 28vw;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-wrap: wrap;
}
.head-quick {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding-right: 10px;
  margin-right: 4px;
  border-right: 1px solid #ebeef5;
  line-height: 1;
}
.quick-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #606266;
}
.quick-btn:hover {
  background: rgba(0, 174, 236, 0.1);
  color: #00aeec;
}
.quick-item {
  line-height: 40px;
  cursor: pointer;
  flex-shrink: 0;
}
.msg-quick .msg-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  text-align: center;
  vertical-align: middle;
}
.search-more {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #00aeec;
  cursor: pointer;
  border-bottom: 1px solid #f2f3f5;
}
.search-more:hover {
  background: #f0faff;
}
.search-empty-pill {
  padding: 8px 14px;
  font-size: 12px;
  color: #909399;
}
.kuang{
    height: 70px; 
    border-radius: 10px;
    outline-style: none ;
    padding: 13px 14px;
    width: 620px;
    font-size: 14px;
    font-weight: 700;
    font-family: "Microsoft soft";
}
::v-deep(.el-input__inner) {
  border: none !important; /* 确保覆盖默认边框 */
  background-color: #fff; /* 自定义背景色 */
  color: #333; /* 自定义字体颜色 */
}
.acgn:hover{
  cursor: pointer;
  transform: translateY(-5px);
  transition: all 0.5s ease
}
button:hover {
  transform: scale(1.02);
  background: linear-gradient(145deg, #ff8ad0 0%, #ff5ab8 100%);
  box-shadow: 
    0 6px 12px rgba(255, 110, 199, 0.4),
    inset 2px 2px 4px rgba(255, 255, 255, 0.3);
}
.avater {
  transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.left-group {
  display: flex;
  align-items: center;
  gap: 1rem;          /* logo 与导航之间的间距 */
}
.heads {
  display: flex;
  align-items: center;
  justify-content: space-between;   /* 左右贴边，中间自动居中 */
  gap: 1rem;
  padding: 0 1rem;
  height: auto;
  min-height: 5rem;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1500;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,.05);
}
.heads:hover{
/* background-color: #ff8ad0; */
}
button:active {
  transform: scale(0.98);
}
/* 聚焦时的样式 */
::v-deep(.el-input__inner:focus) {
  border-color: #66afe9;
  outline: 0;
  box-shadow: 0 0 8px rgba(102, 175, 233, 0.6);
}
.search{
  flex: 1 1 auto;
  max-width: clamp(300px, 40%, 620px);
  margin: 0 auto;          /* 关键：让搜索框自己居中 */
}
input:focus{
    border-color: #66afe9;
    outline: 0;
    -webkit-box-shadow: inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6);
    box-shadow: inset 0 1px 1px rgba(0,0,0,.075),0 0 8px rgba(102,175,233,.6)
}
.profile-box{
  width: 280pX;
  height: 30px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: white;
    cursor: pointer;
    transition: all 0.3s ease;
    margin: 10px 0;
}
.profile-box:hover {
  background: #f5f7fa;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.profile-box:hover .arrow-icon {
  color: #606266;
  transform: translateX(3px);
}
.search-results-dropdown {
  position: absolute;
  top: 100%;
  left: 25%;
  width: 60%;
  max-height: 400px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 2000;
}

/* 单个结果项样式 */
.result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background-color: #f9f9f9;
}

/* 图片样式 */
.result-img {
  width: 50px;
  height: 70px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 15px;
}

/* 名称样式 */
.result-name {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 类型样式 */
.result-type {
  padding: 4px 10px;
  background: #f0f7ff;
  color: #00aeec;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}
.tip{
  text-align: left; 
  width: 400px; 
  height: 230px; 
  position: absolute;
  top: calc(100% ); 
  left: 50%;
  transform: translateX(-50%);
  /* margin-top: 80px;   原来 8px → 现在 12px，往下多 4px */
  z-index: 1020;
  padding: 16px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,.1);
}
.avatar-wrapper{
  position: relative;   /* 关键：让子元素 absolute 相对它定位 */
}
.avatar{
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  vertical-align: middle;
  cursor: pointer;
  z-index: 1050 !important;
}
.nav-history {
  vertical-align: middle;
  cursor: pointer;
}
.nav-history:hover {
  color: #00aeec;
}
.usertip{
    position: absolute;
    top: calc(100% + 12px);   /* 头像底边再往下 12px */
    left: 0%;
    transform: translateX(-50%); /* 水平居中 */
    margin-top: 0;             /* 把原来的 3% 去掉 */
    text-align: left;
    background: white;
    border: 1px solid #ccc;
    border-radius: 8px;
    padding: 22px;
    width: 300px;
    height: 475px;
    z-index: 1000;
}
</style>

<style lang="scss">
.legal-about-p {
  line-height: 1.72;
  font-size: 14px;
  color: #303133;
  margin: 0 0 14px;
}
.legal-about-p.muted {
  margin-bottom: 0;
  color: #909399;
}
</style>
