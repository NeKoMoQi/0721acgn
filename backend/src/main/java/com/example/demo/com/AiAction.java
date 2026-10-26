package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敌方 AI 的一次动作日志，前端可以根据这个序列做动画回放。
 *  - type = "MOVE":   chess 从 (fromQ,fromR) 移到 (toQ,toR)
 *  - type = "ATTACK": attackerId 攻击 targetId，伤害=damage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAction {
    /** "MOVE" / "ATTACK" / "SKIP" */
    private String type;
    private String chessId;

    // ---- MOVE ----
    private Integer fromQ;
    private Integer fromR;
    private Integer toQ;
    private Integer toR;
    private Double  toLng;
    private Double  toLat;

    // ---- ATTACK ----
    private String  targetId;
    private Integer damage;
    private Integer targetNewHp;
    private Boolean targetDestroyed;
    private Boolean counterAttacked;
    private Integer counterDamage;
    private Integer attackerNewHp;
    private Boolean attackerDestroyed;

    /** 描述（日志、tooltip 用） */
    private String  message;

    public static AiAction move(String chessId,
                                int fromQ, int fromR,
                                int toQ, int toR,
                                Double toLng, Double toLat,
                                String message) {
        AiAction a = new AiAction();
        a.setType("MOVE");
        a.setChessId(chessId);
        a.setFromQ(fromQ);
        a.setFromR(fromR);
        a.setToQ(toQ);
        a.setToR(toR);
        a.setToLng(toLng);
        a.setToLat(toLat);
        a.setMessage(message);
        return a;
    }

    public static AiAction attack(String attackerId, String targetId,
                                  int damage, int targetNewHp, boolean targetDestroyed,
                                  boolean counterAttacked, int counterDamage,
                                  int attackerNewHp, boolean attackerDestroyed,
                                  String message) {
        AiAction a = new AiAction();
        a.setType("ATTACK");
        a.setChessId(attackerId);
        a.setTargetId(targetId);
        a.setDamage(damage);
        a.setTargetNewHp(targetNewHp);
        a.setTargetDestroyed(targetDestroyed);
        a.setCounterAttacked(counterAttacked);
        a.setCounterDamage(counterDamage);
        a.setAttackerNewHp(attackerNewHp);
        a.setAttackerDestroyed(attackerDestroyed);
        a.setMessage(message);
        return a;
    }
}
