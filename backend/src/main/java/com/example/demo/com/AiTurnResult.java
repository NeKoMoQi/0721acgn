package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 敌方 AI 跑完一回合后的结果。
 *  - executed = false：本次根本没轮到 AI 行动（比如当前还是我方回合 / 战斗已结束）
 *  - actions  ：AI 这一回合做过的所有动作，按顺序
 *  - state    ：AI 行动 + 自动 endTurn 之后的最终回合状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTurnResult {
    private Boolean        executed;
    private String         message;
    private BattleState    state;
    private List<AiAction> actions;
}
