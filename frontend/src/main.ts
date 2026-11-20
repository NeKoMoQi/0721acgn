import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/mobile.css'
import { zhCn } from 'element-plus/es/locales.mjs'
import App from './App.vue'
import router from './router'
import useUserStore from '@/stores/modules/users'
import { GET_TOKEN } from '@/utils/token'
import { ensureAcgnUserHydrated } from '@/utils/acgnLocal'
import { installImageOptimizer } from '@/utils/imageOptimizer'

installImageOptimizer()

// 发版后仍持有旧 index.html 的客户端，路由 chunk 可能已经 404（白屏/只剩背景）。
// Vite 的 preloadError 能捕获这种情况，自动刷新一次换成新版本；10 秒内只刷一次防循环。
const PRELOAD_RELOAD_KEY = 'acgn:preload-reload-at'
window.addEventListener('vite:preloadError', (event) => {
  event.preventDefault()
  try {
    const last = Number(sessionStorage.getItem(PRELOAD_RELOAD_KEY) || 0)
    if (Date.now() - last < 10000) return
    sessionStorage.setItem(PRELOAD_RELOAD_KEY, String(Date.now()))
  } catch {
    /* 隐私模式下 sessionStorage 不可用时仍然尝试刷新 */
  }
  window.location.reload()
})

const app = createApp(App)
import './assets/reset.css'
const pinia = createPinia()
app.use(pinia)

ensureAcgnUserHydrated().catch(() => {
  /* 后端未就绪时足迹等仍可在后续页面重试加载 */
})

app.use(router)
/** 等路由就绪后恢复资料，使凭证失效时能记住当前受保护页面。 */
router.isReady().then(() => {
  if (GET_TOKEN()) {
    useUserStore(pinia).userinfo().catch(() => {
      // 网络故障保留凭证；确认失效时由请求层清理并提示重新登录。
    })
  }
})
app.use(ElementPlus,{
    locale: zhCn
})
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }
// 引入leaflet
import 'leaflet/dist/leaflet.css'
//引入模版全局样式
import '@/style/index.scss'
//引入自定义插件对象：注册整个项目全局组件
import gloalComponent from '@/components'
app.use(gloalComponent)
import Headpage from '@/components/Headpage/index.vue'
import SvgIcon from '@/components/SvgIcon/index.vue'
app.component('SvgIcon',SvgIcon)
app.component('Headpage',Headpage)
import 'virtual:svg-icons-register'
app.mount('#app')
