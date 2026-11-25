export const UJI_WORK_ID = 424
export const UJI_ANIMATION_ID = 11187
export const UJI_WALK = '/shengdi/uji'
export const UJI_SOURCES_CHECKED = '2026-11-25'
export const ujiSources = [
  { title: '京阪电车 · 舞台探访地图', url: 'https://www.keihan.co.jp/euphonium/assets/img/pamphlet.pdf' },
  { title: '宇治市 · 动画中的宇治风景', url: 'https://www.city.uji.kyoto.jp/uploaded/attachment/32906.pdf' },
]

export const ujiStops = [
  {
    id: 9655, name: '宇治桥西侧', ep: 1, coordinates: [135.8057, 34.8925],
    note: '从桥西侧沿宇治川往上游走。回望桥面和河岸，可以对照第一季开篇的街景。',
    photo: {
      src: '/xunli/uji/uji-bridge.webp',
      source: 'https://commons.wikimedia.org/wiki/File:Uji-Bridge.jpg',
      author: '京都東',
    },
  },
  {
    id: 9508, name: '网代木之道', ep: 1, coordinates: [135.8081, 34.8899],
    note: '沿平等院一侧的河滨步道前行。这里是第一季开篇的河边镜头取景处。',
    photo: {
      src: '/xunli/uji/ajirogi.webp',
      source: 'https://commons.wikimedia.org/wiki/File:%E3%81%82%E3%81%98%E3%82%8D%E3%81%8E%E3%81%AE%E9%81%93.jpg',
      author: '御影守駒氏',
    },
  },
  {
    id: 9670, name: '朝雾桥东端', ep: 4, coordinates: [135.8098, 34.8906],
    note: '经宇治公园一带过河，从东端回望桥身。站内对照收录的是第一季第四集的东端镜头。',
    photo: {
      src: '/xunli/uji/asagiri.webp',
      source: 'https://commons.wikimedia.org/wiki/File:Asagiri_Bashi%26Hikaru_Genji_statue.jpg',
      author: '御影守駒氏',
    },
  },
  {
    id: 9690, name: '大吉山展望台', ep: 8, coordinates: [135.8125, 34.8926],
    note: '从宇治上神社附近上山。第一季第八集里，久美子和丽奈带着乐器来到这里，可以从展望台俯瞰宇治川。',
    photo: {
      src: '/xunli/uji/daikichiyama.webp',
      source: 'https://commons.wikimedia.org/wiki/File:%E5%A4%A7%E5%90%89%E5%B1%B1%E5%B1%95%E6%9C%9B.jpg',
      author: 'オクノサンライズ',
    },
  },
] as const

export const ujiWorkLink = { name: 'Animationintroduce', query: { animationid: UJI_ANIMATION_ID } }
export const ujiMapLink = { name: 'shengdi', query: { walk: 'uji' } }
export const ujiStopLink = (spot: number) => ({ name: 'duizhao', query: { work: UJI_WORK_ID, spot } })
