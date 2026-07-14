package com.example.demo.mapper.postgresql;
import com.example.demo.com.Chessing;
import com.example.demo.com.WarGrid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarChessingMapper {
    List<Chessing> getWarChessing(int id);
    // 新增：根据ID查询单个棋子
    Chessing selectById(@Param("id") String id);
    WarGrid selectGridByQAndR(@Param("q") Integer q, @Param("r") Integer r);
    // 新增：更新棋子
    int updateById(Chessing chessing);

    /** 攻击：更新目标血量 */
    int updateHp(@Param("id") String id, @Param("hp") Integer hp);

    /** 攻击：标记攻击者本回合已开火 */
    int updateFired(@Param("id") String id, @Param("fired") Boolean fired);

    /** 回合结束：把指定阵营的所有存活棋子 moved/fired 复位 */
    int resetTurnFlags(@Param("battleId") Long battleId,
                       @Param("player") Short player);

    /** 统计某阵营存活棋子数（hp > 0） */
    int countAlive(@Param("battleId") Long battleId,
                   @Param("player") Short player);

    /** 重置：清掉 live 表内某 battle 的所有棋子 */
    int deleteByBattleId(@Param("battleId") Long battleId);

    /** 重置：从 tb_chessing_backup 把某 battle 的初始棋子拷回 live 表 */
    int restoreFromBackup(@Param("battleId") Long battleId);
}
