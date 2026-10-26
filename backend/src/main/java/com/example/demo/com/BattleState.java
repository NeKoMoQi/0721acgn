package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BattleState {
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_WIN_P0 = "WIN_PLAYER0";
    public static final String STATUS_WIN_P1 = "WIN_PLAYER1";
    public static final String STATUS_DRAW   = "DRAW";

    private Long battleId;
    private Short currentPlayer;
    private Integer roundNo;
    private String status;
    private Short winner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
