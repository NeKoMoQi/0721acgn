<template>
    <div class="home-page">
        <Headpage class="head" ></Headpage>
        <div class="midbc" style="" >
        <div class="mainfont1">
        欢迎来到0721ACGN小窝～(≧ω≦)ﾉ✨<br>
        </div>
        <div class="mainfont2">
            Ciallo～(∠・ω< )⌒★
        </div>
        <div class="scroll-arrow" @click="scrollDown">
            <svgicon name="down" color="white" class="animated-icon" :class="{ 'scrolling': isScrolling }"></svgicon>
            </div>
            <div class="tsg" @click="getactivity(id)">

            </div>
        </div>


        <div class="behinds">
            <div class="zixun" @scroll="handscroll" ref="scrollcontainer" >
              <div class="zi1" v-for="(item,index) in rea">
                <div class="zifont1">
                  <div class="headfont">{{item.activityname}}</div>
                  <div class="desfont">{{item.activitydescribe}}</div>
              </div>
                <div class="ziimg1"><img :src=item.activityimg alt="" style="height: 100%; width: 100%;" class="imgs"></div>
              </div>
              <div v-if="isloading" class="tishi">加载中...请稍等哦^_^</div>
              <div v-if="!hasmore"  class="tishi">已经到底啦~亲(๑•̀ㅂ•́)و✧</div>
            </div>
            <div class="meiriuser">
              <div class="title"> 
              <div style="display: flex;">
                <div style="width: 200px; height: 150px; margin-left: -20px; margin-top: -20px;"><img src="/LOGO.png" style="height: 100%;width: 100%;" alt=""></div>
                <div style="margin-top: 25px; align-items: center;">
                  <div class="liulan"><h4 >{{liulan}}</h4> 次浏览</div>
                  <div class="liulan"><h4>{{ wenzhang }}</h4> 篇文章</div>
                  <div class="liulan"><h4>{{bianjizhe}}</h4> 编辑者</div>
                </div>
              </div>
              <div class="fenge"></div>
              <div v-if="wechat" class="weixin"><img src="/socialapp/wechat.jpg" alt="" style="width: 100px; height: 100px;"></div>
              <div v-if="qq" class="qq"><img src="/socialapp/qq.jpg" alt="" style="width: 100px; height: 100px;"></div>
              <div v-if="blbl" class="blbl"><img src="/socialapp/blbl.jpg" alt="" style="width: 100px; height: 100px;"></div>
              <div style="display: flex; align-items: center; justify-content: space-between;padding-left: 20px; padding-right: 20px; margin-top: 20px;">
                <div @mouseenter="wechatenter" @mouseleave="wechatleave"><img src="/png/wechat.png" alt="" style="width: 50px; height: 50px;"></div>
                <div @mouseenter="qqenter" @mouseleave="qqleave"><img src="/png/qq.png" alt="" style="width: 40px; height: 40px;"></div>
                <div @mouseenter="blenter" @mouseleave="blleave"><img src="/png/bilibili.png" alt="" style="width: 50px; height: 50px;"></div>
              </div>
              <div style="width: 80%; margin-left: 10%; margin-left: 10%; margin-top: 30px;"><button style="background-color: rgb(241,241,241); border: none; width: 100%; height: 40px;">随机文章？？ => Go</button></div>
              </div>
              <div class="character sidebar-note">
                <div class="hottitle">🌸欢迎来到小窝🌸</div>
                <p class="sidebar-note-copy">这里放着喜欢的作品、随手写的推荐，还有想去看看的地方。</p>
                <p class="sidebar-note-copy">从一部番剧、一本漫画或一卷轻小说开始，找到属于自己的小小快乐。欢迎来坐坐，慢慢看，慢慢逛～</p>
                <p class="sidebar-note-author">—— NeKoMoQi</p>
              </div>
              <div class="character">
                <div class="hottitle">🎲随机介绍🎲</div>
                <div style="display: flex; justify-content: center; margin-top: 15px;">
                <div style="width: 80px; height: 80px; "><img style="width: 100%; height: 100%; border-radius: 50%;" :src=roleavatar alt=""></div>
                </div>                
                <div style="display: flex; justify-content: center; margin-top: 15px;">
                  <div><h4>{{rolename}}</h4></div>
                </div>
                <div style="display: flex; justify-content: flex-start; margin-top: 15px;">
                  <div>性别:</div><div>{{rolesex}}</div>
                </div>
                <div style="display: flex; justify-content: flex-start; margin-top: 5px;">
                  <div>生日:</div><div>{{ roelbirth}}</div>
                </div>
                <div style="display: flex; justify-content: flex-start; margin-top: 5px;">
                  <div style="width: 40px; height: 10px; writing-mode: horizontal-tb;">萌点:</div><div style=" overflow: hidden; 
                  text-overflow: ellipsis;white-space: nowrap; width: 210px;">{{ roletalent}}</div>
                </div>
                <div style="width: 50%; margin-left: 56%;height: 30px; margin-top: 15%;"><button style="width: 90%; height: 100%; border: none; background-color: white;">了解更多--></button></div>
                

              </div>
              <div class="birthday">
                <div class="hottitle">🎈今日生日🎈</div>
                <div style="width: 100%; 
            height: 90px; 
            display: flex; 
            justify-content: center;
            position: relative;"> <!-- 父容器添加相对定位 -->
    <!-- 底层角色保持原有样式 -->
    <div style="width: 90px; 
            height: 90px;
            border-radius: 50%;
            overflow: hidden;
            position: relative;
            margin-top: 5px;"> <!-- 新增容器 -->
