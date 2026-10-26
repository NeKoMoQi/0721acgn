package com.example.demo.com;

import lombok.Data;

@Data
public class AttackResponse {
    private Boolean success;
    private Integer damage;          // 造成的伤害
    private Integer targetNewHp;     // 目标新血量
    private Boolean targetDestroyed; // 目标是否被击败
    private String message;

    // ---- 反击 ----
    /** 是否触发反击（目标存活 + 在目标射程内） */
    private Boolean counterAttacked = Boolean.FALSE;
    /** 反击伤害 */
    private Integer counterDamage = 0;
    /** 攻击者被反击后剩余血量 */
    private Integer attackerNewHp;
    /** 攻击者是否被反击击败 */
    private Boolean attackerDestroyed = Boolean.FALSE;
}