-- ============================================================
-- 模拟后端 BattleStateServiceImpl.doRestore 的执行顺序：
--   1. DELETE live 棋子
--   2. DELETE live 回合状态
--   3. INSERT live 棋子   ← FROM tb_chessing_backup
--   4. INSERT live 回合状态 ← FROM tb_battle_state_backup
-- 用于本地验证 reset 流程是否真的能把 live 还原到一份干净的开局。
-- ============================================================

BEGIN;

-- 1. 清理 live
DELETE FROM acgn0721_gis.tb_chessing
WHERE battleid = 1001;

DELETE FROM acgn0721_gis.tb_battle_state
WHERE battle_id = 1001;

-- 2. 从备份还原（与 WarChessingMapper.xml / BattleStateMapper.xml 完全一致）
INSERT INTO acgn0721_gis.tb_chessing
SELECT *
FROM acgn0721_gis.tb_chessing_backup
WHERE battleid = 1001;

INSERT INTO acgn0721_gis.tb_battle_state
SELECT *
FROM acgn0721_gis.tb_battle_state_backup
WHERE battle_id = 1001;

COMMIT;
