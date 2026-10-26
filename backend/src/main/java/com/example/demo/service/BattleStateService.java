package com.example.demo.service;

import com.example.demo.com.BattleState;

public interface BattleStateService {

    /** 获取（不存在时自动初始化）一局对战的回合状态 */
    BattleState getOrInit(Long battleId);

    /** 结束当前方回合，切到对方；若对方已无棋子则直接判胜 */
    BattleState endTurn(Long battleId);

    /** 重新评估胜负：调用方在攻击/移动后可调用 */
    BattleState evaluateVictory(Long battleId);

    /** 校验：本次行动是否属于当前回合方 */
    boolean canActAs(Long battleId, Short player);

    /**
     * 智能初始化：上一局已结束 / live 没数据 → 从备份还原；
     * 正在进行的局 → 保留现状。返回当前回合状态。
     *
     * @param humanPlayer 仅在「从备份还原」时生效：0=苏军先手，1=德军先手（由人类扮演方先手）
     */
    BattleState initOrRestore(Long battleId, Short humanPlayer);

    /** 硬重置：始终从备份还原（用于"重新开始"按钮），还原后由 humanPlayer 方先手 */
    BattleState resetBattle(Long battleId, Short humanPlayer);
}
