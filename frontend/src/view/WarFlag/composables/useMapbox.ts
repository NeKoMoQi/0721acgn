import { ref, Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import type { FilterSpecification } from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import MapboxLanguage from '@mapbox/mapbox-gl-language'
import { buildGeoJSON } from '@/view/WarFlag/utils/geojson'
import { getWarChessing, getWarChessingRange, moveChess, resetBattle as resetBattleApi, runEnemyAiTurn } from '@/api/warflag/warChessing'
import { useAttack } from './useAttack'
import { useChessPiece } from './useChessPiece'
import { useBattleState } from './useBattleState'
import {
  isChessOnMap,
  type GridItem,
  type ChessData,
  type AiAction,
  type AiTurnResultData
} from '@/view/WarFlag/types/war'

// 引入攻击API
import { getWarChessingAttackRange, attackChess } from '@/api/warflag/warChessing'

mapboxgl.accessToken = 'pk.eyJ1IjoiNHd4bm51dXYiLCJhIjoiY204N25oaGJ3MDk0NzJpcXU1YmtvcGM5OCJ9.05C4KGMpkW1QolRvxwSt_g'

export type SelectedInfo = 
  | { type: 'grid'; data: GridItem }
  | { type: 'chess'; data: ChessData }
  | null

const HEX_DIRECTIONS = [
  { q: 1, r: 0 },
  { q: 1, r: -1 },
  { q: 0, r: -1 },
  { q: -1, r: 0 },
  { q: -1, r: 1 },
  { q: 0, r: 1 },
]

export function useMapbox(
  containerRef: Ref<HTMLDivElement | null>,
  options: {
    center: [number, number]
    zoom: number
    minZoom: number
    maxZoom: number
    maxBounds: [[number, number], [number, number]]
    battleId: number
    /** 玩家所扮演的阵营：0=苏军，1=德军（与数据库 player 一致） */
    myPlayerId: number
  }
) {
  const map = ref<mapboxgl.Map | null>(null)
  const selectedGridId = ref<string | null>(null)
  const selectedChessId = ref<string | null>(null)
  const selectedInfo = ref<SelectedInfo>(null)
  const showInfoButton = ref(false)
  const showDetailModal = ref(false)
  
  const currentMoveRange = ref<Set<string>>(new Set())
  const currentSelectedChess = ref<ChessData | null>(null)
  
  let allGrids: GridItem[] = []
  let chessPieceCtrl: ReturnType<typeof useChessPiece> | null = null
  
  const { 
    attackableTargets, 
    fetchAttackableTargets, 
    executeAttack, 
    isTargetAttackable,
    clearAttackState 
  } = useAttack()

  // 回合状态
  const battleStateCtrl = useBattleState(options.battleId, options.myPlayerId)

  function calculateMoveRange(chessData: ChessData, range: number = 2): Set<string> {
    const rangeSet = new Set<string>()
    
    let currentGrid: GridItem | null = null
    
    if (chessData.hexId) {
      currentGrid = allGrids.find(g => g.id === String(chessData.hexId)) || null
    }
    
    if (!currentGrid) {
      let minDist = Infinity
      
      for (const grid of allGrids) {
        const centerLng = grid.center?.[0] ?? grid.centerLng
        const centerLat = grid.center?.[1] ?? grid.centerLat
        
        if (centerLng == null || centerLat == null) continue
        
        const dx = centerLng - chessData.centerLng
        const dy = centerLat - chessData.centerLat
        const dist = Math.sqrt(dx * dx + dy * dy)
        
        if (dist < minDist) {
          minDist = dist
          currentGrid = grid
        }
      }
    }
    
    if (!currentGrid) {
      console.warn('未找到棋子所在格子', chessData)
      return rangeSet
    }

    const queue = [{ q: currentGrid.q, r: currentGrid.r, dist: 0 }]
    const visited = new Set<string>([`${currentGrid.q},${currentGrid.r}`])
    
    while (queue.length > 0) {
      const current = queue.shift()!
      
      const grid = allGrids.find(g => g.q === current.q && g.r === current.r)
      if (grid) {
        rangeSet.add(grid.id)
      }
      
      if (current.dist < range) {
        for (const dir of HEX_DIRECTIONS) {
          const nextQ = current.q + dir.q
          const nextR = current.r + dir.r
          const key = `${nextQ},${nextR}`
          
          if (!visited.has(key)) {
            visited.add(key)
            queue.push({ q: nextQ, r: nextR, dist: current.dist + 1 })
          }
        }
      }
    }
    return rangeSet
  }

  async function fetchMoveRangeFromBackend(chessId: string, battleId: number) {
    try {
      const result = await getWarChessingRange(chessId, battleId)
      
      if (result.code === 200 && result.data?.rangeGridIds) {
        currentMoveRange.value = new Set(result.data.rangeGridIds)
        const rangeArray = Array.from(currentMoveRange.value)
        console.log(`✅ 后端返回 ${result.data.gridCount} 个可移动格子，计算耗时 ${result.data.costTimeMs}ms`)
        
        if (rangeArray.length > 0 && map.value) {
          const filterExpr: any = ['match', ['get', 'id'], rangeArray, true, false]
          map.value.setFilter('hex-move-range', filterExpr)
          map.value.setFilter('hex-move-range-line', filterExpr)
        } else {
          hideMoveRange()
        }
      } else {
        console.warn('⚠️ 后端返回异常:', result.msg)
        hideMoveRange()
      }
    } catch (error) {
      console.error('❌ 获取行动范围失败:', error)
      hideMoveRange()
    }
  }

  function hideMoveRange() {
    const noneFilter = false as unknown as FilterSpecification
    map.value?.setFilter('hex-move-range', noneFilter)
    map.value?.setFilter('hex-move-range-line', noneFilter)
    currentMoveRange.value.clear()
  }

  /** 是否是当前回合方的棋子（决定能不能弹操作菜单） */
  function canControl(chessData: ChessData): boolean {
    const state = battleStateCtrl.state.value
    if (!state) return false
    if (state.status !== 'ONGOING') return false
    if (Number(state.currentPlayer) !== options.myPlayerId) return false
    return Number(chessData.player) === options.myPlayerId
  }

  /** 给当前选中的"我方"棋子刷新可攻击目标（用于初次选中、以及移动后重算） */
  async function refreshAttackTargets(chess: ChessData) {
    if (chess.fired === true) {
      chessPieceCtrl?.clearAttackMarks()
      clearAttackState()
      return
    }
    const targets = await fetchAttackableTargets(
      String(chess.id),
      chess.battleId,
      getWarChessingAttackRange
    )
    chessPieceCtrl?.markAttackableTargets(targets.map(t => t.chessId))
  }

  /**
   * 选中棋子：
   *  - 我方且当前回合：同时显示移动范围 + 可攻击目标（已动/已开火则各自跳过）
   *  - 敌方 / 非当前回合：仅展示信息按钮
   */
  async function selectChess(chessData: ChessData) {
    clearGridHighlight()
    hideMoveRange()
    chessPieceCtrl?.clearAttackMarks()
    clearAttackState()

    selectedChessId.value = String(chessData.id)
    currentSelectedChess.value = chessData
    selectedInfo.value = { type: 'chess', data: chessData }
    showInfoButton.value = true

    if (!canControl(chessData)) {
      return
    }

    // 我方回合：把移动范围 + 攻击目标并行拉出来
    const tasks: Promise<any>[] = []

    if (chessData.moved !== true) {
      tasks.push(
        fetchMoveRangeFromBackend(String(chessData.id), chessData.battleId)
      )
    }

    tasks.push(refreshAttackTargets(chessData))

    await Promise.all(tasks)
  }

  /** 反击动画：目标 → 攻击者 */
  function playCounterAttack(
    attackerChess: ChessData,
    targetChess: ChessData,
    result: NonNullable<Awaited<ReturnType<typeof executeAttack>>>
  ) {
    const targetRange = Number(targetChess.range ?? 1)
    const attackerId = String(attackerChess.id)

    chessPieceCtrl?.playAttackAnimation(
      String(targetChess.id),
      attackerId,
      targetRange,
      () => {
        chessPieceCtrl?.playHitAnimation(attackerId, () => {
          if (result.attackerNewHp != null) {
            chessPieceCtrl?.updateHp(attackerId, result.attackerNewHp, attackerChess.maxHp)
            attackerChess.hp = result.attackerNewHp
          }
          if (result.attackerDestroyed) {
            setTimeout(() => {
              chessPieceCtrl?.destroyPiece(attackerId)
              if (currentSelectedChess.value && String(currentSelectedChess.value.id) === attackerId) {
                clearChessSelection()
                selectedInfo.value = null
                showInfoButton.value = false
              }
            }, 500)
          }
        })
      }
    )
  }

  async function handleAttackClick(targetChess: ChessData) {
    if (!selectedChessId.value || !currentSelectedChess.value) return
    if (!isTargetAttackable(String(targetChess.id))) {
      console.warn('目标不可攻击')
      return
    }

    const attackerChess = currentSelectedChess.value
    const result = await executeAttack(
      selectedChessId.value,
      String(targetChess.id),
      attackerChess.battleId,
      attackChess
    )

    if (result?.success) {
      const attackerRange = Number(attackerChess.range ?? 1)
      const attackerId = String(attackerChess.id)
      const targetId = String(targetChess.id)

      chessPieceCtrl?.playAttackAnimation(
        attackerId,
        targetId,
        attackerRange,
        () => {
          chessPieceCtrl?.playHitAnimation(targetId, () => {
            chessPieceCtrl?.updateHp(targetId, result.targetNewHp, targetChess.maxHp)
            targetChess.hp = result.targetNewHp

            if (result.targetDestroyed) {
              setTimeout(() => {
                chessPieceCtrl?.destroyPiece(targetId)
              }, 500)
              return
            }

            // 反击：目标存活 + 后端判定可反击
            if (result.counterAttacked) {
              setTimeout(() => playCounterAttack(attackerChess, targetChess, result), 350)
            }
          })
        }
      )

      attackerChess.fired = true

      chessPieceCtrl?.clearAttackMarks()
      clearAttackState()

      // 刷新回合状态（探测胜负）
      await battleStateCtrl.refresh()
    } else if (result && !result.success) {
      console.warn('攻击失败:', result.message)
    }
  }

  async function handleMoveRangeClick(targetGrid: GridItem) {
    if (!currentSelectedChess.value || !selectedChessId.value) {
      console.warn('没有选中的棋子')
      return
    }
    if (currentSelectedChess.value.moved === true) {
      console.warn('该棋子本回合已移动')
      return
    }

    const chessId = selectedChessId.value
    const battleId = currentSelectedChess.value.battleId

    try {
      const result = await moveChess({
        chessId: chessId,
        battleId: battleId,
        targetGridId: targetGrid.id,
      })

      if (result.code === 200) {
        targetGrid.centerLat = result.data.targetLat
        targetGrid.centerLng = result.data.targetLng

        chessPieceCtrl?.moveTo(chessId, [targetGrid.centerLng, targetGrid.centerLat])

        currentSelectedChess.value.centerLng = targetGrid.centerLng
        currentSelectedChess.value.centerLat = targetGrid.centerLat
        currentSelectedChess.value.hexId = targetGrid.id
        currentSelectedChess.value.moved = true

        hideMoveRange()

        // 移动后位置变了，重新拉一次可攻击目标
        await refreshAttackTargets(currentSelectedChess.value)
        await battleStateCtrl.refresh()
      } else {
        console.error('❌ 后端更新失败:', result.msg)
      }
    } catch (error) {
      console.error('❌ 移动棋子失败:', error)
    }
  }

  function clearGridHighlight() {
    selectedGridId.value = null
    map.value?.setFilter('hex-highlight', ['==', ['get', 'id'], ''])
  }

  function clearChessSelection() {
    selectedChessId.value = null
    currentSelectedChess.value = null
    chessPieceCtrl?.clear()
    hideMoveRange()
    clearAttackState()
  }

  function selectGrid(gridItem: GridItem) {
    clearChessSelection()
    selectedGridId.value = gridItem.id
    selectedInfo.value = { type: 'grid', data: gridItem }
    showInfoButton.value = true
    showDetailModal.value = false
    map.value?.setFilter('hex-highlight', ['==', ['get', 'id'], gridItem.id])
  }

  function clearAll() {
    clearGridHighlight()
    clearChessSelection()
    selectedInfo.value = null
    showInfoButton.value = false
    showDetailModal.value = false
  }

  /** 棋子创建/重建时统一的事件回调（复用给 init 与 reloadChessPieces） */
  const chessCreateOptions = {
    onSelect: (data: ChessData) => selectChess(data),
    onAttackClick: (data: ChessData) => handleAttackClick(data)
  }

  /**
   * 从后端重新拉取棋子列表，并全量重建地图上的棋子。
   * 用于结束回合后同步最新的 moved / fired / hp / 位置等状态。
   */
  async function reloadChessPieces() {
    if (!chessPieceCtrl) return
    try {
      const chessRes = await getWarChessing(options.battleId)
      if (chessRes?.code !== 200) {
        console.warn('刷新棋子失败:', chessRes?.msg)
        return
      }
      chessPieceCtrl.reloadAll(chessRes.data as ChessData[], chessCreateOptions)
    } catch (e) {
      console.error('❌ 刷新棋子异常', e)
    }
  }

  const AI_ACTION_GAP_MS = 420
  const AI_MOVE_DURATION_MS = 720

  function delay(ms: number) {
    return new Promise<void>((r) => setTimeout(r, ms))
  }

  /**
   * 按后端 AiAction 顺序播放敌方移动 / 开火（与玩家操作同款动画）。
   * preAiBoard：调用 runEnemyAiTurn 之前的一帧棋盘快照，用于取 range / maxHp。
   */
  async function replayAiActions(actions: AiAction[], preAiBoard: ChessData[]) {
    if (!chessPieceCtrl || !actions.length) return

    const sim = new Map<string, ChessData>(
      preAiBoard.map((c) => [String(c.id), { ...c }])
    )

    for (const a of actions) {
      await delay(AI_ACTION_GAP_MS)

      if (a.type === 'MOVE') {
        const id = String(a.chessId)
        if (!chessPieceCtrl || !sim.has(id)) continue
        const lng = a.toLng
        const lat = a.toLat
        if (lng == null || lat == null) continue

        await new Promise<void>((resolve) => {
          chessPieceCtrl!.moveTo(id, [lng, lat], AI_MOVE_DURATION_MS, resolve)
        })

        const u = sim.get(id)
        if (u) {
          if (a.toQ != null) u.q = a.toQ
          if (a.toR != null) u.r = a.toR
          u.centerLng = lng
          u.centerLat = lat
        }
        continue
      }

      if (a.type === 'ATTACK') {
        const attackerId = String(a.chessId)
        const targetId = a.targetId != null ? String(a.targetId) : ''
        if (!targetId || !chessPieceCtrl) continue

        const attacker = sim.get(attackerId)
        const range = Number(attacker?.range ?? 1)

        await new Promise<void>((resolve) => {
          chessPieceCtrl!.playAttackAnimation(attackerId, targetId, range, () => {
            chessPieceCtrl!.playHitAnimation(targetId, () => {
              const tgt = sim.get(targetId)
              const maxHp = Number(tgt?.maxHp ?? 100)
              const newHp = Number(a.targetNewHp ?? 0)
              chessPieceCtrl!.updateHp(targetId, newHp, maxHp)

              if (a.targetDestroyed) {
                chessPieceCtrl!.destroyPiece(targetId)
              }

              if (tgt) tgt.hp = newHp

              const finishTurn = () => {
                if (attacker && a.attackerNewHp != null) {
                  attacker.hp = Number(a.attackerNewHp)
                }
                const waitDestroy =
                  a.targetDestroyed || a.attackerDestroyed ? 560 : 220
                window.setTimeout(resolve, waitDestroy)
              }

              if (a.counterAttacked && a.attackerNewHp != null && attacker) {
                chessPieceCtrl!.playHitAnimation(attackerId, () => {
                  chessPieceCtrl!.updateHp(
                    attackerId,
                    Number(a.attackerNewHp),
                    Number(attacker.maxHp ?? 100)
                  )
                  if (a.attackerDestroyed) {
                    chessPieceCtrl!.destroyPiece(attackerId)
                  }
                  finishTurn()
                })
              } else {
                finishTurn()
              }
            })
          })
        })
      }
    }
  }

  /**
   * 当前若是「非人类」的回合（例如选德军时开局先手是苏军），自动请求 AI 并播放动画。
   * 也会在玩家结束回合后、轮到敌方时调用。
   */
  async function runEnemyAiSequence() {
    const st = battleStateCtrl.state.value
    if (!st || st.status !== 'ONGOING') return
    if (Number(st.currentPlayer) === Number(options.myPlayerId)) return

    try {
      let preAiBoard: ChessData[] | null = null
      const snapRes = await getWarChessing(options.battleId)
      if (snapRes?.code === 200 && Array.isArray(snapRes.data)) {
        preAiBoard = (snapRes.data as ChessData[]).map((c) => ({ ...c }))
      }

      const aiRes = await runEnemyAiTurn(options.battleId, options.myPlayerId)
      const payload = aiRes?.data as AiTurnResultData | undefined

      if (
        aiRes?.code === 200 &&
        payload?.executed &&
        preAiBoard?.length &&
        payload.actions?.length
      ) {
        await replayAiActions(payload.actions, preAiBoard)
      }
    } catch (e) {
      console.error('❌ 敌方 AI 回合异常', e)
    }
  }

  /** 结束回合：调用后端，若进入敌方回合则跑 AI 并顺序播放动画，最后再对齐服务器状态 */
  async function endTurn() {
    clearAll()
    await battleStateCtrl.endTurn()
    await runEnemyAiSequence()
    await battleStateCtrl.refresh()
    await reloadChessPieces()
  }

  /**
   * 一键重置战局：
   *   1. 调后端 /battle/reset，从 tb_chessing_backup / tb_battle_state_backup 还原
   *   2. 重新拉回合状态（恢复到第 1 回合 / 我方先手 / ONGOING）
   *   3. 全量重建地图棋子（位置、HP、moved/fired 全部回到开局状态）
   */
  async function resetBattle() {
    try {
      const res = await resetBattleApi(options.battleId, options.myPlayerId)
      if (res?.code !== 200) {
        console.warn('重置战局失败:', res?.msg)
        return false
      }

      clearAll()
      await battleStateCtrl.refresh()
      await reloadChessPieces()
      await runEnemyAiSequence()
      await battleStateCtrl.refresh()
      await reloadChessPieces()
      return true
    } catch (e) {
      console.error('❌ 重置战局异常', e)
      return false
    }
  }

  function openDetailModal() {
    if (selectedInfo.value) {
      showDetailModal.value = true
    }
  }

  function closeDetailModal() {
    showDetailModal.value = false
  }

  async function init(gridData: GridItem[], chessData: ChessData[]) {
    if (!containerRef.value) return

    allGrids = gridData

    // 初始化拉一次回合状态
    await battleStateCtrl.refresh()

    const { battleId, ...mapOptions } = options
    map.value = new mapboxgl.Map({
      container: containerRef.value,
      style: 'mapbox://styles/mapbox/streets-v11',
      ...mapOptions,
      attributionControl: false,
      pitch: 0,
      bearing: 0,
      antialias: false,
      renderWorldCopies: false,
      skipOnboarding: true
    } as mapboxgl.MapboxOptions)

    map.value.addControl(new MapboxLanguage({ defaultLanguage: 'zh-Hans' }))

    await new Promise(resolve => map.value!.on('load', resolve))

    setChinaWorldview()
    loadGrid(gridData)

    chessPieceCtrl = useChessPiece(map.value as any)

    chessData.forEach((piece) => {
      if (!isChessOnMap(piece)) return
      chessPieceCtrl!.create(piece, chessCreateOptions)
    })

    await runEnemyAiSequence()
    await battleStateCtrl.refresh()
    await reloadChessPieces()

    map.value.on('click', (e) => {
      const gridFeatures = map.value?.queryRenderedFeatures(e.point, {
        layers: ['hex-fill']
      })

      const clickedChess = (e.originalEvent.target as HTMLElement | null)?.closest('.chess-piece') !== null

      if ((!gridFeatures || gridFeatures.length === 0) && !clickedChess) {
        clearAll()
      }
    })
  }

  function setChinaWorldview() {
    const layers = ['admin-0-boundary', 'admin-1-boundary', 'place-label', 'road-label']
    layers.forEach(layerId => {
      if (map.value?.getLayer(layerId)) {
        map.value.setFilter(layerId, [
          'all',
          ['==', ['get', 'admin_level'], 0],
          ['==', ['get', 'disputed'], 'false'],
          ['==', ['get', 'maritime'], 'false'],
          ['match', ['get', 'worldview'], ['all', 'CN'], true, false]
        ])
      }
    })

    map.value?.setFilter('admin-0-boundary-disputed', [
      'all',
      ['==', ['get', 'disputed'], 'true'],
      ['match', ['get', 'worldview'], ['all', 'CN'], true, false]
    ])
  }

  function loadGrid(gridData: GridItem[]) {
    if (!map.value) return

    const geojson = buildGeoJSON(gridData)

    map.value.addSource('hex-grid', {
      type: 'geojson',
      data: geojson
    })

    map.value.addLayer({
      id: 'hex-fill',
      type: 'fill',
      source: 'hex-grid',
      paint: {
        'fill-color': [
          'interpolate',
          ['linear'],
          ['get', 'elevation'],
          0, '#51bbd6',
          100, '#f1f075',
          300, '#f28cb1'
        ],
        'fill-opacity': 0.3,
        'fill-outline-color': '#000'
      }
    })

    map.value.addLayer({
      id: 'hex-line',
      type: 'line',
      source: 'hex-grid',
      paint: {
        'line-color': 'black',
        'line-width': 1
      }
    })

    map.value.addLayer({
      id: 'hex-move-range',
      type: 'fill',
      source: 'hex-grid',
      paint: {
        'fill-color': '#ffffff',
        'fill-opacity': 0.25,
        'fill-outline-color': '#ffffff'
      },
      filter: false as unknown as FilterSpecification
    })

    map.value.addLayer({
      id: 'hex-move-range-line',
      type: 'line',
      source: 'hex-grid',
      paint: {
        'line-color': '#ffffff',
        'line-width': 4,
        'line-opacity': 0.9,
        'line-blur': 0
      },
      filter: false as unknown as FilterSpecification
    })

    map.value.addLayer({
      id: 'hex-highlight',
      type: 'fill',
      source: 'hex-grid',
      paint: {
        'fill-color': '#ff4d4f',
        'fill-opacity': 0.65
      },
      filter: ['==', ['get', 'id'], '']
    })

    map.value.on('click', 'hex-fill', (e) => {
      const feature = e.features?.[0]
      if (!feature) return
      
      const prop = feature.properties
      const gridItem = gridData.find(g => g.id === prop.id)
      if (!gridItem) return

      if (selectedChessId.value && currentMoveRange.value.has(gridItem.id)) {
        handleMoveRangeClick(gridItem)
        return
      }

      selectGrid(gridItem)
    })
  }

  return {
    map,
    selectedGridId,
    selectedChessId,
    selectedInfo,
    showInfoButton,
    showDetailModal,
    init,
    clearAll,
    openDetailModal,
    closeDetailModal,

    // 回合制
    battleState: battleStateCtrl.state,
    isMyTurn: battleStateCtrl.isMyTurn,
    isOver: battleStateCtrl.isOver,
    winnerLabel: battleStateCtrl.winnerLabel,
    endTurn,
    resetBattle
  }
}
