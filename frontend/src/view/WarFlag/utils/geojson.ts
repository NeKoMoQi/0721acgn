import type { GridItem } from '@/view/WarFlag/types/war.ts'

export function buildGeoJSON(gridData: GridItem[]) {
  return {
    type: 'FeatureCollection' as const,
    features: gridData.map(item => ({
      type: 'Feature' as const,
      geometry: {
        type: 'Polygon' as const,
        coordinates: [item.gridgeometry]
      },
      properties: {
        elevation: item.elevation,
        q: item.q,
        r: item.r,
        id: item.id,
        center: item.center
      }
    }))
  }
}
