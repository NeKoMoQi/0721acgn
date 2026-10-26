package com.example.demo.service;

public interface ChessPieceService {
    // 修改：去掉 targetLng, targetLat，只传 targetGridId
    boolean moveChessPiece(String chessId, Long battleId, String targetGridId);
}