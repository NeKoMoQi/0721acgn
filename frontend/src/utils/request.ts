import axios from 'axios'
import { ElMessage } from 'element-plus'
import useUserStore from '@/stores/modules/users'
import { getOrCreateAcgnClientId } from '@/utils/acgnLocal'
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
    return response.data
  },
  (error) => {
    //失败回调，处理网络错�
let msg = ''
    let status = error.response?.status
    switch (status) {
      case 401:
        msg = 'token过期'
        break
      case 403:
        msg = '无权访问'
        break
      case 404:
        msg = '请求地址错误'
        break
      case 500:
        msg = '服务器出现问�'
        break
      default:
        msg = '无网�'
    }
    //提示错误信息
    ElMessage({
      type: 'error',
      message: msg,
    })
    return Promise.reject(error)
  },
)
export default request
