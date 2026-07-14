import { ref } from 'vue'
import type { ChessData } from '@/view/WarFlag/types/war'

// 攻击目标信息
export interface AttackTarget {
  chessId: string
  chessname: string
  hp: number
  maxHp: number
  centerLng: number
  centerLat: number
  distance: number // 距离（用于排序）
}

// 攻击结果
export interface AttackResult {
  success: boolean
  damage: number
  targetNewHp: number
  targetDestroyed: boolean
  // 反击信息（目标在自己射程内会反打一次）
  counterAttacked?: boolean
  counterDamage?: number
  attackerNewHp?: number
  attackerDestroyed?: boolean
  message?: string
}

export function useAttack() {
  // 当前可攻击目标列表
  const attackableTargets = ref<AttackTarget[]>([])
  // 当前选中的攻击目标
  const selectedTargetId = ref<string | null>(null)
  // 攻击中状态
  const isAttacking = ref(false)

  /**
   * 获取可攻击目标 - 完全交给后端计算
   */
  async function fetchAttackableTargets(
    chessId: string, 
    battleId: number,
    api: (chessId: string, battleId: number) => Promise<any>
  ): Promise<AttackTarget[]> {
    try {
      const result = await api(chessId, battleId)
      
      const rawTargets = Array.isArray(result.data)
        ? result.data
        : Array.isArray(result.data?.targets)
          ? result.data.targets
          : []

      if (result.code === 200 && rawTargets.length > 0) {
        attackableTargets.value = rawTargets
        console.log(`✅ 后端返回 ${rawTargets.length} 个可攻击目标`)
        return rawTargets
      } else if (result.code === 200) {
        attackableTargets.value = []
        console.log('✅ 后端返回 0 个可攻击目标')
        return []
      } else {
        attackableTargets.value = []
        console.warn('⚠️ 获取可攻击目标失败:', result.msg)
        return []
      }
    } catch (error) {
      console.error('❌ 获取可攻击目标异常:', error)
      attackableTargets.value = []
      return []
    }
  }

  /**
   * 执行攻击 - 完全交给后端计算
   */
  async function executeAttack(
    attackerId: string,
    targetId: string,
    battleId: number,
    api: (params: { attackerId: string; targetId: string; battleId: number }) => Promise<any>
  ): Promise<AttackResult | null> {
    if (isAttacking.value) {
      console.warn('攻击进行中，请等待')
      return null
    }

    // 检查目标是否在可攻击列表中
    const target = attackableTargets.value.find(t => t.chessId === targetId)
    if (!target) {
      console.warn('目标不在可攻击范围内')
      return null
    }

    isAttacking.value = true

    try {
      const result = await api({
        attackerId,
        targetId,
        battleId
      })

      if (result.code === 200) {
        const attackResult: AttackResult = {
          success: true,
          damage: result.data.damage,
          targetNewHp: result.data.targetNewHp,
          targetDestroyed: result.data.targetDestroyed,
          counterAttacked: !!result.data.counterAttacked,
          counterDamage: result.data.counterDamage ?? 0,
          attackerNewHp: result.data.attackerNewHp,
          attackerDestroyed: !!result.data.attackerDestroyed,
          message: result.data.message
        }
        console.log('✅ 攻击成功:', attackResult)
        return attackResult
      } else {
        console.error('❌ 攻击失败:', result.msg)
        return {
          success: false,
          damage: 0,
          targetNewHp: target.hp,
          targetDestroyed: false,
          counterAttacked: false,
          counterDamage: 0,
          attackerDestroyed: false,
          message: result.msg
        }
      }
    } catch (error) {
      console.error('❌ 攻击请求异常:', error)
      return {
        success: false,
        damage: 0,
        targetNewHp: target.hp,
        targetDestroyed: false,
        message: '攻击请求失败'
      }
    } finally {
      isAttacking.value = false
    }
  }

  /**
   * 选择攻击目标
   */
  function selectTarget(targetId: string | null) {
    selectedTargetId.value = targetId
  }

  /**
   * 检查目标是否可攻击
   */
  function isTargetAttackable(targetId: string): boolean {
    return attackableTargets.value.some(t => t.chessId === targetId)
  }

  /**
   * 获取目标信息
   */
  function getTargetInfo(targetId: string): AttackTarget | undefined {
    return attackableTargets.value.find(t => t.chessId === targetId)
  }

  /**
   * 清除攻击状态
   */
  function clearAttackState() {
    attackableTargets.value = []
    selectedTargetId.value = null
    isAttacking.value = false
  }

  return {
    // 状态
    attackableTargets,
    selectedTargetId,
    isAttacking,
    
    // 方法
    fetchAttackableTargets,
    executeAttack,
    selectTarget,
    isTargetAttackable,
    getTargetInfo,
    clearAttackState
  }
}