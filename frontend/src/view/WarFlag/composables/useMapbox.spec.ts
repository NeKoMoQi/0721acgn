import { mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { expect, it, vi } from 'vitest'
import { useMapbox } from './useMapbox'

const { maps } = vi.hoisted(() => ({
  maps: [] as { loaded: () => void; remove: ReturnType<typeof vi.fn> }[],
}))
vi.mock('mapbox-gl', () => ({
  default: { Map: class {
    loaded = () => {}
    remove = vi.fn()
    constructor() { maps.push(this) }
    addControl() {}
    addSource() {}
    addLayer() {}
    getLayer() { return null }
    setFilter() {}
    on(event: string, handler: () => void) { if (event === 'load') this.loaded = handler }
    off(event: string) { if (event === 'load') this.loaded = () => {} }
  } },
}))
vi.mock('@mapbox/mapbox-gl-language', () => ({ default: class {} }))
vi.mock('@/api/warflag/warChessing', () => ({
  getWarChessing: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getWarChessingRange: vi.fn(), moveChess: vi.fn(), resetBattle: vi.fn(), runEnemyAiTurn: vi.fn(),
  getWarChessingAttackRange: vi.fn(), attackChess: vi.fn(),
}))
vi.mock('./useBattleState', () => ({
  useBattleState: () => ({ state: { value: null }, refresh: vi.fn() }),
}))
vi.mock('./useChessPiece', () => ({
  useChessPiece: () => ({ reloadAll: vi.fn() }),
}))

it('回收超时地图、允许重试并在离开战场时取消等待', async () => {
  vi.useFakeTimers()
  let api!: ReturnType<typeof useMapbox>
  const BattleMap = defineComponent({
    setup() {
      api = useMapbox(ref(document.createElement('div')), {
        center: [0, 0], zoom: 1, minZoom: 1, maxZoom: 2,
        maxBounds: [[-1, -1], [1, 1]], battleId: 1001, myPlayerId: 0,
      })
      return () => null
    },
  })
  const view = mount(BattleMap)
  try {
    const timeout = expect(api.init([], [])).rejects.toThrow('地图加载超时')
    await vi.advanceTimersByTimeAsync(12000)
    await timeout
    expect(maps[0].remove).toHaveBeenCalledTimes(1)
    expect(api.map.value).toBeNull()

    const retry = api.init([], [])
    await Promise.resolve()
    maps[1].loaded()
    await retry
    expect(api.map.value).toBe(maps[1])

    view.unmount()
    expect(maps[1].remove).toHaveBeenCalledTimes(1)

    const nextView = mount(BattleMap)
    const leaving = expect(api.init([], [])).rejects.toThrow('已离开战场')
    await Promise.resolve()
    nextView.unmount()
    await leaving
    expect(maps[2].remove).toHaveBeenCalledTimes(1)
  } finally {
    vi.useRealTimers()
  }
})
