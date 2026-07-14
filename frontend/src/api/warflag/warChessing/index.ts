import request from '@/utils/request'

enum API {
    GET_CHESSING_URL = "/chessing/getflag",
    GET_CHESSING_RANGE_URL = '/chessing/getflagrange',
    MOVE_CHESSING_URL = '/chessing/moveFlag',  
    GET_CHESSING_ATTACKRANGE_URL = '/chessing/attackrange',
    CHESSING_ATTACK_URL = '/chessing/attack',
    BATTLE_STATE_URL = '/battle/state',
    BATTLE_END_TURN_URL = '/battle/endTurn',
    BATTLE_INIT_URL = '/battle/init',
    BATTLE_RESET_URL = '/battle/reset',
    BATTLE_AI_TURN_URL = '/battle/aiTurn'
}

export const getWarChessing = (data: any) => 
    request.get<any, any>(API.GET_CHESSING_URL + `?id=${data}`);

export const getWarChessingRange = (data: any, range: number) => 
    request.get<any, any>(API.GET_CHESSING_RANGE_URL + `?id=${data}&range=${range}`);

export const moveChess = (data: any) => 
    request.post<any, any>(API.MOVE_CHESSING_URL, data);

// 统一的箭头函数风格
export const getWarChessingAttackRange = (chessId: string, battleId: number) => 
    request.get<any, any>(API.GET_CHESSING_ATTACKRANGE_URL + `?chessId=${chessId}&battleId=${battleId}`);

export const attackChess = (data: any) => 
    request.post<any, any>(API.CHESSING_ATTACK_URL, data);

// ========== 回合制 ==========
export const getBattleState = (battleId: number) =>
    request.get<any, any>(API.BATTLE_STATE_URL + `?battleId=${battleId}`);

export const endBattleTurn = (battleId: number) =>
    request.post<any, any>(API.BATTLE_END_TURN_URL + `?battleId=${battleId}`);

/** 智能初始化：上一局已结束/无数据时从备份还原（还原后 humanPlayer 先手）；ONGOING 中保留 */
export const initBattle = (battleId: number, humanPlayer: number = 0) =>
    request.post<any, any>(
        API.BATTLE_INIT_URL + `?battleId=${battleId}&humanPlayer=${humanPlayer}`
    );

/** 强制从备份重置；还原后由 humanPlayer 先手 */
export const resetBattle = (battleId: number, humanPlayer: number = 0) =>
    request.post<any, any>(
        API.BATTLE_RESET_URL + `?battleId=${battleId}&humanPlayer=${humanPlayer}`
    );

/** AI 操作当前非 humanPlayer 的回合（优先摧毁 humanPlayer 的指挥部），结束后切回人类玩家 */
export const runEnemyAiTurn = (battleId: number, humanPlayer: number = 0) =>
    request.post<any, any>(
        API.BATTLE_AI_TURN_URL + `?battleId=${battleId}&humanPlayer=${humanPlayer}`
    );
