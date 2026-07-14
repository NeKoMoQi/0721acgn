package com.example.demo.mapper.postgresql;

import com.example.demo.com.BattleState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BattleStateMapper {

    BattleState selectByBattleId(@Param("battleId") Long battleId);

    int insertInitial(BattleState state);

    int updateState(BattleState state);

    /** 切换回合：current_player 翻转、round_no 自增（仅当对方走完回合时） */
    int switchTurn(@Param("battleId") Long battleId,
                   @Param("nextPlayer") Short nextPlayer,
                   @Param("nextRound") Integer nextRound);

    int updateStatus(@Param("battleId") Long battleId,
                     @Param("status") String status,
                     @Param("winner") Short winner);

    /** 重置：清掉 live 表内某 battle 的回合状态 */
    int deleteByBattleId(@Param("battleId") Long battleId);

    /** 重置：从 backup 表把某 battle 的初始回合状态拷回 live 表 */
    int restoreFromBackup(@Param("battleId") Long battleId);

    /** 开局/还原后：强制由人类玩家先手（round 回到 1） */
    int setHumanFirstTurn(@Param("battleId") Long battleId,
                          @Param("humanPlayer") Short humanPlayer);
}
