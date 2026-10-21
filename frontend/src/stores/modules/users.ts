//创建用户相关的小仓库
import { defineStore } from 'pinia'
import { reqLogin, reqUserInfo, reqLogout } from '@/api/user'
import { GET_TOKEN, SET_TOKEN, REMOVE_TOKEN } from '@/utils/token'
//引入路由
import { constantRoute } from '@/router/routes'
let useUserStore = defineStore('User', {
  //函数
  state: () => {
    return {
      token: GET_TOKEN(), //用户标识token
      menuRoutes: constantRoute, //仓库存储生成菜单需要数组(路由)
      username: '',
      avatar: '',
      level:'',
      cartooncomicid:'',
      cartoonchapterlength:'',
      novelchapter:'',
      novelid:'',
      xunlilist:{},
      loginVisible: false,
      loginRedirect: '',
      loginReason: '',
    }
  },
  actions: {
    triggerLoginWall(redirect = '', reason = '登录后即可继续使用此功能') {
      this.loginRedirect = redirect
      this.loginReason = reason
      this.loginVisible = true
    },
    closeLogin() {
      this.loginVisible = false
      this.loginRedirect = ''
      this.loginReason = ''
    },
    clearSession() {
      this.token = ''
      this.username = ''
      this.avatar = ''
      this.level = ''
      REMOVE_TOKEN()
    },
    //用户登录方法
    async userLogin(data) {
      let result = await reqLogin(data)
      if (result.code == 200) {
        this.token = result.data as string
        SET_TOKEN(result.data as string)
        return 'ok'
      } else {
        throw new Error(result.msg || '登录失败，请检查账号和密码')
      }
    },
    //获取用户信息
    async userinfo() {
      //获取用户信息并存储于仓库中
      let result= await reqUserInfo()
      //如果获取成功，就存取信息
      if (result.code == 200) {
        this.username = result.data.username
        let av = result.data.avatar
        try {
          const o = localStorage.getItem('acgn_avatar_local_v1')?.trim()
          if (o) av = o
        } catch {
          //
        }
        this.avatar = av
        this.level = result.data.level
        return 'ok'
      } else {
        throw new Error(result.msg || '获取用户信息失败')
      }
    },
    /** 个人中心头像：仅存本机，下次进入仍覆盖服务端头像字段展示 */
    setLocalAvatar(dataUrl: string) {
      const v = dataUrl?.trim()
      if (!v) return
      try {
        localStorage.setItem('acgn_avatar_local_v1', v)
      } catch {
        //
      }
      this.avatar = v
    },
    //退出登录
    async userLogout() {
      try {
        await reqLogout()
      } finally {
        this.clearSession()
        this.closeLogin()
        try {
          localStorage.removeItem('acgn_avatar_local_v1')
        } catch {
          //
        }
      }
    },
  },
  getters: {},
})
export default useUserStore
