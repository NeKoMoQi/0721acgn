package com.example.demo.service;

import com.example.demo.com.AttackResponse;
import com.example.demo.com.AttackTargetDTO;
import com.example.demo.com.Chessing;

import java.util.List;

public interface WarChessingService {
    List<Chessing> getWarChessing(int id);

    List<AttackTargetDTO> getAttackableTargets(String chessId, Long battleId);

    /**
     * 执行攻击：校验范围/状态，扣除目标血量，标记攻击者已开火。
     */
    AttackResponse attack(String attackerId, String targetId, Long battleId);
}
