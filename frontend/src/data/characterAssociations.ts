import { UJI_ANIMATION_ID } from './ujiPilgrimage'

export interface CastMember {
  id: number | string
  name: string
  aliases?: string[]
}

export interface CharacterWork {
  type: 'animation' | 'comic' | 'novel'
  id: number
  title: string
}

const casts: { works: CharacterWork[]; people: CastMember[] }[] = [
  {
    works: [
      { type: 'comic', id: 24, title: '葬送的芙莉莲' },
      { type: 'animation', id: 10941, title: '葬送的芙莉莲' },
    ],
    people: [
      { id: 'frieren', name: '芙莉莲', aliases: ['芙莉蓮', 'フリーレン', 'Frieren'] },
      { id: 'fern', name: '菲伦', aliases: ['菲倫', '费伦', 'フェルン', 'Fern'] },
      { id: 'stark', name: '休塔尔克', aliases: ['休塔爾克', 'シュタルク', 'Stark'] },
      { id: 'himmel', name: '辛美尔', aliases: ['欣梅尔', '欣梅爾', 'ヒンメル', 'Himmel'] },
    ],
  },
  {
    works: [
      { type: 'comic', id: 31, title: '孤独摇滚！' },
      { type: 'animation', id: 10952, title: '孤独摇滚！' },
    ],
    people: [
      { id: 'hitori', name: '后藤一里', aliases: ['後藤ひとり', '小孤独', '波奇'] },
      { id: 'nijika', name: '伊地知虹夏', aliases: ['にじか'] },
      { id: 'ryo', name: '山田凉', aliases: ['山田涼', '山田リョウ'] },
      { id: 'kita', name: '喜多郁代' },
    ],
  },
  {
    works: [
      { type: 'animation', id: UJI_ANIMATION_ID, title: '吹响吧！上低音号' },
      { type: 'animation', id: 11385, title: '吹响吧！上低音号 第二季' },
      { type: 'animation', id: 11160, title: '吹响吧！上低音号 第三季' },
    ],
    people: [
      { id: 'kumiko', name: '黄前久美子' },
      { id: 'reina', name: '高坂丽奈', aliases: ['高坂麗奈'] },
      { id: 'hazuki', name: '加藤叶月', aliases: ['加藤葉月'] },
      { id: 'sapphire', name: '川岛绿辉', aliases: ['川島緑輝'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 1, title: '欢迎来到实力至上主义的教室' },
      { type: 'animation', id: 10872, title: '欢迎来到实力至上主义的教室' },
    ],
    people: [{ id: 76, name: '轻井泽惠', aliases: ['軽井沢恵'] }],
  },
  {
    works: [
      { type: 'novel', id: 7, title: '不时轻声地以俄语遮羞的邻座艾莉同学' },
      { type: 'animation', id: 11165, title: '不时用俄语小声说真心话的邻桌艾莉同学' },
    ],
    people: [{ id: 80, name: '周防有希', aliases: ['Yuki Suou'] }],
  },
  {
    works: [{ type: 'animation', id: 10840, title: '鬼灭之刃' }],
    people: [{ id: 47, name: '富冈义勇', aliases: ['冨岡義勇'] }],
  },
  {
    works: [{ type: 'animation', id: 11227, title: '碧蓝航线' }],
    people: [{ id: 104, name: '光辉', aliases: ['イラストリアス', 'Illustrious'] }],
  },
  {
    works: [{ type: 'animation', id: 10963, title: '天使降临到我身边' }],
    people: [{ id: 6, name: '星野日向', aliases: ['星野ひなた'] }],
  },
]

export function castForWork(type: CharacterWork['type'], id: number) {
  return casts.find(group => group.works.some(work => work.type === type && work.id === id))?.people ?? []
}

export function worksForCharacter(id?: number | string) {
  return casts.find(group => group.people.some(person => String(person.id) === String(id)))?.works ?? []
}

export function editorialCharacter(id: string) {
  return casts.flatMap(group => group.people).find(person => person.id === id)
}

export function workLocation(work: CharacterWork) {
  if (work.type === 'comic') return { name: 'ComicIntroduce', query: { comicid: work.id } }
  if (work.type === 'novel') return { name: 'NovelIntroduce', query: { novelid: work.id } }
  return { name: 'Animationintroduce', query: { animationid: work.id } }
}

export function workTypeLabel(type: CharacterWork['type']) {
  return { animation: '动漫', comic: '漫画', novel: '轻小说' }[type]
}

export function matchingCast(keyword: string) {
  const query = keyword.normalize('NFKC').toLowerCase()
  return casts.flatMap(({ works, people }) => people
    .filter(person => {
      if (!query) return typeof person.id === 'string'
      const terms = [person.name, ...(person.aliases || [])]
      return terms.some(term => term.normalize('NFKC').toLowerCase().includes(query))
    })
    .map(person => ({ ...person, title: works[0].title })))
}
