package com.example.demo.com;

import lombok.Data;

@Data
public class MoveResult {
    private String chessId;
    private Long battleId;
    private String targetGridId;
    private Integer q;
    private Integer r;
    private Double targetLng;
    private Double targetLat;
    private Boolean success;
}