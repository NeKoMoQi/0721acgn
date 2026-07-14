import mapboxgl from 'mapbox-gl'

export interface ChessData {
  centerLng: number
  centerLat: number
  chessurl: string
  hp?: number
  maxHp?: number
}

/**
 * 在指定地图上创建一个“棋子+血条”的 Marker
 * @param map mapboxgl.Map 实例
 * @param data 棋子数据
 * @returns 返回 Marker 实例，方便外部移除或更新血量
 */
export function createChessMarker(
  map: mapboxgl.Map,
  data: ChessData
): mapboxgl.Marker {
  const { centerLng, centerLat, chessurl, hp = 100, maxHp = 100 } = data

  /* 最外层包裹：行内 60×60，Mapbox 能立即读到尺寸 */
  const wrapper = document.createElement('div')
  wrapper.style.cssText = `
    width:60px;
    height:60px;
    position:relative;
    pointer-events:auto;
  `

  /* 棋子本体 */
  const icon = document.createElement('div')
  icon.style.cssText = `
    width:100%;
    height:100%;
    background-image:url(${chessurl});
    background-size:contain;
    background-repeat:no-repeat;
    background-position:center;
  `

  /* 血条容器 */
  const hpBox = document.createElement('div')
  hpBox.style.cssText = `
    position:absolute;
    left:50%;
    bottom:-10px;
    transform:translateX(-50%);
    width:50px;
    height:8px;
  `

  /* 血条背景 */
  const hpBg = document.createElement('div')
  hpBg.style.cssText = `
    width:100%;
    height:100%;
    background:rgba(0,0,0,.4);
    border:1px solid #000;
    border-radius:4px;
    overflow:hidden;
  `

  /* 实际血量 */
  const hpInner = document.createElement('div')
  hpInner.style.cssText = `
    height:100%;
    background:linear-gradient(to right,#ff4d4f,#ffa940);
    width:${(hp / maxHp) * 100}%;
    transition:width .3s;
  `

  /* 拼 DOM 树 */
  hpBg.appendChild(hpInner)
  hpBox.appendChild(hpBg)
  wrapper.appendChild(icon)
  wrapper.appendChild(hpBox)

  /* 生成 Marker */
  const marker = new mapboxgl.Marker(wrapper, { anchor: 'center' })
    .setLngLat([centerLng, centerLat])
    .addTo(map)

  /* 把血量元素挂到 marker 实例上，方便外部更新 */
  ;(marker as any).hpInner = hpInner
  ;(marker as any).maxHp = maxHp

  return marker
}

/* 辅助方法：更新血量 */
