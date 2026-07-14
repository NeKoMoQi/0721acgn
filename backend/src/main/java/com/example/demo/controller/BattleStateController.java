package com.example.demo.controller;

import com.example.demo.com.AiTurnResult;
import com.example.demo.com.BattleState;
import com.example.demo.com.Result;
import com.example.demo.service.BattleStateService;
import com.example.demo.service.EnemyAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class BattleStateController {

    @Autowired
    private BattleStateService battleStateService;

    @Autowired
    private EnemyAiService enemyAiService;

    /** 进入战场时拉取/初始化回合状态 */
    @GetMapping("/battle/state")
    public Result<BattleState> getState(@RequestParam Long battleId) {
        return Result.success(battleStateService.getOrInit(battleId));
    }

    /**
     * 结束当前方回合（仅切到对方并复位对方棋子本回合状态，不执行 AI）。
     * 前端在我方点击「结束回合」后，若返回 currentPlayer=1，应继续调用 {@code /battle/aiTurn}。
     */
    @PostMapping("/battle/endTurn")
    public Result<BattleState> endTurn(@RequestParam Long battleId) {
        return Result.success(battleStateService.endTurn(battleId));
    }

    /**
     * 进入战场页面时调用：
     *   - 上一局已结束 / live 无数据 → 从备份还原
     *   - 正在进行的局 → 保留现状
     * 适合页面 onMount 直接调一次再去拉棋子。
     */
    @PostMapping("/battle/init")
    public Result<BattleState> init(
            @RequestParam Long battleId,
            @RequestParam(required = false, defaultValue = "0") Short humanPlayer) {
        BattleState state = battleStateService.initOrRestore(battleId, humanPlayer);
        log.info("[battle/init] battleId={}, humanPlayer={}, status={}, currentPlayer={}, roundNo={}",
                battleId, humanPlayer, state.getStatus(), state.getCurrentPlayer(), state.getRoundNo());
        return Result.success(state);
    }

    /** 强制从备份重置；还原后由 humanPlayer 方先手（与玩家阵营一致） */
    @PostMapping("/battle/reset")
    public Result<BattleState> reset(
            @RequestParam Long battleId,
            @RequestParam(required = false, defaultValue = "0") Short humanPlayer) {
        BattleState state = battleStateService.resetBattle(battleId, humanPlayer);
        log.info("[battle/reset] battleId={}, humanPlayer={} 已从备份还原并设先手", battleId, humanPlayer);
        return Result.success(state);
    }

    /**
     * AI 完整回合：操作「非 humanPlayer」阵营；优先打击 humanPlayer 的指挥部；最后 endTurn 切回人类。
     *
     * @param humanPlayer 人类所选阵营：0=苏军，1=德军（默认 0，兼容旧前端）
     */
    @PostMapping("/battle/aiTurn")
    public Result<AiTurnResult> aiTurn(
            @RequestParam Long battleId,
            @RequestParam(required = false, defaultValue = "0") Short humanPlayer) {
        AiTurnResult result = enemyAiService.runEnemyTurn(battleId, humanPlayer);
        log.info("[battle/aiTurn] battleId={}, executed={}, actions={}, finalStatus={}",
                battleId,
                result.getExecuted(),
                result.getActions() == null ? 0 : result.getActions().size(),
                result.getState() == null ? null : result.getState().getStatus());
        return Result.success(result);
    }
}