</div>

    <!-- 叠加角色添加绝对定位 -->
    <img style="width: 80px;
                height: 80px; 
                border-radius: 50%;
                position: absolute;  
                left: 50%;  
                top: 50%;  
                transform: translate(-50%, -50%);  
                border: 2px solid white; 
                box-shadow: 0 0 8px rgba(0,0,0,0.2);" 
         :src=birthavatar 
         alt="">
</div>
                <div style="width: 100%; 
            height: 25px; 
            margin-top: 20px; 
            display: flex; 
            justify-content: center;
            position: relative;
            font-weight: 600;
            font-size: 18px;
            color: #ff6b6b; margin-top: 18px;">
    <span style="background: linear-gradient(45deg, #ff6b6b, #ff8e53);
                  -webkit-background-clip: text;
                  color: transparent;
                  letter-spacing: 1px;
                  text-shadow: 0 2px 4px rgba(255,107,107,0.2);
                  position: relative;
                 ">
        🎂 {{birthname}} 🎂
    </span>
    
    <!-- 装饰线 -->
    <div style="position: absolute;
                bottom: -5px;
                width: 120px;
                height: 2px;
                background: linear-gradient(90deg, 
                    transparent 0%, 
                    #ff9f43 30%, 
                    #ff6b6b 70%, 
                    transparent 100%);"></div>
</div>
              </div>
              <div class="hot">
  <!-- 标题样式 -->
  <div class="hottitle">🔥 最近热门 🔥</div>
  <div style="display: flex;   justify-content: space-between; margin-top:10px ;">
    <div class="littletitle">文章名</div>
    <div class="littletitle">点击量</div>
  </div>
  <!-- 列表项样式 -->
  <div 
    v-for="(item, index) in hotlist" 
    class="hot-item"
  >
    <!-- 左侧活动名称 -->
    <p class="activity-name">{{index+1}}.{{ item.activityname }}</p>
    
    <!-- 右侧点击量 -->
    <div class="click-wrapper">
      <span class="click-count">{{ item.click }}</span>
      <el-icon class="arrow-icon"><Right /></el-icon>
    </div>
  </div>
