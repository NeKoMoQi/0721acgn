import { ref } from 'vue'
import type { ChessData } from '@/view/WarFlag/types/war'

export type ActionMode = 'idle' | 'move' | 'attack'

export function useActionMenu() {
  // 浮动菜单是否显示
  const visible = ref(false)
  // 屏幕坐标（px），用于定位浮动菜单
  const screenX = ref(0)
  const screenY = ref(0)
  // 当前选中棋子
  const target = ref<ChessData | null>(null)
  // 当前操作模式：菜单关闭后用户选择走哪个分支
  const mode = ref<ActionMode>('idle')

  function open(chess: ChessData, point: { x: number; y: number }) {
    target.value = chess
    screenX.value = point.x
    screenY.value = point.y
    mode.value = 'idle'
    visible.value = true
  }

  function close() {
    visible.value = false
    target.value = null
    mode.value = 'idle'
  }

  function setMode(m: ActionMode) {
    mode.value = m
    visible.value = false
  }

  return {
    visible,
    screenX,
    screenY,
    target,
    mode,
    open,
    close,
    setMode
  }
}
