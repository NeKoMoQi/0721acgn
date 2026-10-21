import axios from 'axios'
import { ElMessage } from 'element-plus'
import useUserStore from '@/stores/modules/users'
import { getOrCreateAcgnClientId } from '@/utils/acgnLocal'
import router from '@/router'

function requireLogin(failedToken?: unknown) {
  const userStore = useUserStore()
  // 旧请求的延迟响应不能清掉刚登录取得的新凭证，也不能在取消后反复弹窗。
  if (failedToken && failedToken !== userStore.token) return
  const route = router.currentRoute.value
  userStore.clearSession()
  if (!userStore.loginVisible) {
    userStore.triggerLoginWall(route.meta.requiresAuth ? route.fullPath : '', '登录状态已失效，请重新登录；也可以继续以游客浏览')
  }
  if (route.meta.requiresAuth) void router.replace('/')
}
//用axios对象的create方法，创建axios实例
let request = axios.create({
  // 基础路径会携�?api
  baseURL:'/api',
  timeout: 5000, //超时时间
})
//请求拦截�
request.interceptors.request.use((config) => {
  //获取仓库及内部token
  let usestore = useUserStore()
  if (usestore.token) {
    config.headers.token = usestore.token
  }
  ;(config.headers as Record<string, string>)['X-Acgn-Client'] = getOrCreateAcgnClientId()
  return config
})
//响应拦截�
request.interceptors.response.use(
  // 成功的回调，可以把data取出�
(response) => {
    if (response.data?.msg === 'NOT_LOGIN') {
      requireLogin(response.config.headers.token)
      return Promise.reject(new Error('登录状态已失效，请重新登录'))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      requireLogin(error.config?.headers?.token)
      return Promise.reject(new Error('登录状态已失效，请重新登录'))
    }
    //失败回调，处理网络错�
let msg = ''
    let status = error.response?.status
    switch (status) {
      case 403:
        msg = '无权访问'
        break
      case 404:
        msg = '请求地址错误'
        break
      case 500:
        msg = '服务器暂时无法处理请求'
        break
      default:
        msg = error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络连接失败，请稍后重试'
    }
    //提示错误信息
    ElMessage({
      type: 'error',
      message: msg,
    })
    return Promise.reject(new Error(msg))
  },
)
export default request