</div>
              <div class="character sidebar-note">
                <div class="hottitle">📌小窝公告📌</div>
                <p class="sidebar-note-copy">游客可以直接浏览和搜索作品，挑一部感兴趣的慢慢看吧。</p>
                <p class="sidebar-note-copy">想接着上次的进度？到「个人中心 · 继续观看 / 阅读」，就能找到上次看到的集数、章节或卷数。</p>
                <p class="sidebar-note-copy">小窝的内容会逐步补充。遇到缺章、链接失效或资料有误，欢迎带上作品名和章节反馈。</p>
              </div>
              <div class="fenlei">
                <div class="hottitle">🔀分类🔀</div>
                <div style="display: flex;   justify-content: space-between; margin-top:10px ;">
                  <div class="littletitle">类别</div>
                  <div class="littletitle">数目</div>
                </div>
                <div 
    v-for="(item, index) in leibielist" 
    class="hot-item"
  >
    <!-- 左侧活动名称 -->
    <p class="activity-name">{{index+1}}.{{ item.actleibie }}</p>
    
    <!-- 右侧点击量 -->
    <div class="click-wrapper">
      <span class="click-count">{{ item.count }}</span>
      <el-icon class="arrow-icon"><Right /></el-icon>
    </div>
  </div>
              </div>
              <div class="shengdi">
                <div class="hottitle">🌍圣地巡礼热门榜🌍</div>
                <div style="display: flex;   justify-content: space-between; margin-top:10px ;">
                  <div class="littletitle">番剧名</div>
                  <div class="littletitle">巡礼地点数目</div>
                </div>
                <div 
    v-for="(item, index) in shengdihot" 
    class="hot-item"
  >
    <!-- 左侧活动名称 -->
    <p class="activity-name">{{index+1}}.{{ item.fanju }}</p>
    
    <!-- 右侧点击量 -->
    <div class="click-wrapper">
      <span class="click-count">{{ item.xunlicount }}</span>
      <el-icon class="arrow-icon"><Right /></el-icon>
    </div>
  </div>
              </div>
            </div>
           
        </div>
        <!-- <div class="search_lan"></div> -->
        <!-- <div class="bottom">
          <div style="margin-left: 17.5%;margin-right: 17.5% ; padding: 0px;">
            <div style="width: 120px; height: 90px;"><img style="width: 100%; height: 100%;" src="/LOGO.png" alt=""></div>
            <div><p style="font-size: 14px;font-weight: 400;">本站全部内容禁止商业使用。文本内容除另有声明外，均在知识共享 署名-非商业性使用-相同方式共享 3.0 中国大陆（CC BY-NC-SA 3.0 CN）许可协议下提供，附加条款亦可能应用，详细信息参见0721Acgn:著作权信息。其他类型作品著作权归属原作者，如有授权遵照授权协议使用。</p></div>
            <div class="divider"></div>
            <div style="display: flex; justify-content: center;margin-top: 10px;"><div>
              网站备案，我还没搞到
            </div></div>
          </div>
      </div> -->
      <!-- <behind></behind> -->
       <Behind></Behind>
    </div>

