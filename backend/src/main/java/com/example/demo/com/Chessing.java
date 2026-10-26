package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Chessing {

    /* 主键 */
    private String id;
    /* 业务字段 */
    private String chessname;
    private Long battleId;
    private Long hexId;
    private Integer q;
    private Integer r;
    private Double centerLng;   // 经度
    private Double centerLat;   // 纬度
    /* 兵种类别：infantry / tank / artillery */
    private String type;
    /* 玩家 0=己方 1=敌方 */
    private Short player;
    /* 血量 & 上限 */
    private Integer hp;
    private Integer maxHp;
    /* 机动、攻击、射程 */
    private Integer move;
    private Integer atk;
    private Integer range;
    /* 回合内状态 */
    private Boolean moved;
    private Boolean fired;
    /* 时间戳 */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private  String chessurl;
}

