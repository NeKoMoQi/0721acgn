<template>
  <router-view :key="route.fullPath"></router-view>
  <LoginDialog />
  <canvas v-if="showGlobalLive2D" id="live2d" class="live2d-canvas"></canvas>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import LoginDialog from '@/components/LoginDialog.vue';
import { loadLive2DScript } from '@/utils/live2d-loader';

const route = useRoute();
const live2dInitialized = ref(false);
/** SDK 或模型加载失败时不再显示空画布，也不再往控制台抛错。 */
const live2dAvailable = ref(true);

const showGlobalLive2D = computed(() => {
  if (!live2dAvailable.value) return false;
  return !route.path.startsWith('/warflag') && !route.path.startsWith('/shengdi');
});

function cleanupGlobalLive2D({ includeCanvas = false } = {}) {
  live2dInitialized.value = false;
  [
    'live2dMessageBox',
    'live2d-toolbox',
    'live2dChatPanel',
    'chat-history-panel',
  ].forEach((id) => document.getElementById(id)?.remove());

  if (includeCanvas) {
    document.getElementById('live2d')?.remove();
  }
}

async function mountGlobalLive2D() {
  if (live2dInitialized.value || !live2dAvailable.value) return;
  await nextTick();
  if (!document.getElementById('live2d')) return;

  try {
    const live2d = await loadLive2DScript();
    await live2d.initializeLive2D({
      CanvasId: 'live2d',
      BackgroundRGBA: [0.0, 0.0, 0.0, 0.0],
      ResourcesPath: './models/cat/sdwhite cat free.model3.json',
      CanvasSize: {
        height: 300,
        width: 240,
      },
      ShowToolBox: true,
      CanvasPosition: 'right',
      LoadFromCache: true,
      Live2dCubismcoreUrl: '/live2dcubismcore.min.js',
    });

    live2dInitialized.value = true;
  } catch (error) {
    live2dAvailable.value = false;
    console.warn('看板娘没能加载，已隐藏画布：', error);
  }
}

watch(
  showGlobalLive2D,
  async (visible) => {
    if (visible) {
      mountGlobalLive2D();
    } else {
      await nextTick();
      cleanupGlobalLive2D({ includeCanvas: route.path.startsWith('/warflag') });
    }
  },
  { immediate: true },
);
</script>

<style lang="scss">
html {
  font-size: clamp(12px, 1vw, 16px);
}

#live2d {
  position: fixed;
  right: 0;
  bottom: 0;
  z-index: 1200;
  width: 240px;
  height: 300px;
  pointer-events: none;
}

#live2dMessageBox-content {
  background-color: #ff95bc;
  color: white;
  font-family: var(--base-font);
  padding: 10px;
  height: fit-content;
  border-radius: 0.7em;
  word-break: break-all;
}

.live2dMessageBox-content-hidden {
  opacity: 0;
  transform: scaleY(0.2);
  transition: all 0.35s ease-in;
}

.live2dMessageBox-content-visible {
  opacity: 1;
  transition: all 0.35s ease-out;
}
</style>