</template>
<script setup lang="ts">
import behind from '@/components/behind/index.vue'
import svgicon from '@/components/SvgIcon/index.vue'
import {ref,onMounted, reactive,onBeforeMount} from 'vue'
import {getpageActivity,getAllActivity,getActivityHot,getActivityLeibie} from '@/api/activity'
import { getPeopleCount } from '@/api/peoplecount'
import {getAcgnUserRandom,getAcgnUserBirth} from '@/api/acgnuser/index'
import { useRoute,useRouter } from 'vue-router'
import { getShengDiHot } from '@/api/shengdi/index'
import { getScrollTop } from 'element-plus/es/utils/index.mjs'
const scrollcontainer = ref(null)
const qq = ref<boolean>(false)
const wechat = ref<boolean>(false)
const blbl = ref<boolean>(false)
const isScrolling = ref(false)
const scrollDistance = ref(0)
/** 占位：首页「图书馆」等区域点击入口 */
const getactivity = (_activityId: number) => {}
const leibielist  =  ref( [] )
const id = ref(1)
const imgsrc = ref()
const zihead = ref()
const zides = ref()
const rea = ref([])
const hotlist = ref([])
const bianjizhe = ref()
const wenzhang = ref()
const liulan = ref()
const roleavatar = ref()
const rolename = ref()
const roelbirth = ref()
const roletalent = ref()
const rolesex = ref()
const  birthavatar = ref()
const birthname = ref()
const shengdihot = ref([])
const isloading = ref(false)
const hasmore = ref(true)
const currentpage = ref(2)
const getpage=async()=>{
  const pageresult = await getpageActivity(currentpage.value)
  if(pageresult.data.length<=9){
    hasmore.value = false
  }
  rea.value.push(...pageresult.data)
  currentpage.value++
  isloading.value = false
}
const loadpage = () => {
  if(isloading.value||!hasmore.value) return
  console.log('到底了')
  isloading.value = true
  getpage()
}
const handscroll =()=>{
  const contianer =  scrollcontainer.value
  const zuidiheight = 200
  if(!contianer) return
  if(contianer.scrollHeight - (contianer.scrollTop+contianer.clientHeight)<zuidiheight){
    loadpage()
  }

}
const qqenter = () => {
  qq.value = true
}
const qqleave = () => {
  qq.value = false
}
const wechatenter = () => {
  wechat.value = true
}
const wechatleave = () => {
  wechat.value = false
}
const blenter = () => {
  blbl.value = true
}
const blleave = () => {
  blbl.value = false
}
onMounted(async () => {
  const [
    birthRes,
    roleRes,
    resultRes,
    result1Res,
    hotRes,
    liebieRes,
    hotshengdiRes,
  ] = await Promise.allSettled([
    getAcgnUserBirth(),
    getAcgnUserRandom(),
    getAllActivity(),
    getPeopleCount(),
    getActivityHot(),
    getActivityLeibie(),
    getShengDiHot(),
  ])

  if (hotshengdiRes.status === 'fulfilled' && hotshengdiRes.value?.data !== undefined) {
    shengdihot.value = hotshengdiRes.value.data
  }
  if (liebieRes.status === 'fulfilled' && liebieRes.value?.data !== undefined) {
    leibielist.value = liebieRes.value.data
  }
  if (birthRes.status === 'fulfilled' && birthRes.value?.data) {
    const b = birthRes.value.data
    birthavatar.value = b.avatarurl
    birthname.value = b.name
  }
  if (roleRes.status === 'fulfilled' && roleRes.value?.data) {
    const r = roleRes.value.data
    rolesex.value = r.sex
    rolename.value = r.name
    roelbirth.value = r.birthday
    roleavatar.value = r.avatarurl
    roletalent.value = r.talent
  }
  if (hotRes.status === 'fulfilled' && hotRes.value?.data !== undefined) {
    hotlist.value = hotRes.value.data
  }
  if (resultRes.status === 'fulfilled' && resultRes.value?.data !== undefined) {
    rea.value = resultRes.value.data
  }
  if (result1Res.status === 'fulfilled' && result1Res.value?.data) {
    const p = result1Res.value.data
    bianjizhe.value = p.bianjizhe
    liulan.value = p.liulan
    wenzhang.value = p.wenzhang
  }
  console.log(rea.value)
})
const getDocumentHeight = () => {
  return Math.max(
    document.body.scrollHeight,
    document.documentElement.scrollHeight,
    document.body.offsetHeight,
    document.documentElement.offsetHeight,
    document.body.clientHeight,
    document.documentElement.clientHeight
  )
}
//检测到下滑到最底层
//执行分页查询
const scrollDown = () => {
  if (isScrolling.value) return
  
  isScrolling.value = true
  const startY = window.scrollY
  const targetY = startY + window.innerHeight
  const maxY = getDocumentHeight() - window.innerHeight
  const finalY = targetY
  
  const duration = 800
  const startTime = performance.now()

  const animateScroll = (currentTime: number) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    // 使用缓动函数
    const ease = (t: number) => {
      return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1
    }
    
    window.scrollTo(0, startY + (finalY - startY) * ease(progress))
    
    if (progress < 1) {
      requestAnimationFrame(animateScroll)
    } else {
      isScrolling.value = false
      scrollDistance.value = finalY
    }
  }

  requestAnimationFrame(animateScroll)
}
</script>

