//创建用户相关的小仓库
import { defineStore } from 'pinia'
import { reqLogin, reqUserInfo, reqLogout } from '@/api/user'
import { GET_TOKEN, SET_TOKEN, REMOVE_TOKEN } from '@/utils/token'
//引入路由
import { constantRoute } from '@/router/routes'
let REMOVE = REMOVE_TOKEN()
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
      /** 递增以触发全局登录弹层（见 Headpage watch） */
      loginWallTrigger: 0,
    }
  },
  actions: {
    /** 非首页且未登录时由路由守卫调用，打开登录窗口 */
    triggerLoginWall() {
      this.loginWallTrigger += 1
    },
    //用户登录方法
    async userLogin(data) {
      let result = await reqLogin(data)
      console.log(result)
      if (result.code == 200) {
        this.token = result.data as string
        SET_TOKEN(result.data as string)
        console.log(result)
        return 'ok'
      } else {
        return Promise.reject(new Error(result.data))
      }
    },
    //获取用户信息
    async userinfo() {
      //获取用户信息并存储于仓库中
      console.log('1')
      let result= await reqUserInfo()
      //如果获取成功，就存取信息
      console.log(result)
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
        return Promise.reject(new Error(result.message))
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
      let result: any = await reqLogout()
      if (result.code == 200) {
        //目前没有mock接口：退出登录接口(通知服务器本地用户唯一标识失效)
        this.token = ''
        this.username = ''
        this.avatar = ''
        try {
          localStorage.removeItem('acgn_avatar_local_v1')
        } catch {
          //
        }
        REMOVE_TOKEN()
        return 'ok'
      } else {
        return Promise.reject(new Error(result.message))
      }
    },
  },
  getters: {},
})
export default useUserStore
