export const constantRoute = [
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('@/view/ActivityDetail/index.vue'),
    meta: {
      title: '活动详情',
      name: 'ActivityDetail',
    },
  },  {
    path: '/character/:id',
    name: 'Character',
    component: () => import('@/view/Character/index.vue'),
    meta: {
      title: '角色介绍',
      name: 'Character',
    },
  },
  {
    path: '/news/:id?',
    name: 'News',
    component: () => import('@/view/News/index.vue'),
    meta: {
      title: 'ACGN资讯',
      name: 'News',
    },
  },
  {
    path: '',
    name: 'home',
    component: () => import('@/view/main/index.vue'),
    meta: {
      title: '主页',
      name: 'home',
    },
  },
  {
    path: '/search',
    name: 'SiteSearch',
    component: () => import('@/view/Search/index.vue'),
    meta: {
      title: '全站搜索',
      name: 'SiteSearch',
    },
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/view/Messages/index.vue'),
    meta: {
      title: '消息',
      requiresAuth: true,
      name: 'Messages',
    },
  },
  {
    path: '/user/center',
    name: 'UserCenter',
    component: () => import('@/view/User/Center.vue'),
    meta: {
      title: '个人中心',
      requiresAuth: true,
      name: 'UserCenter',
    },
  },
  {
    path: '/user/upload',
    name: 'UserUpload',
    component: () => import('@/view/User/Upload.vue'),
    meta: {
      title: '我要上传',
      requiresAuth: true,
      name: 'UserUpload',
    },
  },
  {
    path: '/user/activity',
    name: 'UserActivity',
    component: () => import('@/view/User/Activity.vue'),
    meta: {
      title: '我的动态',
      requiresAuth: true,
      name: 'UserActivity',
    },
  },
  {
    path: '/shengdi',
    name: 'shengdi',
    component: () => import('@/view/shengdi/index.vue'),
    meta: {
      title: '圣地',
      name: 'shengdi',
    },
  },
  {
    path: '/warflag',
    name: 'warflag',
    component: () => import('@/view/WarFlag/Start.vue'),
    meta: {
      title: '战旗',
      requiresAuth: true,
      name: 'warflagi',
    },
  },
  {
    path: '/warflag/battle',
    name: 'warflagBattle',
    component: () => import('@/view/WarFlag/index.vue'),
    meta: {
      title: '战旗 · 战场',
      requiresAuth: true,
      name: 'warflagBattle',
    },
  },
  {
    path: '/shengdi/duizhao',
    name: 'duizhao',
    component: () => import('@/view/shengdi/duizhao.vue'),
    meta: {
      title: '对照',
      name: 'duizhao',
    },
  },
  {
    path: '/shengdi/addnew',
    name: 'addnew',
    component: () => import('@/view/shengdi/addnew.vue'),
    meta: {
      title: '新增地标',
      requiresAuth: true,
      name: 'addnew',
    },
  },
  {
    path: '/bookshelf',
    name: 'Bookshelf',
    component: () => import('@/view/Bookshelf/index.vue'),
    meta: {
      title: '历史记录',
      name: 'Bookshelf',
    },
  },
  {
    path: '/Animation',
    name: 'Animation',
    component: () => import('@/view/Animation/index.vue'),
    meta: {
      title: '番剧',
      name: 'Animation',
    },
  },
  {
    path: '/Animationintroduce',
    name: 'Animationintroduce',
    component: () => import('@/view/Animation/AnimationIntroduce/index.vue'),
    meta: {
      title: '番剧简介',
      name: 'Animationintroduce',
    },
  },
  {
    path: '/AnimationWatch',
    name: 'AnimationWatch',
    component: () => import('@/view/Animation/AnimationWatch/index.vue'),
    meta: {
      title: '番剧观看',
      name: 'AnimationWatch',
    },
  },
  {
    path: '/Comic',
    name: 'Comic',
    component: () => import('@/view/Comic/index.vue'),
    meta: {
      title: '漫画',
      name: 'Comic',
    },
  },
  {
    path: '/ComicIntroduce',
    name: 'ComicIntroduce',
    component: () => import('@/view/Comic/Comicintroduce/index.vue'),
    meta: {
      title: '漫画简介',
      name: 'ComicIntroduce',
    },
  },
  {
    path: '/Comicsee',
    name: 'Comicsee',
    props: true,
    component: () => import('@/view/Comic/Comicsee/index.vue'),
    meta: {
      title: '漫画正文',
      name: 'Comicsee',
    },
  },
  {
    path: '/Novel',
    name: 'Novel',
    component: () => import('@/view/Novel/index.vue'),
    meta: {
      title: '轻小说',
      name: 'Novel',
    },
  },
  {
    path: '/NovelIntroduce',
    name: 'NovelIntroduce',
    component: () => import('@/view/Novel/NovelIntroduce/index.vue'),
    meta: {
      title: '轻小说简介',
      name: 'NovelIntroduce',
    },
  },
  {
    path: '/Novelsee',
    name: 'Novelsee',
    component: () => import('@/view/Novel/Novelsee/index.vue'),
    meta: {
      title: '轻小说正文',
      name: 'Novelsee',
    },
  },
  {
    path: '/Galagame',
    name: 'Galagame',
    component: () => import('@/view/GalaGame/index.vue'),
    meta: {
      title: 'Galgame',
      name: 'Galagame',
    },
  },
  {
    path: '/GalgameIntroduce',
    name: 'GalgameIntroduce',
    component: () => import('@/view/GalaGame/GalgameIntroduce/index.vue'),
    meta: {
      title: 'Galgame 简介',
      name: 'GalgameIntroduce',
    },
  },
]