<style scoped>
.home-page{
  position: relative;
  min-height: 100vh;
  background-color: rgb(241,241,241);
  overflow-x: hidden;
}
.tishi{
  text-align: center;    /* 文字居中 */
  padding: 1rem;         /* 内边距 */
  color: #666;           /* 文字颜色 */
  font-size: 0.9rem;     /* 字体大小 */
  background: #f8f9fa;   /* 浅灰色背景 */
  border-radius: 4px;    /* 圆角 */
  margin: 10px 0;        /* 上下外边距 */
  border: 1px solid #e9ecef;
}
.littletitle{
  font-size: 16px;
  font-weight: 600;
  color: #333;
  padding-bottom: 10px;
}
.hottitle {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding-bottom: 10px;
}
.hot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  transition: all 0.3s ease;
  border-bottom: 1px solid #f5f5f5;
  
}

/* 最后一项去底边 */
.hot-item:last-child {
  border-bottom: none;
}

/* 活动名称样式 */
.activity-name {
  color: #666;
  font-size: 14px;
  margin: 0;
  flex-grow: 1;
  padding-right: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 点击量容器 */
.click-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ff4444;
}

/* 点击量数字 */
.click-count {
  font-size: 13px;
  font-weight: 500;
}

/* 箭头图标 */
.arrow-icon {
  color: #ff4444;
  font-size: 14px;
  margin-top: 1px;
}

/* 悬停效果 */
.hot-item:hover {
  background: #f9f9f9;
  transform: translateX(4px);
  cursor: pointer;
  border-radius: 4px;
}
.fenge{
  width: 100%;
  height: 1px;
  background: linear-gradient(
  to right,
  transparent 0%, 
    black 40%,
    black 60%,
  transparent 100%
   );
}
.weixin{
  width: 120px;
  height: 120px;
  top: 32%;
  left: -10.5%;
  position: absolute;
  margin-top: -70px;
  padding: 20px;
}
.qq{
  width: 120px;
  height: 120px;
  top: 32%;
  left: 25%;
  position: absolute;
  margin-top: -70px;
  padding: 20px;
}
.blbl{
  width: 120px;
  height: 120px;
  top: 32%;
  left: 60%;
  position: absolute;
  margin-top: -70px;
  padding: 20px;
}
.liulan{
  display: flex;
  align-items: center; /* 垂直居中 */
  justify-content: flex-end;
}
.headfont{
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.05em;
  position: relative;
  padding-left: 12px;
}
.desfont{

  height: 100%;
  width: 100%;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 15px;
  color: #666;
  padding-left: 12px;
  line-height: 1.7;
  font-weight: 300;
  letter-spacing: 0.03em;
  text-indent: 2em; /* 中文标准首行缩进 */
  display: -webkit-box;
  -webkit-line-clamp: 6;    /* 限制行数 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;        /* 需要明确行高 */
  max-height: 9em;         /* 行高 × 行数 */
  max-width: 100%;          /* 根据父容器宽度限制 */
}
.imgs{
  width: 100%;
  height: 100%;
  object-fit: cover;       /* 关键属性：保持比例填充容器 */
  object-position: center; /* 焦点居中 */
  position: absolute;      /* 绝对定位配合容器relative */
  top: 0;
  left: 0;
}
.title{
  position: relative;
  width: 100%;
  height: 12%;
  margin-bottom: 30px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}
