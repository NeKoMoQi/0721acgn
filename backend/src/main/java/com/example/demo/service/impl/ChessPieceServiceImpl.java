package com.example.demo.service.impl;

import com.example.demo.com.Chessing;
import com.example.demo.com.WarGrid;
import com.example.demo.mapper.postgresql.WarChessingMapper;
import com.example.demo.service.BattleStateService;
import com.example.demo.service.ChessPieceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChessPieceServiceImpl implements ChessPieceService {

    @Autowired
    private WarChessingMapper warChessingMapper;

    @Autowired
    private BattleStateService battleStateService;

    @Override
    @Transactional
    public boolean moveChessPiece(String chessId, Long battleId, String targetGridId) {
        Chessing piece = warChessingMapper.selectById(chessId);
        if (piece == null) {
            throw new RuntimeException("妫嬪瓙涓嶅瓨鍦? " + chessId);
        }

        if (!piece.getBattleId().equals(battleId)) {
            throw new RuntimeException("Invalid operation");
        }

        if (!battleStateService.canActAs(battleId, piece.getPlayer())) {
            throw new RuntimeException("鐜板湪涓嶆槸浣犵殑鍥炲悎");
        }

        if (Boolean.TRUE.equals(piece.getMoved())) {
            throw new RuntimeException("Invalid operation");
        }

        String[] parts = targetGridId.split(",");
        if (parts.length != 2) {
            throw new RuntimeException("鐩爣鏍煎瓙ID鏍煎紡閿欒: " + targetGridId);
        }
        Integer q = Integer.parseInt(parts[0]);
        Integer r = Integer.parseInt(parts[1]);

        WarGrid grid = warChessingMapper.selectGridByQAndR(q, r);
        System.out.println(q + " " + r + " " + grid);
        if (grid == null || grid.getCenter() == null || grid.getCenter().size() < 2) {
            throw new RuntimeException("鐩爣鏍煎瓙涓嶅瓨鍦ㄦ垨鍧愭爣鏃犳晥: " + targetGridId);
        }
        Double targetLng = grid.getCenter().get(0);
        Double targetLat = grid.getCenter().get(1);

        piece.setQ(q);
        piece.setR(r);
        piece.setCenterLng(targetLng);
        piece.setCenterLat(targetLat);
        piece.setUpdatedAt(LocalDateTime.now());
        piece.setMoved(true);

        return warChessingMapper.updateById(piece) > 0;
    }
}
