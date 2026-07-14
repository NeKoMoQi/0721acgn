-- ============================================================
-- 备份/初始模板表
--   用法：
--     1) 先跑这份 DDL（CREATE 部分）建出空备份表
--     2) 再跑下面"快照写入"那一段，把当前 1001 这一局的状态作为初始模板
--   以后如果想改初始局面：先在 tb_chessing 调成你想要的开局，
--   再重新执行下面"快照写入"段即可（不需要改任何代码）
-- ============================================================

-- ---------- 1. 创建备份表（结构与 live 表一致） ----------
CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_chessing_backup (
    LIKE acgn0721_gis.tb_chessing INCLUDING DEFAULTS
);

CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_battle_state_backup (
    LIKE acgn0721_gis.tb_battle_state INCLUDING DEFAULTS
);

-- ---------- 2. 快照写入：把当前 1001 局作为初始模板 ----------
-- 棋子模板：清空 backup 后从 live 拷一份（仅这一 battleid）
DELETE FROM acgn0721_gis.tb_chessing_backup
WHERE battleid = 1001;

INSERT INTO acgn0721_gis.tb_chessing_backup
SELECT *
FROM acgn0721_gis.tb_chessing
WHERE battleid = 1001;

-- 回合模板：永远是初始状态（player=0 我方先手，第 1 回合，进行中）
DELETE FROM acgn0721_gis.tb_battle_state_backup
WHERE battle_id = 1001;

INSERT INTO acgn0721_gis.tb_battle_state_backup
    (battle_id, current_player, round_no, status, winner, created_at, updated_at)
VALUES
    (1001, 0, 1, 'ONGOING', NULL, NOW(), NOW());

-- ---------- 3. 校验 ----------
SELECT 'chessing_backup' AS source, COUNT(*) AS rows
FROM acgn0721_gis.tb_chessing_backup
WHERE battleid = 1001
UNION ALL
SELECT 'battle_state_backup', COUNT(*)
FROM acgn0721_gis.tb_battle_state_backup
WHERE battle_id = 1001;
