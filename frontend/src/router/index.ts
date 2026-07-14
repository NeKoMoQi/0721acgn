import { createRouter, createWebHashHistory } from 'vue-router';
import { constantRoute } from './routes';
import { GET_TOKEN } from '@/utils/token';
import useUserStore from '@/stores/modules/users';

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoute,
});

router.beforeEach((to) => {
  if (to.name === 'home') {
    return true;
  }
  if (!GET_TOKEN()) {
    useUserStore().triggerLoginWall();
    return { path: '/', replace: true };
  }
  return true;
});

export default router;
