-- ============================================================
-- 战棋备份功能 DDL + 初始快照
-- 1. 创建棋子备份表 tb_chessing_backup（结构与 tb_chessing 完全一致）
-- 2. 创建回合状态备份表 tb_battle_state_backup（结构与 tb_battle_state 完全一致）
-- 3. 把当前 live 数据"洗白"后写入备份：hp=maxhp、moved=false、fired=false
-- 4. 写入 1001 局的初始回合快照（player=0、round=1、ONGOING）
-- 这之后 BattleStateService.resetBattle 调用 INSERT ... SELECT * FROM ..._backup 即可一键还原。
-- ============================================================

BEGIN;

-- ---------- 1. tb_chessing_backup ----------
DROP TABLE IF EXISTS acgn0721_gis.tb_chessing_backup;
CREATE TABLE acgn0721_gis.tb_chessing_backup (
    id         varchar      NOT NULL,
    battleid   integer      NOT NULL,
    hexid      integer,
    q          integer,
    r          integer,
    type       varchar,
    player     integer,
    hp         integer,
    maxhp      integer,
    move       integer,
    atk        integer,
    range      integer,
    moved      boolean,
    fired      boolean,
    createdat  timestamp without time zone,
    updatedat  timestamp without time zone,
    chessname  varchar,
    CONSTRAINT pk_tb_chessing_backup PRIMARY KEY (battleid, id)
);

-- 把当前 live 数据"洗白"后落到备份表：
--   hp 拉满到 maxhp、moved/fired 清零、updatedat 用当前时间
-- 之后 INSERT ... SELECT * FROM tb_chessing_backup 还原时就是一份"干净的开局"。
INSERT INTO acgn0721_gis.tb_chessing_backup
    (id, battleid, hexid, q, r, type, player,
     hp, maxhp, move, atk, range,
     moved, fired, createdat, updatedat, chessname)
SELECT
    id,
    battleid,
    hexid,
    q,
    r,
    type,
    player,
    maxhp                AS hp,    -- 关键：hp 直接拉满
    maxhp,
    move,
    atk,
    range,
    FALSE                AS moved, -- 关键：清空回合内状态
    FALSE                AS fired,
    COALESCE(createdat, NOW()),
    NOW()                AS updatedat,
    chessname
FROM acgn0721_gis.tb_chessing
WHERE battleid = 1001;


-- ---------- 2. tb_battle_state_backup ----------
DROP TABLE IF EXISTS acgn0721_gis.tb_battle_state_backup;
CREATE TABLE acgn0721_gis.tb_battle_state_backup (
    battle_id      bigint    NOT NULL,
    current_player smallint  NOT NULL DEFAULT 0,
    round_no       integer   NOT NULL DEFAULT 1,
    status         varchar   NOT NULL DEFAULT 'ONGOING',
    winner         smallint,
    created_at     timestamp without time zone,
    updated_at     timestamp without time zone,
    CONSTRAINT pk_tb_battle_state_backup PRIMARY KEY (battle_id)
);

-- 写入 1001 局的初始回合状态：我方先手、第 1 回合、进行中、无胜方
INSERT INTO acgn0721_gis.tb_battle_state_backup
    (battle_id, current_player, round_no, status, winner, created_at, updated_at)
VALUES
    (1001, 0, 1, 'ONGOING', NULL, NOW(), NOW());


COMMIT;

-- ============================================================
-- 自检（执行完后人肉 / 程序均可对比）
-- ============================================================
-- SELECT COUNT(*) FROM acgn0721_gis.tb_chessing_backup     WHERE battleid = 1001;
-- SELECT *        FROM acgn0721_gis.tb_battle_state_backup WHERE battle_id = 1001;
