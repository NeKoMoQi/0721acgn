package com.example.demo.service.impl;

import com.example.demo.com.BattleState;
import com.example.demo.com.Chessing;
import com.example.demo.mapper.postgresql.BattleStateMapper;
import com.example.demo.mapper.postgresql.WarChessingMapper;
import com.example.demo.service.BattleStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BattleStateServiceImpl implements BattleStateService {

    private final BattleStateMapper battleStateMapper;
    private final WarChessingMapper warChessingMapper;

    public BattleStateServiceImpl(BattleStateMapper battleStateMapper,
                                  WarChessingMapper warChessingMapper) {
        this.battleStateMapper = battleStateMapper;
        this.warChessingMapper = warChessingMapper;
    }

    @Override
    @Transactional
    public BattleState getOrInit(Long battleId) {
        BattleState state = battleStateMapper.selectByBattleId(battleId);
        if (state != null) {
            return state;
        }
        BattleState init = new BattleState();
        init.setBattleId(battleId);
        init.setCurrentPlayer((short) 0);
        init.setRoundNo(1);
        init.setStatus(BattleState.STATUS_ONGOING);
        battleStateMapper.insertInitial(init);
        return battleStateMapper.selectByBattleId(battleId);
    }

    @Override
    @Transactional
    public BattleState endTurn(Long battleId) {
        BattleState state = getOrInit(battleId);
        if (!BattleState.STATUS_ONGOING.equals(state.getStatus())) {
            return state;
        }

        Short curr = state.getCurrentPlayer() == null ? 0 : state.getCurrentPlayer();
        Short next = (short) ((curr == 0) ? 1 : 0);

        // 把"下一个回合方"的所有存活棋子 moved/fired 复位（让他们能行动）
        warChessingMapper.resetTurnFlags(battleId, next);

        // 回到 player=0 算一个完整回合
        int nextRound = state.getRoundNo() == null ? 1 : state.getRoundNo();
        if (next == 0) {
            nextRound += 1;
        }

        battleStateMapper.switchTurn(battleId, next, nextRound);

        BattleState refreshed = battleStateMapper.selectByBattleId(battleId);
        return evaluateVictory(refreshed.getBattleId());
    }

    @Override
    @Transactional
    public BattleState evaluateVictory(Long battleId) {
        BattleState state = battleStateMapper.selectByBattleId(battleId);
        if (state == null) {
            return getOrInit(battleId);
        }
        if (!BattleState.STATUS_ONGOING.equals(state.getStatus())) {
            return state;
        }

        List<Chessing> all = warChessingMapper.getWarChessing(Math.toIntExact(battleId));
        boolean p0Alive = sideStillInFight(all, battleId, (short) 0);
        boolean p1Alive = sideStillInFight(all, battleId, (short) 1);

        String newStatus = null;
        Short winner = null;
        if (!p0Alive && !p1Alive) {
            newStatus = BattleState.STATUS_DRAW;
        } else if (!p0Alive) {
            newStatus = BattleState.STATUS_WIN_P1;
            winner = (short) 1;
        } else if (!p1Alive) {
            newStatus = BattleState.STATUS_WIN_P0;
            winner = (short) 0;
        }

        if (newStatus != null) {
            battleStateMapper.updateStatus(battleId, newStatus, winner);
            state.setStatus(newStatus);
            state.setWinner(winner);
        }
        return state;
    }

    @Override
    public boolean canActAs(Long battleId, Short player) {
        if (battleId == null || player == null) return false;
        BattleState state = getOrInit(battleId);
        if (!BattleState.STATUS_ONGOING.equals(state.getStatus())) return false;
        return player.equals(state.getCurrentPlayer());
    }

    /**
     * 智能初始化：上局已结束 / live 无数据 → 从备份还原；ONGOING 中保留现状。
     * 从备份还原后，由 {@code humanPlayer} 方先手（与玩家所选阵营一致）。
     */
    @Override
    @Transactional
    public BattleState initOrRestore(Long battleId, Short humanPlayer) {
        BattleState state = battleStateMapper.selectByBattleId(battleId);
        int aliveTotal = warChessingMapper.countAlive(battleId, (short) 0)
                + warChessingMapper.countAlive(battleId, (short) 1);

        boolean shouldRestore = state == null
                || !BattleState.STATUS_ONGOING.equals(state.getStatus())
                || aliveTotal == 0;

        if (!shouldRestore) {
            return state;
        }
        BattleState restored = doRestoreRaw(battleId);
        if (restored == null) {
            getOrInit(battleId);
            return finalizeHumanFirstTurn(battleId, humanPlayer);
        }
        return finalizeHumanFirstTurn(battleId, humanPlayer);
    }

    /**
     * 硬重置：从 backup 还原后，由 {@code humanPlayer} 方先手。
     */
    @Override
    @Transactional
    public BattleState resetBattle(Long battleId, Short humanPlayer) {
        BattleState restored = doRestoreRaw(battleId);
        if (restored == null) {
            getOrInit(battleId);
            return finalizeHumanFirstTurn(battleId, humanPlayer);
        }
        return finalizeHumanFirstTurn(battleId, humanPlayer);
    }

    /** 清掉 live → 从 backup 拷回（不改正回合方）。 */
    private BattleState doRestoreRaw(Long battleId) {
        warChessingMapper.deleteByBattleId(battleId);
        battleStateMapper.deleteByBattleId(battleId);
        warChessingMapper.restoreFromBackup(battleId);
        battleStateMapper.restoreFromBackup(battleId);
        return battleStateMapper.selectByBattleId(battleId);
    }

    private static short normalizeHuman(Short humanPlayer) {
        return humanPlayer == null ? 0 : humanPlayer.shortValue();
    }

    private void resetAllPieceTurnFlags(Long battleId) {
        warChessingMapper.resetTurnFlags(battleId, (short) 0);
        warChessingMapper.resetTurnFlags(battleId, (short) 1);
    }

    /** 第 1 回合、ONGOING、由人类所选阵营先手；并清空双方本回合 moved/fired。 */
    private BattleState finalizeHumanFirstTurn(Long battleId, Short humanPlayer) {
        short human = normalizeHuman(humanPlayer);
        battleStateMapper.setHumanFirstTurn(battleId, human);
        resetAllPieceTurnFlags(battleId);
        return evaluateVictory(battleId);
    }

    /**
     * 该阵营是否仍在战斗中：
     * 若场上存在该阵营的「指挥部」棋子（type 为图鉴名或 10/11），则仅看指挥部 hp>0；
     * 否则回退为「任意单位存活」（兼容旧关卡没有指挥部时）。
     */
    private boolean sideStillInFight(List<Chessing> all, Long battleId, short player) {
        boolean hasHq = all.stream()
                .anyMatch(c -> playerEquals(c.getPlayer(), player) && isHeadquartersType(c.getType()));
        if (hasHq) {
            return all.stream()
                    .filter(c -> playerEquals(c.getPlayer(), player) && isHeadquartersType(c.getType()))
                    .anyMatch(c -> c.getHp() != null && c.getHp() > 0);
        }
        return warChessingMapper.countAlive(battleId, player) > 0;
    }

    private static boolean playerEquals(Short p, short expected) {
        return p != null && p.shortValue() == expected;
    }

    /** 与 WarChessingMapper 里 COALESCE(ct.name, c.type) 及裸 type 列一致 */
    private static boolean isHeadquartersType(String type) {
        if (type == null) return false;
        if ("10".equals(type) || "11".equals(type)) return true;
        return "GermanCommand".equals(type)
                || "sovcommand".equalsIgnoreCase(type);
    }
}
