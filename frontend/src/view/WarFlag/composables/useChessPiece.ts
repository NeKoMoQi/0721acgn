import { ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import { isChessOnMap, type ChessData, type ChessElements } from '@/view/WarFlag/types/war'

const TARGET_GRID_RADIUS_KM = 5
const PIECE_SIZE_RATIO = 0.6
const ATTACK_MARK_STYLE_ID = 'war-attack-mark-style'

function ensureAttackMarkStyles() {
  if (typeof document === 'undefined' || document.getElementById(ATTACK_MARK_STYLE_ID)) return

  const style = document.createElement('style')
  style.id = ATTACK_MARK_STYLE_ID
  style.textContent = `
    .chess-attack-mark {
      filter: drop-shadow(0 4px 10px rgba(255, 77, 79, 0.4));
      animation: war-attack-breathe 1.2s ease-in-out infinite;
    }

    .chess-attack-arrow {
      position: absolute;
      width: 16px;
      height: 16px;
      opacity: 0.42;
      animation: war-attack-arrow-flash 1.2s ease-in-out infinite;
    }

    .chess-attack-arrow::after {
      content: '';
      position: absolute;
      inset: 0;
      background: #ff4d4f;
      clip-path: polygon(50% 100%, 0 0, 100% 0);
    }

    .chess-attack-arrow::before {
      content: '';
      position: absolute;
      left: 50%;
      top: -6px;
      transform: translateX(-50%);
      width: 4px;
      height: 8px;
      border-radius: 999px;
      background: #ff4d4f;
    }

    .chess-attack-arrow.arrow-1 { left: 50%; top: -2px; transform: translateX(-50%) rotate(0deg); animation-delay: 0s; }
    .chess-attack-arrow.arrow-2 { right: 12%; top: 12%; transform: rotate(45deg); animation-delay: 0.12s; }
    .chess-attack-arrow.arrow-3 { right: -2px; top: 50%; transform: translateY(-50%) rotate(90deg); animation-delay: 0.24s; }
    .chess-attack-arrow.arrow-4 { right: 12%; bottom: 12%; transform: rotate(135deg); animation-delay: 0.36s; }
    .chess-attack-arrow.arrow-5 { left: 50%; bottom: -2px; transform: translateX(-50%) rotate(180deg); animation-delay: 0.48s; }
    .chess-attack-arrow.arrow-6 { left: 12%; bottom: 12%; transform: rotate(225deg); animation-delay: 0.6s; }
    .chess-attack-arrow.arrow-7 { left: -2px; top: 50%; transform: translateY(-50%) rotate(270deg); animation-delay: 0.72s; }
    .chess-attack-arrow.arrow-8 { left: 12%; top: 12%; transform: rotate(315deg); animation-delay: 0.84s; }

    @keyframes war-attack-breathe {
      0%, 100% {
        transform: translate(-50%, -50%) scale(0.96);
        opacity: 0.78;
      }
      50% {
        transform: translate(-50%, -50%) scale(1.08);
        opacity: 1;
      }
    }

    @keyframes war-attack-arrow-flash {
      0%, 100% {
        opacity: 0.38;
      }
      50% {
        opacity: 1;
      }
    }

    /* ===== 远程开炮特效 ===== */
    .chess-muzzle-flash {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%) scale(0.4);
      width: 70%;
      height: 70%;
      border-radius: 50%;
      background: radial-gradient(circle,
        rgba(255, 255, 220, 1) 0%,
        rgba(255, 200, 80, 0.95) 30%,
        rgba(255, 120, 0, 0.7) 55%,
        rgba(255, 60, 0, 0) 75%);
      pointer-events: none;
      z-index: 50;
      animation: chess-muzzle-flash 0.32s ease-out forwards;
      filter: blur(0.5px);
    }

    @keyframes chess-muzzle-flash {
      0% {
        opacity: 0;
        transform: translate(-50%, -50%) scale(0.3);
      }
      30% {
        opacity: 1;
        transform: translate(-50%, -50%) scale(1.15);
      }
      100% {
        opacity: 0;
        transform: translate(-50%, -50%) scale(1.4);
      }
    }

    .chess-projectile {
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: radial-gradient(circle,
        #fffbe6 0%,
        #ffd54f 35%,
        #ff7043 65%,
        #b71c1c 100%);
      box-shadow:
        0 0 12px 4px rgba(255, 152, 0, 0.85),
        0 0 24px 10px rgba(255, 87, 34, 0.55);
      pointer-events: none;
    }

    .chess-projectile-trail {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      width: 38px;
      height: 38px;
      border-radius: 50%;
      background: radial-gradient(circle,
        rgba(255, 200, 80, 0.55) 0%,
        rgba(255, 120, 0, 0) 70%);
      pointer-events: none;
      animation: chess-projectile-pulse 0.6s ease-in-out infinite;
    }

    @keyframes chess-projectile-pulse {
      0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(0.85); }
      50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
    }

    .chess-explosion {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%) scale(0.2);
      width: 180%;
      height: 180%;
      border-radius: 50%;
      background: radial-gradient(circle,
        rgba(255, 255, 255, 1) 0%,
        rgba(255, 235, 59, 0.95) 18%,
        rgba(255, 152, 0, 0.85) 38%,
        rgba(255, 87, 34, 0.6) 58%,
        rgba(0, 0, 0, 0) 78%);
      pointer-events: none;
      z-index: 60;
      animation: chess-explosion 0.55s ease-out forwards;
      filter: blur(0.5px);
    }

    .chess-explosion-ring {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%) scale(0.2);
      width: 200%;
      height: 200%;
      border-radius: 50%;
      border: 3px solid rgba(255, 220, 120, 0.9);
      box-shadow: 0 0 24px 6px rgba(255, 152, 0, 0.6) inset;
      pointer-events: none;
      z-index: 59;
      animation: chess-explosion-ring 0.55s ease-out forwards;
    }

    @keyframes chess-explosion {
      0%   { opacity: 0; transform: translate(-50%, -50%) scale(0.2); }
      35%  { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
      100% { opacity: 0; transform: translate(-50%, -50%) scale(1.5); }
    }

    @keyframes chess-explosion-ring {
      0%   { opacity: 0.9; transform: translate(-50%, -50%) scale(0.2); }
      100% { opacity: 0;   transform: translate(-50%, -50%) scale(1.8); }
    }

    /* ===== 阵营外框（敌我区分） ===== */
    .chess-piece-frame {
      position: absolute;
      inset: -8%;
      border-radius: 50%;
      border: 3px solid transparent;
      box-sizing: border-box;
      pointer-events: none;
      z-index: 0;
    }
    .chess-piece-frame.faction-soviet {
      border-color: #dc2626;
      box-shadow:
        inset 0 0 0 1px rgba(0, 0, 0, 0.55),
        0 0 10px 2px rgba(220, 38, 38, 0.55);
    }
    .chess-piece-frame.faction-german {
      border-color: #1f2937;
      box-shadow:
        inset 0 0 0 1px rgba(255, 255, 255, 0.5),
        0 0 10px 2px rgba(31, 41, 55, 0.7);
    }

    /* ===== 阵营徽章（右上角） ===== */
    .chess-piece-badge {
      position: absolute;
      top: -10%;
      right: -10%;
      width: 38%;
      height: 38%;
      min-width: 16px;
      min-height: 16px;
      max-width: 30px;
      max-height: 30px;
      border-radius: 50%;
      border: 2px solid #ffffff;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.55);
      display: flex;
      align-items: center;
      justify-content: center;
      pointer-events: none;
      z-index: 11;
      box-sizing: border-box;
    }
    .chess-piece-badge.faction-soviet {
      background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
    }
    .chess-piece-badge.faction-german {
      background: linear-gradient(135deg, #4b5563 0%, #111827 100%);
    }
    .chess-piece-badge svg {
      width: 70%;
      height: 70%;
      display: block;
    }
  `
  document.head.appendChild(style)
}

export function useChessPiece(map: mapboxgl.Map) {
  const pieces = new Map<string, ChessElements>()
  const selectedId = ref<string | null>(null)
  const animationIds = new Map<string, number>()
  
  // 可攻击目标ID集合（仅用于视觉标记）
  const markedAttackTargets = ref<Set<string>>(new Set())

  function getMetersPerPixel(): number {
    const center = map.getCenter()
    const zoom = map.getZoom()
    return (156543.03392 * Math.cos(center.lat * Math.PI / 180)) / Math.pow(2, zoom)
  }

  function calculateSize(): number {
    const metersPerPixel = getMetersPerPixel()
    const targetDiameterMeters = TARGET_GRID_RADIUS_KM * 1000 * 2 * PIECE_SIZE_RATIO
    const pixelSize = targetDiameterMeters / metersPerPixel
    return Math.max(30, Math.min(pixelSize, 120))
  }

  function updateSize(pieceId?: string) {
    const size = calculateSize()
    const targets = pieceId ? [pieceId] : Array.from(pieces.keys())
    
    targets.forEach(id => {
      const piece = pieces.get(id)
      if (!piece) return
      
      const isSelected = selectedId.value === id
      const finalSize = isSelected ? size * 1.5 : size
      
      const offset = finalSize / 2
      piece.shell.style.width = `${finalSize}px`
      piece.shell.style.height = `${finalSize}px`
      piece.shell.style.marginLeft = `${-offset}px`
      piece.shell.style.marginTop = `${-offset}px`
      
      if (piece.hpBox) {
        piece.hpBox.style.bottom = `${-finalSize * 0.15}px`
        piece.hpBox.style.width = `${finalSize * 0.8}px`
      }
      
      // 同步更新攻击标记位置
      if (piece.attackMark) {
        const markSize = Math.max(finalSize * 1.75, 64)
        piece.attackMark.style.width = `${markSize}px`
        piece.attackMark.style.height = `${markSize}px`
      }
    })
  }

  // ========== 攻击标记相关方法 ==========

  /**
   * 标记可攻击目标（仅视觉）
   */
  function markAttackableTargets(targetIds: string[]) {
    // 清除旧标记
    clearAttackMarks()
    
    // 设置新标记
    markedAttackTargets.value = new Set(targetIds)
    
    targetIds.forEach(id => {
      const piece = pieces.get(id)
      if (piece?.attackMark) {
        piece.attackMark.style.display = 'block'
      }
    })
  }

  /**
   * 清除所有攻击标记
   */
  function clearAttackMarks() {
    markedAttackTargets.value.forEach(id => {
      const piece = pieces.get(id)
      if (piece?.attackMark) {
        piece.attackMark.style.display = 'none'
      }
    })
    markedAttackTargets.value.clear()
  }

  /**
   * 检查是否有攻击标记
   */
  function hasAttackMark(pieceId: string): boolean {
    return markedAttackTargets.value.has(pieceId)
  }

  // ========== 动画方法 ==========

  /**
   * 移动动画（完成后可回调，供 AI 回放等顺序播放）
   */
  function moveTo(
    pieceId: string | number,
    targetLngLat: [number, number],
    duration: number = 500,
    onComplete?: () => void
  ) {
    const strId = String(pieceId)
    const piece = pieces.get(strId)
    if (!piece) {
      console.warn('未找到棋子:', strId)
      onComplete?.()
      return
    }

    const startLngLat = piece.marker.getLngLat()
    const startLng = startLngLat.lng
    const startLat = startLngLat.lat
    const [targetLng, targetLat] = targetLngLat

    const startTime = performance.now()

    function animate(currentTime: number) {
      const elapsed = currentTime - startTime
      const progress = Math.min(elapsed / duration, 1)
      const easeProgress = 1 - Math.pow(1 - progress, 3)

      const currentLng = startLng + (targetLng - startLng) * easeProgress
      const currentLat = startLat + (targetLat - startLat) * easeProgress
      piece.marker.setLngLat([currentLng, currentLat])

      if (progress < 1) {
        requestAnimationFrame(animate)
      } else {
        onComplete?.()
      }
    }

    requestAnimationFrame(animate)
  }

  /**
   * 攻击动画派发：range <= 1 走近战撞击，range >= 2 走远程炮击
   */
  function playAttackAnimation(
    attackerId: string,
    targetId: string,
    rangeOrCallback?: number | (() => void),
    onComplete?: () => void
  ) {
    let range = 1
    let cb: (() => void) | undefined

    if (typeof rangeOrCallback === 'function') {
      cb = rangeOrCallback
    } else {
      if (typeof rangeOrCallback === 'number' && Number.isFinite(rangeOrCallback)) {
        range = rangeOrCallback
      }
      cb = onComplete
    }

    if (range >= 2) {
      playRangedAttackAnimation(attackerId, targetId, cb)
    } else {
      playMeleeAttackAnimation(attackerId, targetId, cb)
    }
  }

  /**
   * 近战攻击动画 - 冲向目标后返回
   */
  function playMeleeAttackAnimation(attackerId: string, targetId: string, onComplete?: () => void) {
    const attacker = pieces.get(attackerId)
    const target = pieces.get(targetId)

    if (!attacker || !target) {
      onComplete?.()
      return
    }

    const attackerLngLat = attacker.marker.getLngLat()
    const targetLngLat = target.marker.getLngLat()

    const midLng = (attackerLngLat.lng + targetLngLat.lng) / 2
    const midLat = (attackerLngLat.lat + targetLngLat.lat) / 2

    const startTime = performance.now()
    const attackDuration = 300
    const returnDuration = 200

    function phase1(currentTime: number) {
      const elapsed = currentTime - startTime
      const progress = Math.min(elapsed / attackDuration, 1)
      const easeProgress = 1 - Math.pow(1 - progress, 3)

      const currentLng = attackerLngLat.lng + (midLng - attackerLngLat.lng) * easeProgress
      const currentLat = attackerLngLat.lat + (midLat - attackerLngLat.lat) * easeProgress
      attacker.marker.setLngLat([currentLng, currentLat])

      if (progress < 1) {
        requestAnimationFrame(phase1)
      } else {
        const returnStartTime = performance.now()
        requestAnimationFrame((t) => phase2(t, returnStartTime))
      }
    }

    function phase2(currentTime: number, returnStartTime: number) {
      const elapsed = currentTime - returnStartTime
      const progress = Math.min(elapsed / returnDuration, 1)
      const easeProgress = progress * progress

      const currentLng = midLng + (attackerLngLat.lng - midLng) * easeProgress
      const currentLat = midLat + (attackerLngLat.lat - midLat) * easeProgress
      attacker.marker.setLngLat([currentLng, currentLat])

      if (progress < 1) {
        requestAnimationFrame((t) => phase2(t, returnStartTime))
      } else {
        attacker.marker.setLngLat([attackerLngLat.lng, attackerLngLat.lat])
        onComplete?.()
      }
    }

    requestAnimationFrame(phase1)
  }

  /**
   * 远程攻击动画 - 炮口闪光 + 后坐力 + 炮弹飞行 + 命中爆炸
   */
  function playRangedAttackAnimation(attackerId: string, targetId: string, onComplete?: () => void) {
    const attacker = pieces.get(attackerId)
    const target = pieces.get(targetId)

    if (!attacker || !target) {
      onComplete?.()
      return
    }

    const attackerLngLat = attacker.marker.getLngLat()
    const targetLngLat = target.marker.getLngLat()

    // 朝目标的单位向量（用于炮口闪光偏移与后坐力反向）
    const dirLng = targetLngLat.lng - attackerLngLat.lng
    const dirLat = targetLngLat.lat - attackerLngLat.lat
    const dirLen = Math.hypot(dirLng, dirLat) || 1
    const unitLng = dirLng / dirLen
    const unitLat = dirLat / dirLen

    // ---- 1. 炮口闪光（贴在攻击者前方）----
    const flash = document.createElement('div')
    flash.className = 'chess-muzzle-flash'
    const flashOffsetX = unitLng * 28
    const flashOffsetY = -unitLat * 28
    flash.style.left = `calc(50% + ${flashOffsetX}px)`
    flash.style.top = `calc(50% + ${flashOffsetY}px)`
    attacker.shell.appendChild(flash)
    setTimeout(() => flash.remove(), 320)

    // ---- 2. 后坐力（攻击者向反方向轻微回弹再回位）----
    const recoilDistanceLng = -unitLng * dirLen * 0.06
    const recoilDistanceLat = -unitLat * dirLen * 0.06
    const recoilEndLng = attackerLngLat.lng + recoilDistanceLng
    const recoilEndLat = attackerLngLat.lat + recoilDistanceLat
    const recoilStart = performance.now()
    const recoilDuration = 140
    const recoilReturnDuration = 220

    function recoilPhase(currentTime: number) {
      const elapsed = currentTime - recoilStart
      const progress = Math.min(elapsed / recoilDuration, 1)
      const ease = 1 - Math.pow(1 - progress, 2)

      attacker.marker.setLngLat([
        attackerLngLat.lng + recoilDistanceLng * ease,
        attackerLngLat.lat + recoilDistanceLat * ease
      ])

      if (progress < 1) {
        requestAnimationFrame(recoilPhase)
      } else {
        const returnStart = performance.now()
        requestAnimationFrame((t) => recoilReturn(t, returnStart))
      }
    }

    function recoilReturn(currentTime: number, returnStart: number) {
      const elapsed = currentTime - returnStart
      const progress = Math.min(elapsed / recoilReturnDuration, 1)
      const ease = 1 - Math.pow(1 - progress, 3)

      attacker.marker.setLngLat([
        recoilEndLng + (attackerLngLat.lng - recoilEndLng) * ease,
        recoilEndLat + (attackerLngLat.lat - recoilEndLat) * ease
      ])

      if (progress < 1) {
        requestAnimationFrame((t) => recoilReturn(t, returnStart))
      } else {
        attacker.marker.setLngLat([attackerLngLat.lng, attackerLngLat.lat])
      }
    }

    requestAnimationFrame(recoilPhase)

    // ---- 3. 炮弹飞行（带余焰拖尾）----
    const projectileEl = document.createElement('div')
    projectileEl.className = 'chess-projectile'
    const trail = document.createElement('div')
    trail.className = 'chess-projectile-trail'
    projectileEl.appendChild(trail)

    const projectileMarker = new mapboxgl.Marker({
      element: projectileEl,
      anchor: 'center'
    })
      .setLngLat([attackerLngLat.lng, attackerLngLat.lat])
      .addTo(map)

    const flyDelay = 80
    const flyDuration = 480
    const flyStart = performance.now() + flyDelay

    // 抛物线高度（按距离自适应）
    const arcHeight = Math.max(Math.abs(dirLat) * 0.35, 0.005)

    function flyProjectile(currentTime: number) {
      const elapsed = currentTime - flyStart
      if (elapsed < 0) {
        requestAnimationFrame(flyProjectile)
        return
      }
      const progress = Math.min(elapsed / flyDuration, 1)
      const ease = progress // 线性飞行
      const lng = attackerLngLat.lng + (targetLngLat.lng - attackerLngLat.lng) * ease
      const lat = attackerLngLat.lat + (targetLngLat.lat - attackerLngLat.lat) * ease
      const arc = Math.sin(progress * Math.PI) * arcHeight
      projectileMarker.setLngLat([lng, lat + arc])

      if (progress < 1) {
        requestAnimationFrame(flyProjectile)
      } else {
        projectileMarker.remove()
        playExplosion(target)
        onComplete?.()
      }
    }

    requestAnimationFrame(flyProjectile)
  }

  /**
   * 命中爆炸特效（贴在目标棋子上）
   */
  function playExplosion(target: ChessElements) {
    const ring = document.createElement('div')
    ring.className = 'chess-explosion-ring'

    const fire = document.createElement('div')
    fire.className = 'chess-explosion'

    target.shell.appendChild(ring)
    target.shell.appendChild(fire)

    setTimeout(() => {
      ring.remove()
      fire.remove()
    }, 600)
  }

  /**
   * 受击动画 - 闪烁红色
   */
  function playHitAnimation(targetId: string, onComplete?: () => void) {
    const piece = pieces.get(targetId)
    if (!piece) {
      onComplete?.()
      return
    }

    const originalFilter = piece.icon.style.filter
    let flashCount = 0
    const maxFlashes = 3

    function flash() {
      if (flashCount >= maxFlashes * 2) {
        piece.icon.style.filter = originalFilter
        onComplete?.()
        return
      }

      // 切换红色滤镜
      const isRed = flashCount % 2 === 0
      piece.icon.style.filter = isRed 
        ? 'drop-shadow(0 2px 4px rgba(0,0,0,0.3)) brightness(0.8) sepia(1) saturate(5) hue-rotate(-50deg)'
        : originalFilter

      flashCount++
      setTimeout(flash, 100)
    }

    flash()
  }

  /**
   * 更新血量显示
   */
  function updateHp(pieceId: string | number, hp: number, maxHp: number) {
    const strId = String(pieceId)
    const piece = pieces.get(strId)
    if (!piece?.hpBar) return
    
    const percentage = Math.max(0, Math.min(100, (hp / maxHp) * 100))
    piece.hpBar.style.width = `${percentage}%`
  }

  /**
   * 销毁棋子（被击败时）
   */
  function destroyPiece(pieceId: string | number) {
    const strId = String(pieceId)
    const piece = pieces.get(strId)
    if (!piece) return

    // 淡出动画
    piece.shell.style.transition = 'opacity 0.5s, transform 0.5s'
    piece.shell.style.opacity = '0'
    piece.shell.style.transform = 'scale(0.5)'

    setTimeout(() => {
      piece.marker.remove()
      pieces.delete(strId)
      markedAttackTargets.value.delete(strId)
    }, 500)
  }

  // ========== 创建棋子 ==========

  function create(
    chessData: ChessData, 
    options?: { 
      onSelect?: (data: ChessData) => void
      onAttackClick?: (data: ChessData) => void
    }
  ) {
    if (!isChessOnMap(chessData)) return

    ensureAttackMarkStyles()

    const { id, centerLng: lng, centerLat: lat, chessurl, hp = 80, maxHp = 100 } = chessData

    const playerNum = Number(chessData.player ?? 0)
    const isSoviet = playerNum === 0
    const factionClass = isSoviet ? 'faction-soviet' : 'faction-german'
    const haloColor = isSoviet
      ? 'rgba(59, 130, 246, 0.6)'   // 我方蓝色
      : 'rgba(245, 158, 11, 0.55)'  // 敌方琥珀色（仅查看，不可控）
    const pulseColor = isSoviet ? '#3b82f6' : '#f59e0b'

    const initialSize = calculateSize()
    const offset = initialSize / 2

    const shell = document.createElement('div')
    shell.className = `chess-piece ${factionClass}`
    shell.style.cssText = `
      position: absolute;
      width: ${initialSize}px;
      height: ${initialSize}px;
      margin-left: ${-offset}px;
      margin-top: ${-offset}px;
      pointer-events: auto;
      cursor: pointer;
      z-index: 1;`

    const inner = document.createElement('div')
    inner.style.cssText = `width: 100%; height: 100%; position: relative;`

    // 阵营外框（敌我描边）
    const frame = document.createElement('div')
    frame.className = `chess-piece-frame ${factionClass}`

    const icon = document.createElement('div')
    icon.className = 'chess-icon'
    icon.style.cssText = `
      width: 100%; height: 100%;
      background-image: url(${chessurl});
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
      position: relative;
      z-index: 1;`

    // 阵营徽章（右上角）
    const badge = document.createElement('div')
    badge.className = `chess-piece-badge ${factionClass}`
    badge.innerHTML = isSoviet
      ? `<svg viewBox="0 0 24 24" fill="#fde047" aria-label="苏军">
           <polygon points="12,2 14.6,9.1 22,9.4 16.1,13.9 18.3,21 12,16.7 5.7,21 7.9,13.9 2,9.4 9.4,9.1"/>
         </svg>`
      : `<svg viewBox="0 0 24 24" fill="#ffffff" aria-label="德军">
           <rect x="3.5" y="10.2" width="17" height="3.6" rx="0.4"/>
           <rect x="10.2" y="3.5" width="3.6" height="17" rx="0.4"/>
         </svg>`

    // 选中光晕（颜色依阵营）
    const halo = document.createElement('div')
    halo.className = 'chess-halo'
    halo.style.cssText = `
      position: absolute; inset: -25%;
      border-radius: 50%;
      background: radial-gradient(circle, ${haloColor} 0%, transparent 60%);
      opacity: 0; transform: scale(0.5);
      pointer-events: none; z-index: -1;`

    // 脉冲动画（颜色依阵营）
    const pulse = document.createElement('div')
    pulse.className = 'chess-pulse'
    pulse.style.cssText = `
      position: absolute; inset: -15%;
      border: 3px solid ${pulseColor};
      border-radius: 50%;
      opacity: 0; pointer-events: none; z-index: -1;`

    // 攻击标记（棋子下方红色向内箭头）
    const attackMark = document.createElement('div')
    attackMark.className = 'chess-attack-mark'
    const attackMarkSize = Math.max(initialSize * 1.75, 64)
    attackMark.style.cssText = `
      position: absolute; left: 50%; top: 50%;
      transform: translate(-50%, -50%);
      width: ${attackMarkSize}px;
      height: ${attackMarkSize}px;
      display: none; pointer-events: none; z-index: 10;`
    attackMark.innerHTML = `
      <div class="chess-attack-arrow arrow-1"></div>
      <div class="chess-attack-arrow arrow-2"></div>
      <div class="chess-attack-arrow arrow-3"></div>
      <div class="chess-attack-arrow arrow-4"></div>
      <div class="chess-attack-arrow arrow-5"></div>
      <div class="chess-attack-arrow arrow-6"></div>
      <div class="chess-attack-arrow arrow-7"></div>
      <div class="chess-attack-arrow arrow-8"></div>
    `

    // 血条
    const hpBox = document.createElement('div')
    hpBox.style.cssText = `
      position: absolute; left: 50%; transform: translateX(-50%);
      bottom: ${-initialSize * 0.15}px;
      width: ${initialSize * 0.8}px; height: 8px;`
    
    const hpBg = document.createElement('div')
    hpBg.style.cssText = `
      width: 100%; height: 100%;
      background: rgba(0,0,0,.4);
      border: 1px solid #000;
      border-radius: 4px;
      overflow: hidden;`
    
    const hpBar = document.createElement('div')
    hpBar.className = 'chess-hp-bar'
    hpBar.style.cssText = `
      height: 100%;
      background: linear-gradient(to right, #ff4d4f, #ffa940);
      width: ${(hp / maxHp) * 100}%;`
    
    hpBg.appendChild(hpBar)
    hpBox.appendChild(hpBg)
    inner.appendChild(frame)
    inner.appendChild(icon)
    inner.appendChild(badge)
    inner.appendChild(halo)
    inner.appendChild(pulse)
    inner.appendChild(attackMark)
    inner.appendChild(hpBox)
    shell.appendChild(inner)

    // 点击事件
    shell.addEventListener('click', (e) => {
      e.stopPropagation()
      const strId = String(id)
      
      // 如果有攻击标记，优先触发攻击
      if (hasAttackMark(strId) && options?.onAttackClick) {
        options.onAttackClick(chessData)
        return
      }
      
      // 否则正常选中
      select(id)
      options?.onSelect?.(chessData)
    })

    const marker = new mapboxgl.Marker({
      element: shell,
      anchor: 'top-left',
      offset: [0, 0]
    })
      .setLngLat([lng, lat])
      .addTo(map)

    const piece: ChessElements = { 
      id, shell, icon, halo, pulse, 
      hpBox, hpBar, attackMark, marker 
    }
    pieces.set(String(id), piece)

    return { id, lng, lat, marker }
  }

  // ========== 选中相关 ==========

  function select(pieceId: string | number) {
    const strId = String(pieceId)
    
    if (selectedId.value === strId) {
      clear()
      return
    }

    if (selectedId.value) {
      const prevId = selectedId.value
      stopPulseAnimation(prevId)
      selectedId.value = null
      updateSize(prevId)
    }

    selectedId.value = strId
    updateSize(strId)
    
    const piece = pieces.get(strId)
    if (piece) {
      piece.halo.style.opacity = '1'
      piece.halo.style.transform = 'scale(1.2)'
      startPulseAnimation(strId)
    }
  }

  function startPulseAnimation(pieceId: string) {
    const piece = pieces.get(pieceId)
    if (!piece) return
    
    piece.pulse.style.opacity = '1'
    let scale = 1
    
    const animate = () => {
      if (selectedId.value !== pieceId) return
      scale += 0.008
      if (scale > 1.3) scale = 1
      piece.pulse.style.transform = `scale(${scale})`
      piece.pulse.style.opacity = String(1 - (scale - 1) / 0.3)
      const animId = requestAnimationFrame(animate)
      animationIds.set(pieceId, animId)
    }
    animate()
  }

  function stopPulseAnimation(pieceId: string) {
    const piece = pieces.get(pieceId)
    if (!piece) return
    
    piece.halo.style.opacity = '0'
    piece.halo.style.transform = 'scale(0.5)'
    piece.pulse.style.opacity = '0'
    piece.pulse.style.transform = 'scale(1)'
    
    const animId = animationIds.get(pieceId)
    if (animId) {
      cancelAnimationFrame(animId)
      animationIds.delete(pieceId)
    }
  }

  function clear() {
    const previousId = selectedId.value
    if (previousId) {
      stopPulseAnimation(previousId)
      selectedId.value = null
      updateSize(previousId)
    }
    clearAttackMarks()
  }

  function destroy() {
    map.off('zoom', updateAllSizes)
    map.off('move', updateAllSizes)
    
    pieces.forEach((piece) => piece.marker.remove())
    pieces.clear()
    animationIds.clear()
    selectedId.value = null
    markedAttackTargets.value.clear()
  }

  /**
   * 用最新的棋子数据全量重建：
   *  - 销毁现有 marker / 动画 / 选中 / 攻击标记
   *  - 按新数据重新 create
   * 用于回合结束后从后端拉取最新 moved/fired 等状态后同步到地图。
   */
  function reloadAll(
    chessData: ChessData[],
    options?: {
      onSelect?: (data: ChessData) => void
      onAttackClick?: (data: ChessData) => void
    }
  ) {
    animationIds.forEach((id) => cancelAnimationFrame(id))
    animationIds.clear()

    pieces.forEach((piece) => piece.marker.remove())
    pieces.clear()

    selectedId.value = null
    markedAttackTargets.value.clear()

    chessData.forEach((piece) => {
      if (!isChessOnMap(piece)) return
      create(piece, options)
    })
  }

  function updateAllSizes() {
    updateSize()
  }

  map.on('zoom', updateAllSizes)
  map.on('move', updateAllSizes)

  return {
    // 基础
    create,
    select,
    clear,
    destroy,
    reloadAll,
    selectedId,
    moveTo,
    updateHp,
    destroyPiece,
    
    // 攻击标记
    markAttackableTargets,
    clearAttackMarks,
    hasAttackMark,
    
    // 动画
    playAttackAnimation,
    playHitAnimation,
    
    updateSize: updateAllSizes
  }
}