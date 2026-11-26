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
  {
    works: [{ type: 'novel', id: 11, title: '每晚啾啾吸血后娇羞撒娇的吸血鬼公主' }],
    people: [
      { id: 'akatsuki-shiro', name: '红月史郎', aliases: ['紅月史郎', '紅月史朗'] },
      { id: 'tetra', name: '泰特拉', aliases: ['テトラ'] },
    ],
  },
  {
    works: [{ type: 'novel', id: 23, title: '不迷途的羔羊' }],
    people: [
      { id: 'narita-shinichiro', name: '成田真一郎' },
      { id: 'senba-aki', name: '仙波明希' },
    ],
  },
  {
    works: [{ type: 'novel', id: 24, title: '请别来管我 不知为何她想改变孤狼这没救的高中生活' }],
    people: [
      { id: 'nanamura-hotaka', name: '七村穗高', aliases: ['七村穂高'] },
      { id: 'hanamitsuji-sora', name: '花见辻空', aliases: ['花見辻空'] },
    ],
  },
  {
    works: [{ type: 'novel', id: 28, title: '绝对不愿工作的地下城主想要睡懒觉' }],
    people: [
      { id: 'masuda-keima', name: '桂马', aliases: ['ケーマ', '增田桂马'] },
      { id: 'rokuko', name: '洛库可', aliases: ['罗库可', 'ロクコ'] },
    ],
  },
  {
    works: [{ type: 'novel', id: 29, title: '黑鳶的聖者' }],
    people: [
      { id: 'russell-blackkite', name: '罗素', aliases: ['羅素', 'ラセル'] },
      { id: 'sibylla-blackkite', name: '希碧拉', aliases: ['シビラ', '西比拉'] },
    ],
  },
  {
    works: [{ type: 'novel', id: 30, title: '我要成为魔法少女！' }],
    people: [
      { id: 'nanashino-hiiro', name: '七篠阳彩', aliases: ['七篠陽彩', 'ななしのひいろ'] },
      { id: 'haku-magical', name: '白', aliases: ['ハク'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 14, title: '谭雅战记' }],
    people: [
      { id: 'tanya-degurechaff', name: '谭雅·冯·提古雷查夫', aliases: ['ターニャ', 'ターニャ・フォン・デグレチャフ'] },
      { id: 'mary-sioux', name: '玛丽·苏', aliases: ['メアリー・スー'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 19, title: '天才王子的赤字国家振兴术' }],
    people: [
      { id: 'wein-natra', name: '维恩', aliases: ['ウェイン', '维恩·萨雷玛·阿尔巴斯特'] },
      { id: 'ninym-ralei', name: '妮妮姆', aliases: ['ニニム', '妮妮姆·拉雷'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 56, title: '三流反派的學院生存記' }],
    people: [
      { id: 'ed-rothstaylor', name: '艾德·罗斯泰勒', aliases: ['Ed Rothstaylor', 'Ed Rothtaylor'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 58, title: '渴望復仇的最強勇者、以黑暗之力所向披靡' }],
    people: [
      { id: 'raoul-evans', name: '劳尔·埃文斯', aliases: ['ラウル・エヴァンス', 'Raoul Evans'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 71, title: '反派絕殺' }],
    people: [
      { id: 'villain-cassian', name: '卡西安', aliases: ['Cassian'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 78, title: '裝備我最強' }],
    people: [
      { id: 'shin-youngwoo', name: '申英宇', aliases: ['Shin Youngwoo', 'Grid', '格里德'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 7, title: '想尽情享受的常客小姐' }],
    people: [
      { id: 'mankitsu-izumi', name: '泉' },
      { id: 'mankitsu-honami', name: '穗波', aliases: ['ほなみ'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 8, title: '陷入爱河的蜂谷君是女仆' }],
    people: [
      { id: 'hachiyaa-maid', name: '蜂谷', aliases: ['蜂谷くん'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 13, title: '帮助了迷路的幼女后，美少女留学生邻居变得常来我家玩了' }],
    people: [
      { id: 'aoyagi-akito', name: '青柳明人' },
      { id: 'charlotte-bennett', name: '夏洛特·贝内特', aliases: ['シャーロット・ベネット', 'Charlotte Bennett'] },
      { id: 'emma-bennett', name: '艾玛·贝内特', aliases: ['エマ・ベネット', 'Emma Bennett'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 42, title: '一覺醒來坐擁神裝和飛船，我決定以買一套獨門獨戶的房子為目標作為傭兵自由地活下去' }],
    people: [
      { id: 'space-hiro', name: '希罗', aliases: ['ヒロ', '佐藤孝弘'] },
      { id: 'space-mimi', name: '咪咪', aliases: ['ミミ', 'Mimi'] },
      { id: 'space-elma', name: '艾尔玛', aliases: ['エルマ', 'Elma'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 52, title: '妹妹是知道的' }],
    people: [
      { id: 'miki-kiichiro', name: '三木贵一郎', aliases: ['三木貴一郎'] },
      { id: 'miki-miki', name: '三木美贵', aliases: ['三木美貴'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 66, title: '秋江入夜時' }],
    people: [
      { id: 'autumn-river-rok', name: '李禄', aliases: ['李祿'] },
      { id: 'autumn-river-sosa', name: '素纱', aliases: ['素紗'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 77, title: '玩物' }],
    people: [
      { id: 'yoon-garam-her-toys', name: '尹佳蓝', aliases: ['尹佳藍'] },
      { id: 'lee-myeongjun-her-toys', name: '李明俊' },
    ],
  },
  {
    works: [{ type: 'comic', id: 28, title: '妹妹一天只和我对上一次眼' }],
    people: [
      { id: 'eye-contact-sister', name: '妹妹', aliases: ['妹'] },
      { id: 'eye-contact-brother', name: '哥哥', aliases: ['兄'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 40, title: '海邊之夜' }],
    people: [
      { id: 'low-tide-euihyun', name: '金宜贤', aliases: ['김의현', 'Kim Euihyun'] },
      { id: 'low-tide-taeju', name: '吕泰洲', aliases: ['여태주', 'Yeo Taeju'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 44, title: '後宮開在離婚時' }],
    people: [
      { id: 'wako-ichiro', name: '和光市路', aliases: ['市路'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 73, title: '身為路人司祭的我發現這是乙女遊戲的世界於是決定養成女主' }],
    people: [
      { id: 'mob-priest-krael', name: '克拉艾尔·巴恩', aliases: ['クラエル・バーン', '克拉艾尔'] },
      { id: 'mob-priest-reina', name: '蕾娜', aliases: ['レイナ'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 75, title: '隱語島' }],
    people: [
      { id: 'ingoshima-keita', name: '东堂启太', aliases: ['東堂啓太', '啓太'] },
      { id: 'ingoshima-aoi', name: '宫原葵', aliases: ['宮原葵', '葵'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 79, title: '劍與魔法與NTR' }],
    people: [
      { id: 'otodori-ayumu', name: '乙鸟步', aliases: ['乙鳥歩'] },
    ],
  },
  {
    works: [{ type: 'comic', id: 74, title: '戰姬求生記-背負異世界命運的不正經異性交流' }],
    people: [{ id: 'survival-saga-anastasia', name: '阿纳斯塔西娅', aliases: ['アナスタシア', 'Anastasia'] }],
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
