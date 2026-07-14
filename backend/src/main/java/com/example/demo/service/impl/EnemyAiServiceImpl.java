package com.example.demo.service.impl;

import com.example.demo.com.*;
import com.example.demo.mapper.postgresql.WarChessingMapper;
import com.example.demo.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用敌方 AI：当前回合方若不是人类，则代操该阵营全部棋子，
 * 以消灭人类玩家 {@code humanPlayer} 的指挥部为最高优先级。
 */
@Slf4j
@Service
public class EnemyAiServiceImpl implements EnemyAiService {

    private final WarChessingMapper   warChessingMapper;
    private final WarChessingService  warChessingService;
    private final ChessPieceService   chessPieceService;
    private final MoveRangeService    moveRangeService;
    private final BattleStateService  battleStateService;

    public EnemyAiServiceImpl(WarChessingMapper warChessingMapper,
                              WarChessingService warChessingService,
                              ChessPieceService chessPieceService,
                              MoveRangeService moveRangeService,
                              BattleStateService battleStateService) {
        this.warChessingMapper  = warChessingMapper;
        this.warChessingService = warChessingService;
        this.chessPieceService  = chessPieceService;
        this.moveRangeService   = moveRangeService;
        this.battleStateService = battleStateService;
    }

    @Override
    @Transactional
    public AiTurnResult runEnemyTurn(Long battleId, Short humanPlayer) {
        List<AiAction> actions = new ArrayList<>();
        short human = humanPlayer == null ? 0 : humanPlayer.shortValue();

        BattleState state = battleStateService.getOrInit(battleId);

        if (!BattleState.STATUS_ONGOING.equals(state.getStatus())) {
            return new AiTurnResult(false, "战斗已经结束（" + state.getStatus() + "）", state, actions);
        }
        Short curr = state.getCurrentPlayer();
        if (curr == null || curr.shortValue() == human) {
            return new AiTurnResult(false, "现在不是 AI 回合（或 humanPlayer 参数与 current 冲突）", state, actions);
        }

        final short aiSide = curr.shortValue();

        List<Chessing> all = warChessingMapper.getWarChessing(battleId.intValue());
        Chessing targetHq = findHeadquartersForSide(all, human);
        if (targetHq == null) {
            BattleState afterEnd = battleStateService.endTurn(battleId);
            return new AiTurnResult(true, "人类指挥部已不存在，AI 直接结束回合", afterEnd, actions);
        }

        List<Chessing> aiPieces = all.stream()
                .filter(c -> c.getPlayer() != null && c.getPlayer() == aiSide)
                .filter(c -> c.getHp() != null && c.getHp() > 0)
                .filter(c -> c.getQ() != null && c.getR() != null)
                .sorted(Comparator.comparingInt(c -> hexDistance(
                        c.getQ(), c.getR(), targetHq.getQ(), targetHq.getR())))
                .collect(Collectors.toList());

        log.info("[AI] battleId={} aiSide={} human={} units={} targetHQ={}",
                battleId, aiSide, human, aiPieces.size(), targetHq.getId());

        for (Chessing snapshot : aiPieces) {
            String pieceId = snapshot.getId();

            Chessing piece = warChessingMapper.selectById(pieceId);
            if (!isAlive(piece)) continue;

            tryAttackOnce(piece, battleId, actions, human);

            piece = warChessingMapper.selectById(pieceId);
            if (!isAlive(piece)) continue;

            if (isBattleOver(battleId)) break;

            if (!isHeadquartersForSide(piece, aiSide)
                    && !Boolean.TRUE.equals(piece.getMoved())) {

                Chessing freshHq = warChessingMapper.selectById(targetHq.getId());
                if (isAlive(freshHq)) {
                    tryMoveTowardHq(piece, freshHq, battleId, actions, aiSide);
                }
            }

            piece = warChessingMapper.selectById(pieceId);
            if (!isAlive(piece)) continue;
            if (!Boolean.TRUE.equals(piece.getFired())) {
                tryAttackOnce(piece, battleId, actions, human);
            }

            if (isBattleOver(battleId)) break;
        }

        BattleState finalState = battleStateService.endTurn(battleId);
        log.info("[AI] battleId={} 结束，actions={} status={}",
                battleId, actions.size(), finalState.getStatus());

        return new AiTurnResult(true, "AI 回合执行完毕", finalState, actions);
    }

    private void tryAttackOnce(Chessing attacker, Long battleId,
                               List<AiAction> actions, short humanPlayer) {
        if (Boolean.TRUE.equals(attacker.getFired())) return;

        List<AttackTargetDTO> candidates =
                warChessingService.getAttackableTargets(attacker.getId(), battleId);
        if (candidates.isEmpty()) return;

        AttackTargetDTO chosen = candidates.stream()
                .filter(t -> isHeadquartersId(t.getChessId(), humanPlayer))
                .findFirst()
                .orElse(null);

        if (chosen == null) {
            int atk = attacker.getAtk() == null ? 0 : attacker.getAtk();
            chosen = candidates.stream()
                    .min(Comparator
                            .comparing((AttackTargetDTO t) -> (t.getHp() != null && t.getHp() <= atk) ? 0 : 1)
                            .thenComparing(t -> t.getHp() == null ? Integer.MAX_VALUE : t.getHp())
                            .thenComparing(t -> t.getDistance() == null ? Double.MAX_VALUE : t.getDistance()))
                    .orElse(null);
        }

        if (chosen == null) return;

        AttackResponse resp = warChessingService.attack(attacker.getId(), chosen.getChessId(), battleId);
        if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
            log.warn("[AI] 攻击失败: {} → {}, msg={}",
                    attacker.getId(), chosen.getChessId(),
                    resp == null ? null : resp.getMessage());
            return;
        }

