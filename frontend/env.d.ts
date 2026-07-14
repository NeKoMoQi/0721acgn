/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

interface Window {
  initializeLive2D?: (...args: unknown[]) => void
  hideMessageBox?: () => void
  revealMessageBox?: () => void
  setExpression?: (name?: string) => void
  setRandomExpression?: () => void
  setMessageBox?: (msg: string) => void
}
