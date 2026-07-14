package com.example.demo.com;

import lombok.Data;

@Data
public class AttackTargetDTO {
    private String chessId;
    private String chessname;
    private Integer hp;
    private Integer maxHp;
    private Double centerLng;
    private Double centerLat;
    private Double distance; // 与攻击者的距离
}