.title:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.character{
  width: 100%;
  height: 14%;
  margin-bottom: 30px;
  padding: 20px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
  
}
.character:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.birthday{
  padding: 20px;
  width: 100%;
  height: 8%;
  margin-bottom: 30px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}
.birthday:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.hot{
  padding: 20px;
  width: 100%;
  height: 21%;
  margin-bottom: 30px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}
.hot:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.shengdi{
  padding: 20px;
  width: 100%;
  height: 20%;
  margin-bottom: 30px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}
.shengdi:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.fenlei{
  padding: 20px;
  width: 100%;
  height: 18.5%;
  margin-bottom: 30px;
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}
.fenlei:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.zifont1{
  width: 55%;
  height: 100%;
  padding: 20px;
}
.ziimg1{
  width: 45%;
  height: 100%;
  position: relative;      /* 为图片容器创建定位上下文 */
  overflow: hidden;        /* 隐藏超出容器的部分 */
  min-height: 120px;       /* 根据实际情况设置最小高度 */
  transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.zi1 {
  /* 基础布局保持不变 */
  position: relative;
  justify-content: space-between;
  display: flex;
  height: 10%;
  margin-bottom: 30px;

  /* 新增强化样式 */
  border: 1px solid rgba(224, 224, 224, 0.8); /* 更淡的灰色边框 */
  background: #ffffff;
  border-radius: 6px; /* 圆角增强立体感 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.1), /* 外阴影增加深度 */
    inset 0 1px 2px rgba(255, 255, 255, 0.5); /* 内阴影增加立体层次 */
  transition: all 0.3s ease; /* 添加过渡效果 */
}

/* 悬停时的立体效果增强 */
.zi1:hover {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px); /* 轻微上浮效果 */
}
.zi1:hover .imgs{
  transform: scale(1.1) translate(0%, 0%);
}
.imgs {
  transition: transform 0.4s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.zixun{
  overflow-y: auto; /* 启用垂直滚动 */
  padding-right: 15px; /* 避免滚动条遮挡内容 */
  width: 65%;
  height: 2587px;
  border-radius: 10px;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
  
}
.meiriuser{
  width: 30%;
  height: auto;
}
.sidebar-note{
  height: auto;
}
.sidebar-note-copy{
  margin: 10px 0 0;
  color: #666;
  font-size: 15px;
  line-height: 1.7;
}
.sidebar-note-author{
  margin: 12px 0 0;
  text-align: right;
  color: #666;
  font-size: 14px;
}
.scrolling {
  animation: none !important;
  opacity: 0.7;
  transform: translateY(4px);
}

.tsg {
  transition: transform 0.8s cubic-bezier(0.22, 0.61, 0.36, 1);
  transform: translateY(calc(var(--scroll-y) * 0.1px));
}
@keyframes quickBounce {
  0% { transform: scale(1); }
  30% { transform: scale(0.92); }
  60% { transform: scale(1.05); }
  100% { transform: scale(1); }
}
@keyframes bounceWithFade {
  0%, 100% {
    transform: translateY(0);
    opacity: 0.8;
  }
  30% {
    opacity: 1;
    filter: drop-shadow(0 4px 6px rgba(255,255,255,0.3));
  }
  50% {
    transform: translateY(-20px);
    opacity: 0.6;
  }
  70% {
    transform: translateY(-10px);
  }
}
@keyframes smoothBounce {
  0%, 100% {
    transform: translateY(0);
    opacity: 0.9;
  }
  20% {
    transform: translateY(-8px);
    opacity: 1;
  }
  40% {
    transform: translateY(-16px);
    opacity: 0.8;
  }
  60% {
    transform: translateY(-8px);
    opacity: 0.9;
  }
  80% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

/* 图标动画样式 */
.animated-icon {
  animation: bounceWithFade 2s ease-in-out infinite;
  transition: all 0.3s;
  width: 40px;
  height: 40px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
  
  /* 添加悬浮效果 */
  &:hover {
    transform: scale(1.1);
    opacity: 1 !important;
  }
}
.scroll-arrow:active .animated-icon {
  animation: quickBounce 0.4s;
}

@keyframes quickBounce {
  0% { transform: scale(1); }
  50% { transform: scale(0.9); }
  100% { transform: scale(1); }
}
.scroll-arrow {
    position: absolute;
    bottom: 40px;
    left: 50%;
    top: 25%;
    cursor: pointer;
    z-index: 2000;
    animation: bounce 2s infinite;
}
.animated-icon {
  animation: smoothBounce 2.4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
  transform: translateZ(0); /* 触发GPU加速 */
  will-change: transform, opacity;
}
.scroll-arrow {
  animation: none !important; /* 移除容器动画 */
  perspective: 1000px; /* 创建3D渲染上下文 */
}

.behinds{
  display: flex;
  justify-content: space-between; /* 左右分布 */
  align-items: flex-start; /* 顶部对齐 */
  gap: 40px; /* 元素间距 */
  width: min(1440px, calc(100vw - 64px));
  left: 50%;
  min-height: 2667px;
  height: auto;
  margin-left: auto;
  margin-right: auto;
  padding: 40px 0;
  margin-bottom: 200px;
  /* background-image: url('/behindpic.jpg'); */
}
.qiehuan{
  display: flex;
  gap: 40px; /* 元素间距 */
  width: calc(100vw - 480px);
  height: 30px;
  margin-left: 240px;
  margin-right: 240px;
  padding: 40px;
  border: 1px solid black;
  /* background-image: url('/behindpic.jpg'); */
}
.head {
    position: fixed; /* 固定在屏幕上方 */
    top: 0%; /* 距离顶部为0 */
    left: 0%; /* 距离左侧为0 */
    right: 0%; /* 距离右侧为0 */
    z-index: 1500; /* 确保在最上层显示 */
    background: #ffffff; /* 添加背景色，避免内容被遮挡 */
}
.head:hover{
  background-color: #ff99cc;
}
.mainfont1 {
    position: absolute;
    top: 30%;
    left: -22%;
    font-family: 'Hachi Maru Pop', cursive, 'Comic Sans MS', 幼圆, sans-serif;
    font-size: 30px;
    color: #ff69b4;
    text-align: center;
    max-width: 80%;
    position: relative;
    background: linear-gradient(45deg, #ff99cc, #ffb6c1);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: 2px 2px 0px #ffffff;
    animation: float 3s ease-in-out infinite;
}
.mainfont2 {
    position: absolute;
    font-family: 'Hachi Maru Pop', cursive, 'Comic Sans MS', 幼圆, sans-serif;
    font-size: 20px;
    color: #ff69b4;
    text-align: center;
    top: 35%;
    right: 25%;
    max-width: 80%;
    position: relative;
    background: linear-gradient(45deg, #ff99cc, #ffb6c1);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: 2px 2px 0px #ffffff;
    animation: float 3s ease-in-out infinite;
}
.bc{
    background-image:url(/BCPHOTO/acgnbc4.png) ;
    width: 100vw;
    height: 100vh;
    background-repeat: no-repeat;
    background-attachment: fixed;
    
}
.midbc{
    position: relative;
    /* 后续只需要调这 3 个值，tsg 会以右下角为锚点自适应 */
    --tsg-right: clamp(8px, 1.2vw, 22px);
    --tsg-bottom: clamp(-70px, -200vh, -8px);
    --tsg-width: clamp(220px, 18vw, 300px);
    background-image: url('/tsgsit.jpg'); 
    width: 100vw;
    height: 100vh;
    background-repeat: no-repeat;
    background-size: cover;
    background-position: center
}
.tsg {
  position: absolute;
  right: var(--tsg-right);
  bottom: var(--tsg-bottom);
  width: var(--tsg-width);
  aspect-ratio: 2 / 3;
  background-image: url('/TSG.png');
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center bottom;
  transform: none;
  z-index: 1; /* 确保悬浮在上方 */
}
.search_lan{
  margin-left: 17.5%;
  margin-right: 17.5%;
  width: 65%;
  height: 60px;
  background-color: rebeccapurple;
  padding: 40px;
}
@media (max-width: 1366px) {
  .behinds{
    width: calc(100vw - 48px);
    gap: 24px;
    padding: 32px 0;
  }

  .zixun{
    width: 63%;
    height: 2603px;
  }

  .meiriuser{
    width: 33%;
  }

  .mainfont1{
    font-size: 28px;
  }

  .mainfont2{
    font-size: 18px;
    right: 18%;
  }

  .midbc{
    --tsg-width: clamp(200px, 19vw, 280px);
  }
}
@media (max-width: 1100px) {
  .behinds{
    width: calc(100vw - 32px);
    min-height: 2400px;
    height: auto;
    gap: 20px;
    padding: 28px 0;
    margin-bottom: 140px;
  }

  .zixun{
    width: 60%;
    height: 2344px;
  }

  .meiriuser{
    width: 36%;
  }

  .mainfont1{
    font-size: 24px;
    left: -10%;
    max-width: 72%;
  }

  .mainfont2{
    top: 37%;
    right: 14%;
    font-size: 16px;
    max-width: 60%;
  }

  .midbc{
    --tsg-right: clamp(4px, 0.6vw, 12px);
    --tsg-bottom: clamp(-54px, -8vh, -18px);
    --tsg-width: clamp(180px, 22vw, 240px);
  }
}
@media (max-width: 900px) {
  .midbc{
    min-height: 720px;
    background-position: 62% center;
    --tsg-right: 0px;
    --tsg-bottom: -24px;
    --tsg-width: clamp(160px, 28vw, 210px);
  }

  .mainfont1{
    top: 22%;
    left: 4%;
    max-width: 60%;
    font-size: 22px;
  }

  .mainfont2{
    top: 32%;
    left: 8%;
    right: auto;
    max-width: 52%;
    font-size: 15px;
  }

  .behinds{
    flex-direction: column;
    width: calc(100vw - 24px);
    min-height: 0;
    height: auto;
    gap: 20px;
    padding: 24px 0 32px;
    margin-bottom: 100px;
  }

  .zixun,
  .meiriuser{
    width: 100%;
    height: auto;
  }

  .zixun{
    overflow: visible;
    padding-right: 0;
  }

  .zi1{
    flex-direction: column;
    height: auto;
  }

  .zifont1,
  .ziimg1{
    width: 100%;
  }

  .ziimg1{
    min-height: 220px;
    height: 220px;
  }

  .title,
  .character,
  .birthday,
  .hot,
  .fenlei,
  .shengdi{
    height: auto;
  }
}
@media (max-width: 640px) {
  .midbc{
    min-height: 640px;
    --tsg-width: 170px;
    --tsg-right: -10px;
    --tsg-bottom: -18px;
  }

  .mainfont1{
    top: 20%;
    left: 4%;
    max-width: 62%;
    font-size: 18px;
  }

  .mainfont2{
    top: 29%;
    left: 6%;
    max-width: 56%;
    font-size: 13px;
  }

  .scroll-arrow{
    top: auto;
    bottom: 22px;
  }

  .behinds{
    width: calc(100vw - 16px);
    padding: 16px 0 24px;
  }

  .zifont1{
    padding: 16px;
  }

  .headfont{
    font-size: 18px;
  }

  .desfont{
    font-size: 14px;
    -webkit-line-clamp: 5;
    max-height: 7.5em;
  }

  .title,
  .character,
  .birthday,
  .hot,
  .fenlei,
  .shengdi{
    margin-bottom: 20px;
  }
}
</style>