        actions.add(AiAction.attack(
                attacker.getId(),
                chosen.getChessId(),
                resp.getDamage()           == null ? 0 : resp.getDamage(),
                resp.getTargetNewHp()      == null ? 0 : resp.getTargetNewHp(),
                Boolean.TRUE.equals(resp.getTargetDestroyed()),
                Boolean.TRUE.equals(resp.getCounterAttacked()),
                resp.getCounterDamage()    == null ? 0 : resp.getCounterDamage(),
                resp.getAttackerNewHp()    == null ? 0 : resp.getAttackerNewHp(),
                Boolean.TRUE.equals(resp.getAttackerDestroyed()),
                attacker.getChessname() + " → " + chosen.getChessname() + ": " + resp.getMessage()
        ));
    }

    private void tryMoveTowardHq(Chessing piece, Chessing humanHq,
                                 Long battleId, List<AiAction> actions,
                                 short aiSide) {
        if (canDirectlyHit(piece, humanHq)) return;

        MoveRangeVO range = moveRangeService.calculateMoveRange(piece.getId(), battleId.intValue());
        Set<String> cells = range == null ? null : range.getRangeGridIds();
        if (cells == null || cells.isEmpty()) return;

        int currDist = hexDistance(piece.getQ(), piece.getR(), humanHq.getQ(), humanHq.getR());

        String bestCell = null;
        int bestDist = currDist;
        for (String cell : cells) {
            int[] qr = parseCell(cell);
            if (qr == null) continue;
            int d = hexDistance(qr[0], qr[1], humanHq.getQ(), humanHq.getR());
            if (d < bestDist) {
                bestDist = d;
                bestCell = cell;
            }
        }

        if (bestCell == null) return;

        int fromQ = piece.getQ();
        int fromR = piece.getR();

        try {
            boolean ok = chessPieceService.moveChessPiece(piece.getId(), battleId, bestCell);
            if (!ok) return;
        } catch (RuntimeException e) {
            log.warn("[AI] 移动失败: {} → {}, err={}", piece.getId(), bestCell, e.getMessage());
            return;
        }

        Chessing moved = warChessingMapper.selectById(piece.getId());
        if (moved == null) return;

        actions.add(AiAction.move(
                piece.getId(),
                fromQ, fromR,
                moved.getQ(), moved.getR(),
                moved.getCenterLng(), moved.getCenterLat(),
                piece.getChessname() + " 推进至 (" + moved.getQ() + "," + moved.getR() + ")"
        ));
    }

    private boolean canDirectlyHit(Chessing attacker, Chessing target) {
        if (attacker.getRange() == null || attacker.getRange() < 1) return false;
        if (Boolean.TRUE.equals(attacker.getFired())) return false;
        return hexDistance(attacker.getQ(), attacker.getR(),
                target.getQ(), target.getR()) <= attacker.getRange();
    }

    private boolean isAlive(Chessing c) {
        return c != null && c.getHp() != null && c.getHp() > 0;
    }

    private boolean isHeadquartersId(String chessId, short owner) {
        if (chessId == null) return false;
        return isHeadquartersForSide(warChessingMapper.selectById(chessId), owner);
    }

    /** player=side 且为指挥部类型（苏 11/sovcommand，德 10/GermanCommand） */
    private boolean isHeadquartersForSide(Chessing c, short side) {
        if (c == null || c.getType() == null || c.getPlayer() == null) return false;
        if (c.getPlayer().shortValue() != side) return false;
        String t = c.getType();
        if (side == 0) {
            return "11".equals(t) || "sovcommand".equalsIgnoreCase(t);
        }
        return "10".equals(t) || "GermanCommand".equals(t);
    }

    private Chessing findHeadquartersForSide(List<Chessing> all, short side) {
        return all.stream()
                .filter(c -> isHeadquartersForSide(c, side))
                .filter(c -> c.getHp() != null && c.getHp() > 0)
                .filter(c -> c.getQ() != null && c.getR() != null)
                .findFirst()
                .orElse(null);
    }

    private boolean isBattleOver(Long battleId) {
        BattleState s = battleStateService.evaluateVictory(battleId);
        return s != null && !BattleState.STATUS_ONGOING.equals(s.getStatus());
    }

    private int hexDistance(int q1, int r1, int q2, int r2) {
        int dq = q1 - q2;
        int dr = r1 - r2;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }

    private int[] parseCell(String cell) {
        if (cell == null) return null;
        String[] parts = cell.split(",");
        if (parts.length != 2) return null;
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
