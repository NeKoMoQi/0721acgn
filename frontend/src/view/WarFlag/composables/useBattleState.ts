import { ref, computed } from 'vue'
import { getBattleState, endBattleTurn } from '@/api/warflag/warChessing'

export interface BattleState {
  battleId: number
  currentPlayer: number      // 0 = 苏军阵营回合, 1 = 德军阵营回合（哪一方是「我」由 myPlayerId 决定）
  roundNo: number
  status: 'ONGOING' | 'WIN_PLAYER0' | 'WIN_PLAYER1' | 'DRAW'
  winner?: number | null
}

export const PLAYER_SELF = 0
export const PLAYER_ENEMY = 1

export function useBattleState(battleId: number, myPlayerId: number) {
  const state = ref<BattleState | null>(null)
  const loading = ref(false)

  const isMyTurn = computed(
    () => Number(state.value?.currentPlayer) === Number(myPlayerId)
  )
  const isOver = computed(() => state.value?.status && state.value.status !== 'ONGOING')
  const winnerLabel = computed(() => {
    const s = state.value?.status
    const w = state.value?.winner
    if (!s || s === 'ONGOING') return ''
    if (s === 'DRAW') return '平局'
    if (Number(w) === Number(myPlayerId)) return '我方胜利'
    return '敌方胜利'
  })

  async function refresh() {
    try {
      const res = await getBattleState(battleId)
      if (res.code === 200) {
        state.value = res.data
      }
    } catch (e) {
      console.error('❌ 拉取回合状态失败', e)
    }
  }

  async function endTurn() {
    if (loading.value) return
    loading.value = true
    try {
      const res = await endBattleTurn(battleId)
      if (res.code === 200) {
        state.value = res.data
      } else {
        console.warn('结束回合失败', res.msg)
      }
    } catch (e) {
      console.error('❌ 结束回合异常', e)
    } finally {
      loading.value = false
    }
  }

  return {
    state,
    isMyTurn,
    isOver,
    winnerLabel,
    loading,
    refresh,
    endTurn
  }
}
