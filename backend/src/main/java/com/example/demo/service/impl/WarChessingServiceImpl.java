package com.example.demo.service.impl;

import com.example.demo.com.AttackResponse;
import com.example.demo.com.AttackTargetDTO;
import com.example.demo.com.Chessing;
import com.example.demo.mapper.postgresql.WarChessingMapper;
import com.example.demo.service.BattleStateService;
import com.example.demo.service.WarChessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WarChessingServiceImpl implements WarChessingService {
    private final WarChessingMapper warchessingmapper;
    private final BattleStateService battleStateService;

    public WarChessingServiceImpl(WarChessingMapper warchessingmapper,
                                  BattleStateService battleStateService) {
        this.warchessingmapper = warchessingmapper;
        this.battleStateService = battleStateService;
    }

    @Override
    public List<Chessing> getWarChessing(int id) {
        return warchessingmapper.getWarChessing(id);
    }

    @Override
    public List<AttackTargetDTO> getAttackableTargets(String chessId, Long battleId) {
        if (chessId == null || battleId == null) {
            return Collections.emptyList();
        }

        List<Chessing> allChess = warchessingmapper.getWarChessing(Math.toIntExact(battleId));
        Chessing attacker = allChess.stream()
                .filter(chess -> chessId.equals(chess.getId()))
                .findFirst()
                .orElse(null);

        if (attacker == null
                || attacker.getQ() == null
                || attacker.getR() == null
                || attacker.getRange() == null
                || attacker.getRange() < 1
                || Boolean.TRUE.equals(attacker.getFired())
                || attacker.getHp() == null
                || attacker.getHp() <= 0) {
            return Collections.emptyList();
        }

        return allChess.stream()
                .filter(target -> !chessId.equals(target.getId()))
                .filter(target -> target.getQ() != null && target.getR() != null)
                .filter(target -> target.getHp() != null && target.getHp() > 0)
                .filter(target -> !Objects.equals(attacker.getPlayer(), target.getPlayer()))
                .map(target -> toAttackTargetDTO(attacker, target))
                .filter(target -> target.getDistance() != null && target.getDistance() <= attacker.getRange())
                .sorted(Comparator.comparing(AttackTargetDTO::getDistance).thenComparing(AttackTargetDTO::getChessId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttackResponse attack(String attackerId, String targetId, Long battleId) {
        AttackResponse response = new AttackResponse();

        if (attackerId == null || targetId == null || battleId == null) {
            return failure(response, 0, "参数不完整");
        }
        if (attackerId.equals(targetId)) {
            return failure(response, 0, "不能攻击自己");
        }

        Chessing attacker = warchessingmapper.selectById(attackerId);
        Chessing target = warchessingmapper.selectById(targetId);

        if (attacker == null || target == null) {
            return failure(response, 0, "棋子不存在");
        }
        if (!battleId.equals(attacker.getBattleId()) || !battleId.equals(target.getBattleId())) {
            return failure(response, target.getHp() == null ? 0 : target.getHp(), "战斗ID不匹配");
        }
        if (!battleStateService.canActAs(battleId, attacker.getPlayer())) {
            return failure(response, target.getHp() == null ? 0 : target.getHp(), "现在不是你的回合");
        }
        if (Objects.equals(attacker.getPlayer(), target.getPlayer())) {
            return failure(response, target.getHp() == null ? 0 : target.getHp(), "不能攻击友军");
        }
        if (attacker.getHp() == null || attacker.getHp() <= 0) {
            return failure(response, target.getHp() == null ? 0 : target.getHp(), "攻击者已被击败");
        }
        if (Boolean.TRUE.equals(attacker.getFired())) {
            return failure(response, target.getHp() == null ? 0 : target.getHp(), "本回合已开火");
        }
        if (target.getHp() == null || target.getHp() <= 0) {
            return failure(response, 0, "目标已被击败");
        }
        if (attacker.getRange() == null || attacker.getRange() < 1) {
            return failure(response, target.getHp(), "攻击者无攻击距离");
        }
        if (attacker.getQ() == null || attacker.getR() == null
                || target.getQ() == null || target.getR() == null) {
            return failure(response, target.getHp(), "棋子坐标缺失");
        }

        double distance = calculateHexDistance(
                attacker.getQ(), attacker.getR(),
                target.getQ(), target.getR()
        );
        if (distance > attacker.getRange()) {
            return failure(response, target.getHp(), "目标超出攻击范围");
        }

        int damage = attacker.getAtk() == null ? 0 : Math.max(0, attacker.getAtk());
        int newHp = Math.max(0, target.getHp() - damage);
        boolean destroyed = newHp <= 0;

        warchessingmapper.updateHp(targetId, newHp);
        warchessingmapper.updateFired(attackerId, Boolean.TRUE);

        // ---- 反击 ----
        // 触发条件：目标存活 + 目标射程能覆盖攻击者 + 目标有攻击力
        boolean counterAttacked = false;
        int counterDamage = 0;
        int attackerNewHp = attacker.getHp();
        boolean attackerDestroyed = false;

        if (!destroyed
                && target.getRange() != null && target.getRange() >= 1
                && distance <= target.getRange()
                && target.getAtk() != null && target.getAtk() > 0) {
            counterAttacked = true;
            counterDamage = Math.max(0, target.getAtk());
            attackerNewHp = Math.max(0, attacker.getHp() - counterDamage);
            attackerDestroyed = attackerNewHp <= 0;

            warchessingmapper.updateHp(attackerId, attackerNewHp);
            // 反击是"反应"，不消耗目标的开火状态
        }

        if (destroyed || attackerDestroyed) {
            battleStateService.evaluateVictory(battleId);
        }

        response.setSuccess(Boolean.TRUE);
        response.setDamage(damage);
        response.setTargetNewHp(newHp);
        response.setTargetDestroyed(destroyed);
        response.setCounterAttacked(counterAttacked);
        response.setCounterDamage(counterDamage);
        response.setAttackerNewHp(attackerNewHp);
        response.setAttackerDestroyed(attackerDestroyed);

        StringBuilder msg = new StringBuilder();
        msg.append(destroyed ? "目标已被击败" : "造成 " + damage + " 点伤害");
        if (counterAttacked) {
            msg.append(attackerDestroyed
                    ? "，被反击 " + counterDamage + " 点，自身被击败"
                    : "，被反击 " + counterDamage + " 点");
        }
        response.setMessage(msg.toString());
        return response;
    }

    private AttackResponse failure(AttackResponse response, Integer targetCurrentHp, String message) {
        response.setSuccess(Boolean.FALSE);
        response.setDamage(0);
        response.setTargetNewHp(targetCurrentHp == null ? 0 : targetCurrentHp);
        response.setTargetDestroyed(Boolean.FALSE);
        response.setCounterAttacked(Boolean.FALSE);
        response.setCounterDamage(0);
        response.setAttackerNewHp(0);
        response.setAttackerDestroyed(Boolean.FALSE);
        response.setMessage(message);
        return response;
    }

    private AttackTargetDTO toAttackTargetDTO(Chessing attacker, Chessing target) {
        AttackTargetDTO dto = new AttackTargetDTO();
        dto.setChessId(target.getId());
        dto.setChessname(target.getChessname());
        dto.setHp(target.getHp());
        dto.setMaxHp(target.getMaxHp());
        dto.setCenterLng(target.getCenterLng());
        dto.setCenterLat(target.getCenterLat());
        dto.setDistance(calculateHexDistance(attacker.getQ(), attacker.getR(), target.getQ(), target.getR()));
        return dto;
    }

    private double calculateHexDistance(int q1, int r1, int q2, int r2) {
        int dq = q1 - q2;
        int dr = r1 - r2;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2.0;
    }
}
