<template>
  <router-view :key="route.fullPath"></router-view>
  <canvas v-if="showGlobalLive2D" id="live2d" class="live2d-canvas"></canvas>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const live2dInitialized = ref(false);

const showGlobalLive2D = computed(() => {
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
  if (live2dInitialized.value) return;
  await nextTick();
  if (!document.getElementById('live2d')) return;

  const live2dWindow = window as typeof window & {
    initializeLive2D?: (config: Record<string, unknown>) => Promise<void>;
    hideMessageBox?: unknown;
    revealMessageBox?: unknown;
    setExpression?: unknown;
    setRandomExpression?: unknown;
    setMessageBox?: unknown;
  };

  if (!live2dWindow.initializeLive2D) {
    console.error('Live2D SDK is not loaded. Check index.html script imports.');
    return;
  }

  try {
    await live2dWindow.initializeLive2D({
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
    });

    live2dInitialized.value = true;
    console.log('Global Live2D initialized.', {
      hideMessageBox: live2dWindow.hideMessageBox,
      revealMessageBox: live2dWindow.revealMessageBox,
      setExpression: live2dWindow.setExpression,
      setRandomExpression: live2dWindow.setRandomExpression,
      setMessageBox: live2dWindow.setMessageBox,
    });
  } catch (error) {
    console.error('Global Live2D initialization failed:', error);
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
