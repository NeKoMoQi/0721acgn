package com.example.demo.service;

import com.example.demo.com.MoveRangeVO;

public interface MoveRangeService {
    /**
     * 计算棋子的行动范围
     * @param chessId 棋子ID
     * @param battleId 战场ID
     * @return 行动范围VO
     */
    MoveRangeVO calculateMoveRange(String chessId, int battleId);
}