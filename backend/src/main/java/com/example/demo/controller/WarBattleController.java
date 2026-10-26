package com.example.demo.controller;

import com.example.demo.com.MoveRangeVO;
import com.example.demo.com.MoveResult;
import com.example.demo.com.Result;
import com.example.demo.com.WarGrid;
import com.example.demo.mapper.postgresql.WarGridMapper;
import com.example.demo.service.ChessPieceService;
import com.example.demo.service.MoveRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WarBattleController {

    @Autowired
    private MoveRangeService moveRangeService;

    @Autowired
    private ChessPieceService chessPieceService;

    @Autowired
    private WarGridMapper warGridMapper;

    @GetMapping("/chessing/getflagrange")
    public Result<MoveRangeVO> getMoveRange(
            @RequestParam String id,
            @RequestParam int range) {
        MoveRangeVO vo = moveRangeService.calculateMoveRange(id, range);
        return Result.success(vo);
    }

    @PostMapping("/chessing/moveFlag")
    public Result<MoveResult> moveChess(@RequestBody Map<String, Object> params) {
        String chessId = (String) params.get("chessId");
        Integer battleIdInt = (Integer) params.get("battleId");
        Long battleId = battleIdInt != null ? battleIdInt.longValue() : null;
        String targetGridId = (String) params.get("targetGridId");

        if (chessId == null || targetGridId == null) {
            return Result.error("鍙傛暟涓嶈兘涓虹┖");
        }

        String[] parts = targetGridId.split(",");
        if (parts.length != 2) {
            return Result.error("鐩爣鏍煎瓙ID鏍煎紡閿欒");
        }
        Integer q = Integer.parseInt(parts[0]);
        Integer r = Integer.parseInt(parts[1]);

        WarGrid grid = warGridMapper.selectByQAndR(q, r);
        System.out.println(grid);
        if (grid == null || grid.getCenter() == null || grid.getCenter().size() < 2) {
            return Result.error("鐩爣鏍煎瓙涓嶅瓨鍦ㄦ垨鍧愭爣鏃犳晥");
        }

        Double targetLng = grid.getCenter().get(0);
        Double targetLat = grid.getCenter().get(1);

        boolean success = chessPieceService.moveChessPiece(
                chessId,
                battleId,
                targetGridId
        );

        MoveResult result = new MoveResult();
        result.setChessId(chessId);
        result.setBattleId(battleId);
        result.setTargetGridId(targetGridId);
        result.setQ(q);
        result.setR(r);
        result.setTargetLng(targetLng);
        result.setTargetLat(targetLat);
        result.setSuccess(success);

        if (result.getTargetLat() != null) {
            return Result.success(result);
        } else {
            return Result.error("绉诲姩澶辫触");
        }
    }
}
