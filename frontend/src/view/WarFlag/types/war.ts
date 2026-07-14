export interface GridItem {
  id: string
  q: number
  r: number
  elevation: number
  center: [number, number]
  gridgeometry: [number, number][]
  centerLng?: number
  centerLat?: number
}

// ========== ????????? ==========
export interface ChessData {
  id: string | number
  centerLng: number
  centerLat: number
  chessurl: string
  hp: number
  maxHp: number
  moveRange?: number                // 行动范围（格数），默认 2
  [key: string]: any
}

/**
 * 是否应在地图上绘制该棋子。
 * 后端 getWarChessing 会返回 hp=0 的阵亡单位，若照常 create 会从视觉上「复活」。
 */
export function isChessOnMap(p: Pick<ChessData, 'hp'>): boolean {
  const h = p.hp
  if (h === undefined || h === null) return true
  return Number(h) > 0
}

// ========== 棋子 DOM 元素引用 ==========
export interface ChessElements {
  id: string | number               // 棋子 ID
  shell: HTMLDivElement             // 外壳容器
  icon: HTMLDivElement              // 图标
  halo: HTMLDivElement              // 光环
  pulse: HTMLDivElement             // 脉冲动画
  hpBox: HTMLDivElement             // 血条容器
  hpBar: HTMLDivElement             // 血条
  attackMark: HTMLDivElement        // 攻击标记
  marker: mapboxgl.Marker           // Mapbox Marker 实例
}

// ========== 地图选中信息类型 ==========
export type SelectedInfo = 
  | { type: 'grid'; data: GridItem }
  | { type: 'chess'; data: ChessData }
  | null

// ========== 地图初始化选项 ==========
export interface MapOptions {
  center: [number, number]
  zoom: number
  minZoom: number
  maxZoom: number
  maxBounds: [[number, number], [number, number]]
}

// ========== 棋子创建选项 ==========
export interface ChessCreateOptions {
  onSelect?: (data: ChessData) => void
}

// ========== 六边形方向向量（轴向坐标）==========
export interface HexDirection {
  q: number
  r: number
}

// 六边形六个方向
export const HEX_DIRECTIONS: HexDirection[] = [
  { q: 1, r: 0 },   // 东
  { q: 1, r: -1 },  // 东北
  { q: 0, r: -1 },  // 西北
  { q: -1, r: 0 },  // 西
  { q: -1, r: 1 },  // 西南
  { q: 0, r: 1 },   // 东南
]

// ========== BFS 队列节点 ==========
export interface BFSNode {
  q: number
  r: number
  dist: number
}

// ========== API 响应类型 ==========
export interface ApiResponse<T> {
  code: number
  data: T
  message?: string
}

// 格子数据 API 响应
export type GridApiResponse = ApiResponse<GridItem[]>

// 棋子数据 API 响应
export type ChessApiResponse = ApiResponse<ChessData[]>

/** 后端 EnemyAiService 返回的单步动作（与 Java AiAction 字段一致） */
export interface AiAction {
  type: string
  chessId: string
  fromQ?: number | null
  fromR?: number | null
  toQ?: number | null
  toR?: number | null
  toLng?: number | null
  toLat?: number | null
  targetId?: string | null
  damage?: number | null
  targetNewHp?: number | null
  targetDestroyed?: boolean | null
  counterAttacked?: boolean | null
  counterDamage?: number | null
  attackerNewHp?: number | null
  attackerDestroyed?: boolean | null
  message?: string | null
}

/** POST /battle/aiTurn 的 data 载荷 */
export interface AiTurnResultData {
  executed: boolean
  message?: string
  state?: Record<string, unknown>
  actions?: AiAction[]
}
