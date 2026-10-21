const LIVE2D_SCRIPT_ID = 'live2d-render-bundle-script'
const LIVE2D_SCRIPT_URL = '/live2d-render.bundle.js?v=202605210001'

export interface Live2DRenderApi {
  initializeLive2D: (config: Record<string, unknown>) => Promise<void>
  setMessageBox?: (message: string, duration?: number) => void
  hideMessageBox?: () => void
  revealMessageBox?: () => void
}

// 动�?�加�?UMD 模块的辅助函�
export function loadLive2DScript(): Promise<Live2DRenderApi> {
  return new Promise((resolve, reject) => {
    // 如果已经加载，直接返�
if (window.Live2dRender) {
      resolve(window.Live2dRender);
      return;
    }

    const existingScript = document.getElementById(LIVE2D_SCRIPT_ID) as HTMLScriptElement | null
    if (existingScript) {
      existingScript.addEventListener('load', () => {
        window.Live2dRender ? resolve(window.Live2dRender) : reject(new Error('Live2dRender 未挂载到 window'))
      }, { once: true })
      existingScript.addEventListener('error', () => reject(new Error('加载 live2d-render.bundle.js 失败')), { once: true })
      return
    }

    const script = document.createElement('script');
    script.id = LIVE2D_SCRIPT_ID;
    script.src = LIVE2D_SCRIPT_URL;
    script.type = 'text/javascript';
    
    // 关键：添�?crossorigin 和正确处理加�
script.onload = () => {
      if (window.Live2dRender) {
        resolve(window.Live2dRender);
      } else {
        reject(new Error('Live2dRender 未挂载到 window'));
      }
    };
    
    script.onerror = () => reject(new Error('加载 live2d-render.bundle.js 失败'));
    
    document.head.appendChild(script);
  });
}

export async function mountShengdiLive2D(): Promise<void> {
  cleanupLive2D()

  const live2d = await loadLive2DScript()
  await live2d.initializeLive2D({
    ResourcesPath: '/models/white/白毛宝宝.model3.json',
    CanvasSize: {
      width: 300,
      height: 460,
    },
    CanvasPosition: 'right',
    BackgroundRGBA: [0, 0, 0, 0],
    ShowToolBox: false,
    LoadFromCache: true,
    Live2dCubismcoreUrl: '/live2dcubismcore.min.js',
    AgentChatUrl: '/acgn/live2d/agent/chat',
  })
}

export function cleanupLive2D(): void {
  const ids = [
    'live2d',
    'live2dMessageBox',
    'live2d-toolbox',
    'live2dChatPanel',
    'chat-history-panel',
  ]

  ids.forEach((id) => {
    document.getElementById(id)?.remove()
  })

  document.querySelectorAll('.live2d-shengdi-cleanup').forEach((node) => node.remove())
}

// 类型声明
declare global {
  interface Window {
    Live2dRender?: Live2DRenderApi;
  }
}
