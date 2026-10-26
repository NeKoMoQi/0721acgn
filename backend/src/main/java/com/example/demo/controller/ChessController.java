package com.example.demo.controller;

import com.example.demo.com.*;
import com.example.demo.service.WarChessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ChessController {
    @Autowired
    private WarChessingService warChessingService;
    @GetMapping("/chessing/getflag")
    public Result getWarChessing(@RequestParam int id){
        Integer bigid = id;
        List<Chessing> chessing = warChessingService.getWarChessing(bigid);
        System.out.println(chessing);
        return Result.success(chessing);
    }

    @GetMapping("/chessing/attackrange")
    public Result<List<AttackTargetDTO>> getAttackRange(
            @RequestParam String chessId,
            @RequestParam Long battleId) {
        List<AttackTargetDTO> targets = warChessingService.getAttackableTargets(chessId, battleId);
        return Result.success(targets);
    }

    /**
     * 执行攻击
     */
    @PostMapping("/chessing/attack")
    public Result<AttackResponse> attack(@RequestBody AttackRequest request) {
        if (request == null) {
            return Result.error("请求体不能为空");
        }
        AttackResponse response = warChessingService.attack(
                request.getAttackerId(),
                request.getTargetId(),
                request.getBattleId()
        );
        if (response != null && Boolean.FALSE.equals(response.getSuccess())) {
            log.warn("攻击失败: attacker={}, target={}, battleId={}, msg={}",
                    request.getAttackerId(),
                    request.getTargetId(),
                    request.getBattleId(),
                    response.getMessage());
        }
        return Result.success(response);
    }
}
