package com.example.demo.service;

import com.example.demo.com.AiTurnResult;

public interface EnemyAiService {

    /**
     * 执行「非 humanPlayer 阵营」的完整 AI 回合。
     * <ul>
     *   <li>仅当 status=ON 且 currentPlayer ≠ humanPlayer 时才会执子；</li>
     *   <li>优先消灭 humanPlayer 的指挥部，其余单位按原启发式收残 + 推进；</li>
     *   <li>结束后自动 endTurn，把回合切回 humanPlayer。</li>
     * </ul>
     *
     * @param battleId    战场
     * @param humanPlayer 人类玩家阵营：0=苏军，1=德军（须与库内 player 一致）
     */
    AiTurnResult runEnemyTurn(Long battleId, Short humanPlayer);
}
