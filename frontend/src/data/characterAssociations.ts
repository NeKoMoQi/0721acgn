import catalogAssociations from './catalogCharacterAssociations.json'
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
      { id: 'heiter', name: '海塔', aliases: ['ハイター', 'Heiter'] },
      { id: 'eisen', name: '艾泽', aliases: ['艾澤', 'アイゼン', 'Eisen'] },
      { id: 'flamme', name: '伏拉梅', aliases: ['弗兰梅', 'フランメ', 'Flamme'] },
      { id: 'sein', name: '赞因', aliases: ['贊因', 'ザイン', 'Sein'] },
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
      { id: 'seika', name: '伊地知星歌', aliases: ['伊地知せいか', 'Seika Ijichi'] },
      { id: 'kikuri', name: '广井菊里', aliases: ['廣井菊里', '廣井きくり', 'Hiroi Kikuri'] },
      { id: 'bocchi-pa', name: '音响师', aliases: ['音響師', 'PA桑', 'PA小姐', 'PAさん'] },
      { id: 'futari', name: '后藤二里', aliases: ['後藤ふたり', '後藤二里', 'Futari Gotoh'] },
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
      { id: 'asuka', name: '田中明日香', aliases: ['田中あすか'] },
      { id: 'natsuki', name: '中川夏纪', aliases: ['中川夏紀'] },
      { id: 'yuuko', name: '吉川优子', aliases: ['吉川優子'] },
      { id: 'taki', name: '泷升', aliases: ['滝昇'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 1, title: '欢迎来到实力至上主义的教室' },
      { type: 'animation', id: 10872, title: '欢迎来到实力至上主义的教室' },
    ],
    people: [
      { id: 76, name: '轻井泽惠', aliases: ['軽井沢恵'] },
      { id: 'ayanokoji', name: '绫小路清隆', aliases: ['綾小路清隆'] },
      { id: 54, name: '堀北铃音', aliases: ['堀北鈴音'] },
      { id: 31, name: '栉田桔梗', aliases: ['櫛田桔梗'] },
      { id: 'ichinose', name: '一之濑帆波', aliases: ['一之瀬帆波'] },
      { id: 'sakayanagi', name: '坂柳有栖' },
    ],
  },
  {
    works: [
      { type: 'novel', id: 7, title: '不时轻声地以俄语遮羞的邻座艾莉同学' },
      { type: 'animation', id: 11165, title: '不时用俄语小声说真心话的邻桌艾莉同学' },
    ],
    people: [
      { id: 80, name: '周防有希', aliases: ['Yuki Suou'] },
      { id: 'alya', name: '艾莉莎·米哈伊罗芙娜·九条', aliases: ['艾莉同学', 'アリサ・ミハイロヴナ・九条', 'アーリャ'] },
      { id: 'masachika', name: '久世政近' },
      { id: 'masha', name: '玛利亚·米哈伊罗芙娜·九条', aliases: ['玛莎', 'マリヤ・ミハイロヴナ・九条', 'マーシャ'] },
      { id: 'ayano', name: '君岛绫乃', aliases: ['君嶋綾乃'] },
    ],
  },
  {
    works: [{ type: 'animation', id: 10840, title: '鬼灭之刃' }],
    people: [
      { id: 47, name: '富冈义勇', aliases: ['冨岡義勇'] },
      { id: 202, name: '灶门炭治郎', aliases: ['竈門炭治郎'] },
      { id: 'nezuko', name: '灶门祢豆子', aliases: ['灶门禰豆子', '竈門禰豆子'] },
      { id: 'zenitsu', name: '我妻善逸' },
      { id: 121, name: '嘴平伊之助' },
    ],
  },
  {
    works: [{ type: 'animation', id: 11227, title: '碧蓝航线' }],
    people: [{ id: 104, name: '光辉', aliases: ['イラストリアス', 'Illustrious'] }],
  },
  {
    works: [{ type: 'animation', id: 10963, title: '天使降临到我身边' }],
    people: [{ id: 6, name: '星野日向', aliases: ['星野ひなた'] }],
  },
  {
    works: [
      { type: 'animation', id: 10854, title: '冰菓' },
    ],
    people: [
      { id: 127, name: '折木奉太郎', aliases: ['折木ほうたろう'] },
      { id: 'chitanda', name: '千反田爱瑠', aliases: ['千反田える'] },
      { id: 'satoshi', name: '福部里志' },
      { id: 'mayaka', name: '伊原摩耶花' },
    ],
  },
  {
    works: [
      { type: 'novel', id: 6, title: '我的青春恋爱物语果然有问题' },
      { type: 'animation', id: 10912, title: '我的青春恋爱物语果然有问题。' },
      { type: 'animation', id: 10983, title: '我的青春恋爱物语果然有问题。续' },
      { type: 'animation', id: 10894, title: '我的青春恋爱物语果然有问题。完' },
    ],
    people: [
      { id: 226, name: '比企谷八幡' },
      { id: 138, name: '户冢彩加', aliases: ['戸塚彩加'] },
      { id: 11, name: '雪之下雪乃', aliases: ['雪ノ下雪乃'] },
      { id: 176, name: '由比滨结衣', aliases: ['由比ヶ浜結衣', '由比濱結衣'] },
      { id: 'shizuka', name: '平冢静', aliases: ['平塚静', '平塚 静'] },
    ],
  },
  {
    works: [
      { type: 'animation', id: 11231, title: '笨蛋，测验，召唤兽' },
      { type: 'animation', id: 11523, title: '笨蛋，测验，召唤兽2' },
    ],
    people: [
      { id: 1, name: '木下秀吉', aliases: ['秀吉', 'きのしたひでよし'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 32, title: 'DATE A LIVE' },
      { type: 'animation', id: 11099, title: '约会大作战 第三季' },
      { type: 'animation', id: 10895, title: '约会大作战 第四季' },
    ],
    people: [
      { id: 155, name: '五河士道' },
      { id: 27, name: '诱宵美九', aliases: ['誘宵美九'] },
      { id: 168, name: '时崎狂三', aliases: ['時崎狂三'] },
      { id: 294, name: '八舞夕弦' },
    ],
  },
  {
    works: [
      { type: 'animation', id: 11001, title: '魔法少女小圆' },
    ],
    people: [
      { id: 281, name: '鹿目圆', aliases: ['鹿目まどか', '鹿目圆香'] },
    ],
  },
  {
    works: [
      { type: 'animation', id: 11364, title: '哆啦A梦 第一季' },
    ],
    people: [
      { id: 252, name: '哆啦A梦', aliases: ['ドラえもん', 'Doraemon'] },
    ],
  },
  {
    works: [
      { type: 'animation', id: 10882, title: '中二病也要谈恋爱！' },
      { type: 'animation', id: 11005, title: '中二病也要谈恋爱！恋' },
    ],
    people: [
      { id: 227, name: '凸守早苗' },
    ],
  },
  {
    works: [
      { type: 'comic', id: 6, title: '五等分的新娘' },
      { type: 'animation', id: 10933, title: '五等分的新娘' },
      { type: 'animation', id: 10926, title: '五等分的新娘∬' },
    ],
    people: [
      { id: 'fuutarou', name: '上杉风太郎', aliases: ['上杉風太郎'] },
      { id: 'ichika', name: '中野一花' },
      { id: 'nino', name: '中野二乃' },
      { id: 134, name: '中野三玖' },
      { id: 'yotsuba', name: '中野四叶', aliases: ['中野四葉'] },
      { id: 135, name: '中野五月' },
    ],
  },
  {
    works: [
      { type: 'comic', id: 25, title: '更衣人偶坠入爱河' },
    ],
    people: [
      { id: 'marin', name: '喜多川海梦', aliases: ['喜多川海夢'] },
      { id: 'wakana', name: '五条新菜' },
      { id: 'sajuna', name: '乾纱寿叶', aliases: ['乾紗寿叶', '朱朱'] },
      { id: 'shinju', name: '乾心寿' },
    ],
  },
  {
    works: [
      { type: 'comic', id: 37, title: '地缚少年花子君' },
      { type: 'animation', id: 10966, title: '地缚少年花子君' },
      { type: 'animation', id: 10958, title: '地缚少年花子君 第二季' },
    ],
    people: [
      { id: 'hanako', name: '花子君', aliases: ['花子くん'] },
      { id: 'nene', name: '八寻宁宁', aliases: ['八尋寧々'] },
      { id: 'kou', name: '源光' },
      { id: 'mitsuba', name: '三叶惣助', aliases: ['三葉惣助', '三叶'] },
    ],
  },
  {
    works: [
      { type: 'comic', id: 5, title: '魔女之旅' },
      { type: 'animation', id: 10943, title: '魔女之旅' },
    ],
    people: [
      { id: 293, name: '伊蕾娜', aliases: ['イレイナ'] },
      { id: 'fran', name: '芙兰', aliases: ['フラン'] },
      { id: 'saya', name: '沙耶', aliases: ['サヤ'] },
      { id: 'shela', name: '席拉', aliases: ['シーラ'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 18, title: '药屋少女的呢喃' },
    ],
    people: [
      { id: 'maomao', name: '猫猫', aliases: ['マオマオ'] },
      { id: 'jinshi', name: '壬氏', aliases: ['ジンシ'] },
      { id: 'gaoshun', name: '高顺', aliases: ['高順'] },
      { id: 'gyokuyou', name: '玉叶妃', aliases: ['玉葉妃'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 3, title: '败犬女主太多了！' },
    ],
    people: [
      { id: 'nukumizu', name: '温水和彦' },
      { id: 335, name: '八奈见杏菜', aliases: ['八奈見杏菜'] },
      { id: 'lemon', name: '烧盐柠檬', aliases: ['焼塩檸檬'] },
      { id: 'chika', name: '小鞠知花' },
    ],
  },
  {
    works: [
      { type: 'novel', id: 4, title: '青春猪头少年不会梦到兔女郎学姐' },
      { type: 'animation', id: 10860, title: '青春猪头少年不会梦到兔女郎学姐' },
      { type: 'animation', id: 10913, title: '青春猪头少年不会梦到怀梦美少女' },
    ],
    people: [
      { id: 'sakuta', name: '梓川咲太' },
      { id: 'mai', name: '樱岛麻衣', aliases: ['桜島麻衣', '櫻島麻衣'] },
      { id: 'futaba', name: '双叶理央', aliases: ['双葉理央'] },
      { id: 'koga', name: '古贺朋绘', aliases: ['古賀朋絵'] },
    ],
  },
  {
    works: [
      { type: 'novel', id: 8, title: '86-不存在的战区小说' },
    ],
    people: [
      { id: 'shin', name: '辛耶·诺赞', aliases: ['辛', 'シンエイ・ノウゼン'] },
      { id: 'lena', name: '芙拉蒂蕾娜·米利杰', aliases: ['蕾娜', 'ヴラディレーナ・ミリーゼ'] },
      { id: 'raiden', name: '莱登·修迦', aliases: ['ライデン・シュガ'] },
      { id: 'anju', name: '安琪·艾玛', aliases: ['アンジュ・エマ', '安洁'] },
    ],
  },
]

const linkedCasts = [...casts, ...(catalogAssociations as typeof casts)]

export function castForWork(type: CharacterWork['type'], id: number) {
  const matches = linkedCasts.filter(group => group.works.some(work => work.type === type && work.id === id))
  const people = new Map<string, CastMember>()
  for (const person of matches.flatMap(group => group.people)) {
    const key = String(person.id)
    if (!people.has(key)) people.set(key, person)
  }
  return [...people.values()]
}

export function worksForCharacter(id?: number | string) {
  const matches = linkedCasts.filter(group => group.people.some(person => String(person.id) === String(id)))
  const works = new Map<string, CharacterWork>()
  for (const work of matches.flatMap(group => group.works)) {
    const key = `${work.type}:${work.id}`
    if (!works.has(key)) works.set(key, work)
  }
  return [...works.values()]
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
