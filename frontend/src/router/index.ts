import { createRouter, createWebHashHistory, type NavigationGuard } from 'vue-router';
import { constantRoute } from './routes';
import { GET_TOKEN } from '@/utils/token';
import useUserStore from '@/stores/modules/users';

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoute,
});

export const requireAccount: NavigationGuard = (to, from) => {
  if (to.meta.requiresAuth && !GET_TOKEN()) {
    useUserStore().triggerLoginWall(to.fullPath, `登录后即可访问「${to.meta.title || '此页面'}」`);
    // 站内点击留在原页；直接打开受保护地址时，以首页作为游客落点。
    return from.matched.length ? false : { path: '/', replace: true };
  }
  return true;
};

router.beforeEach(requireAccount);

export default router;
