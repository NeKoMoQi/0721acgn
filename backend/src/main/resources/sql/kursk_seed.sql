-- ============================================================
-- 库尔斯克战役（Kursk, 1943）棋子初始化脚本
--   battleid = 1001
--   player=0 苏军（防御方）
--   player=1 德军（进攻方，南北钳形攻势）
-- 设计：
--   - 苏军布在 q ∈ [5..16]，三层纵深：步兵 / 反坦克 + T-34 / 火炮 + 司令部
--   - 德军分两路：北路 r<0，南路 r>0；q ∈ [-9..-1]
-- 已有的 3 个棋子（第70集团军 / 第2装甲师 / 第4装甲师）保留
--
-- 注意：本脚本不依赖任何 UNIQUE/PK 约束，使用 WHERE NOT EXISTS 做幂等
-- 重复执行不会插重复，删完再插也没问题
-- ============================================================

-- ---------- 1. 兵种字典：补 4 种 ----------
-- 注意：图片 URL 都先指向已有的 tank.jpg，等你上传真图后改 pic_url 即可
-- 不需要改任何前端/后端代码
INSERT INTO acgn0721_gis.tb_chessing_pic (id, name, pic_url)
SELECT v.id::varchar, v.name, v.pic_url
FROM (VALUES
    ('2', 'tiger',     'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/chessing/tank.jpg'),
    ('3', 'infantry',  'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/chessing/tank.jpg'),
    ('4', 'artillery', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/chessing/tank.jpg'),
    ('5', 'anti_tank', 'https://acgn0721.oss-cn-hangzhou.aliyuncs.com/chessing/tank.jpg')
) AS v(id, name, pic_url)
WHERE NOT EXISTS (
    SELECT 1
    FROM acgn0721_gis.tb_chessing_pic t
    WHERE t.id::varchar = v.id::varchar
);

-- ---------- 2. 苏军（player=0）15 个 ----------
-- 兵种属性参考：
--   步兵    type=3  hp=80  atk=25 range=1 move=3
--   T-34    type=1  hp=120 atk=50 range=1 move=5
--   反坦克炮 type=5  hp=70  atk=60 range=2 move=2
--   火炮    type=4  hp=60  atk=80 range=4 move=2
INSERT INTO acgn0721_gis.tb_chessing
    (id, battleid, chessname, q, r, type, player, hp, maxhp, "move", atk, "range", moved, fired, createdat, updatedat)
SELECT v.id::varchar, v.battleid::bigint, v.chessname, v.q, v.r, v.type::varchar, v.player::smallint,
       v.hp, v.maxhp, v.move, v.atk, v.range, v.moved, v.fired, NOW(), NOW()
FROM (VALUES
    -- 一线步兵 (4)
    ('sov-001', 1001, '第13集团军-步兵',     8, -2, '3', 0,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('sov-002', 1001, '第48步兵军',          7,  0, '3', 0,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('sov-003', 1001, '第15步兵师',          6,  2, '3', 0,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('sov-004', 1001, '第8近卫师',           5,  4, '3', 0,  80,  80, 3, 25, 1, FALSE, FALSE),
    -- 反坦克线 (3)
    ('sov-005', 1001, '第3反坦克旅',         9, -1, '5', 0,  70,  70, 2, 60, 2, FALSE, FALSE),
    ('sov-006', 1001, '第5反坦克团',         8,  1, '5', 0,  70,  70, 2, 60, 2, FALSE, FALSE),
    ('sov-007', 1001, '第14反坦克炮兵团',    7,  3, '5', 0,  70,  70, 2, 60, 2, FALSE, FALSE),
    -- T-34 装甲预备队 (4)
    ('sov-008', 1001, '第1坦克集团军',      11, -2, '1', 0, 120, 120, 5, 50, 1, FALSE, FALSE),
    ('sov-009', 1001, '第5近卫坦克军',      10,  0, '1', 0, 120, 120, 5, 50, 1, FALSE, FALSE),
    ('sov-010', 1001, '第2近卫坦克军',      11,  2, '1', 0, 120, 120, 5, 50, 1, FALSE, FALSE),
    ('sov-011', 1001, '第31坦克军',         10,  4, '1', 0, 120, 120, 5, 50, 1, FALSE, FALSE),
    -- 远程火炮 (3)
    ('sov-012', 1001, '第7近卫炮兵师',      14, -2, '4', 0,  60,  60, 2, 80, 4, FALSE, FALSE),
    ('sov-013', 1001, '第9炮兵师',          14,  1, '4', 0,  60,  60, 2, 80, 4, FALSE, FALSE),
    ('sov-014', 1001, '第32喀秋莎火箭炮团', 14,  4, '4', 0,  60,  60, 2, 80, 4, FALSE, FALSE),
    -- 指挥部 (1)
    ('sov-015', 1001, '沃罗涅日方面军司令部', 16, 0, '3', 0, 100, 100, 2, 20, 1, FALSE, FALSE)
) AS v(id, battleid, chessname, q, r, type, player, hp, maxhp, move, atk, range, moved, fired)
WHERE NOT EXISTS (
    SELECT 1
    FROM acgn0721_gis.tb_chessing t
    WHERE t.id::varchar = v.id::varchar
);

-- ---------- 3. 德军（player=1）15 个 ----------
-- 虎式：type=2  hp=180 atk=70 range=2 move=3 （慢、厚、远）
INSERT INTO acgn0721_gis.tb_chessing
    (id, battleid, chessname, q, r, type, player, hp, maxhp, "move", atk, "range", moved, fired, createdat, updatedat)
SELECT v.id::varchar, v.battleid::bigint, v.chessname, v.q, v.r, v.type::varchar, v.player::smallint,
       v.hp, v.maxhp, v.move, v.atk, v.range, v.moved, v.fired, NOW(), NOW()
FROM (VALUES
    -- 北路（r<0）：中央集团军群
    ('ger-001', 1001, '第9集团军-步兵',      -1, -4, '3', 1,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('ger-002', 1001, '第86步兵师',          -3, -2, '3', 1,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('ger-003', 1001, '第6步兵师',           -5,  0, '3', 1,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('ger-004', 1001, '第505重坦克营(虎式)', -2, -3, '2', 1, 180, 180, 3, 70, 2, FALSE, FALSE),
    ('ger-005', 1001, '第78突击师(虎式)',    -4, -1, '2', 1, 180, 180, 3, 70, 2, FALSE, FALSE),
    ('ger-006', 1001, '第18装甲师',          -3, -3, '1', 1, 120, 120, 5, 50, 1, FALSE, FALSE),
    ('ger-007', 1001, '第654重炮兵团',       -7, -2, '4', 1,  60,  60, 2, 80, 4, FALSE, FALSE),
    -- 南路（r>0）：南方集团军群
    ('ger-008', 1001, '第167步兵师',         -1,  4, '3', 1,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('ger-009', 1001, '第106步兵师',         -3,  5, '3', 1,  80,  80, 3, 25, 1, FALSE, FALSE),
    ('ger-010', 1001, '党卫军第2师(虎式)',   -2,  4, '2', 1, 180, 180, 3, 70, 2, FALSE, FALSE),
    ('ger-011', 1001, '党卫军第3师(虎式)',   -4,  5, '2', 1, 180, 180, 3, 70, 2, FALSE, FALSE),
    ('ger-012', 1001, '第7装甲师',           -3,  3, '1', 1, 120, 120, 5, 50, 1, FALSE, FALSE),
    ('ger-013', 1001, '第911突击炮营',       -5,  4, '5', 1,  70,  70, 2, 60, 2, FALSE, FALSE),
    ('ger-014', 1001, '第88高炮团',          -7,  4, '4', 1,  60,  60, 2, 80, 4, FALSE, FALSE),
    -- 集群指挥部
    ('ger-015', 1001, '中央集团军群指挥部',  -9,  0, '3', 1, 100, 100, 2, 20, 1, FALSE, FALSE)
) AS v(id, battleid, chessname, q, r, type, player, hp, maxhp, move, atk, range, moved, fired)
WHERE NOT EXISTS (
    SELECT 1
    FROM acgn0721_gis.tb_chessing t
    WHERE t.id::varchar = v.id::varchar
);

-- ---------- 4. 校验：看看落地了多少 ----------
SELECT player,
       COUNT(*)          AS total,
       SUM(CASE WHEN type='1' THEN 1 ELSE 0 END) AS tank,
       SUM(CASE WHEN type='2' THEN 1 ELSE 0 END) AS tiger,
       SUM(CASE WHEN type='3' THEN 1 ELSE 0 END) AS infantry,
       SUM(CASE WHEN type='4' THEN 1 ELSE 0 END) AS artillery,
       SUM(CASE WHEN type='5' THEN 1 ELSE 0 END) AS anti_tank
FROM acgn0721_gis.tb_chessing
WHERE battleid = 1001
GROUP BY player
ORDER BY player;

-- ============================================================
-- 想推倒重来？取消下面这一段的注释再执行：
-- DELETE FROM acgn0721_gis.tb_chessing
-- WHERE battleid = 1001 AND id LIKE 'sov-%';
-- DELETE FROM acgn0721_gis.tb_chessing
-- WHERE battleid = 1001 AND id LIKE 'ger-%';
-- ============================================================
