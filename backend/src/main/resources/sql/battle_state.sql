-- ===========================================
-- 战旗：回合制状态表
-- 一个 battle_id 对应一行，记录当前回合方/回合数/胜负
-- ===========================================

CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_battle_state (
    battle_id      BIGINT       PRIMARY KEY,
    current_player SMALLINT     NOT NULL DEFAULT 0,   -- 0=己方 1=敌方
    round_no       INT          NOT NULL DEFAULT 1,   -- 回合数
    status         VARCHAR(16)  NOT NULL DEFAULT 'ONGOING',
    -- ONGOING / WIN_PLAYER0 / WIN_PLAYER1 / DRAW
    winner         SMALLINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 给 1001 这一局补一行初始状态（如果还没有）
INSERT INTO acgn0721_gis.tb_battle_state (battle_id, current_player, round_no, status)
VALUES (1001, 0, 1, 'ONGOING')
ON CONFLICT (battle_id) DO NOTHING;
