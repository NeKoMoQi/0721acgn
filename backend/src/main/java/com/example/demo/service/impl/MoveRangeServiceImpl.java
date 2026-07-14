package com.example.demo.service.impl;

import com.example.demo.com.Chessing;
import com.example.demo.com.MoveRangeVO;
import com.example.demo.com.WarGrid;
import com.example.demo.service.MoveRangeService;
import com.example.demo.service.WarChessingService;
import com.example.demo.service.WarGridService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MoveRangeServiceImpl implements MoveRangeService {

    private static final int[][] HEX_DIRECTIONS = {
            {1, 0},
            {1, -1},
            {0, -1},
            {-1, 0},
            {-1, 1},
            {0, 1}
    };

    @Autowired
    private WarChessingService warChessingService;

    @Autowired
    private WarGridService warGridService;

    @Override
    public MoveRangeVO calculateMoveRange(String chessId, int battleId) {
        long startTime = System.currentTimeMillis();

        List<Chessing> allChess = warChessingService.getWarChessing(battleId);
        Chessing currentChess = allChess.stream()
                .filter(c -> c.getId().equals(chessId))
                .findFirst()
                .orElse(null);

        if (currentChess == null) {
            log.warn("Chess piece not found: {}", chessId);
            return new MoveRangeVO(chessId, Collections.emptySet(), 0, 0L);
        }

        List<WarGrid> allGrids = warGridService.getWarGrid();
        Map<String, WarGrid> gridByKey = allGrids.stream()
                .collect(Collectors.toMap(
                        g -> gridKey(g.getQ(), g.getR()),
                        g -> g,
                        (a, b) -> a
                ));

        Set<String> occupiedGrids = allChess.stream()
                .filter(c -> !c.getId().equals(chessId))
                .map(c -> gridKey(c.getQ(), c.getR()))
                .collect(Collectors.toSet());

        int movePoints = currentChess.getMove() != null ? currentChess.getMove() : 3;
        Set<String> reachableGrids = calculateTerrainAwareMoveRange(
                currentChess,
                movePoints,
                gridByKey,
                occupiedGrids
        );

        long costTime = System.currentTimeMillis() - startTime;
        log.info("Move range calculated for chess {}: {} grids, {}ms",
                chessId, reachableGrids.size(), costTime);

        return new MoveRangeVO(chessId, reachableGrids, reachableGrids.size(), costTime);
    }

    /**
     * Terrain-aware movement uses Dijkstra instead of plain BFS.
     *
     * Cost means "movement points needed to enter the target grid".
     * A grid is reachable when the lowest accumulated cost is <= movePoints.
     */
    private Set<String> calculateTerrainAwareMoveRange(Chessing chess,
                                                       int movePoints,
                                                       Map<String, WarGrid> gridByKey,
                                                       Set<String> occupiedGrids) {
        String startKey = gridKey(chess.getQ(), chess.getR());
        if (!gridByKey.containsKey(startKey)) {
            return Collections.emptySet();
        }

        Set<String> rangeSet = new HashSet<>();
        Map<String, Integer> bestCost = new HashMap<>();
        PriorityQueue<GridNode> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));

        bestCost.put(startKey, 0);
        queue.offer(new GridNode(chess.getQ(), chess.getR(), 0));

        while (!queue.isEmpty()) {
            GridNode current = queue.poll();
            String currentKey = gridKey(current.q, current.r);
            if (current.cost > bestCost.getOrDefault(currentKey, Integer.MAX_VALUE)) {
                continue;
            }

            for (int[] dir : HEX_DIRECTIONS) {
                int nextQ = current.q + dir[0];
                int nextR = current.r + dir[1];
                String nextKey = gridKey(nextQ, nextR);
                WarGrid nextGrid = gridByKey.get(nextKey);

                if (nextGrid == null) {
                    continue;
                }
                if (occupiedGrids.contains(nextKey)) {
                    continue;
                }

                int stepCost = terrainMoveCost(nextGrid, chess);
                if (stepCost <= 0) {
                    continue;
                }

                int nextCost = current.cost + stepCost;
                if (nextCost > movePoints) {
                    continue;
                }
                if (nextCost >= bestCost.getOrDefault(nextKey, Integer.MAX_VALUE)) {
                    continue;
                }

                bestCost.put(nextKey, nextCost);
                rangeSet.add(nextKey);
                queue.offer(new GridNode(nextQ, nextR, nextCost));
            }
        }

        rangeSet.remove(startKey);
        return rangeSet;
    }

    private int terrainMoveCost(WarGrid grid, Chessing chess) {
        String terrain = normalize(grid.getEleclass());
        String unitType = normalize(chess.getType());

        boolean tankLike = unitType.contains("tank")
                || unitType.contains("armor")
                || unitType.contains("panzer")
                || unitType.contains("坦克")
                || unitType.contains("装甲");
        boolean artilleryLike = unitType.contains("artillery")
                || unitType.contains("gun")
                || unitType.contains("炮");
        boolean infantryLike = unitType.contains("infantry")
                || unitType.contains("步兵");

        switch (terrain) {
            case "prokhorovka_open":
            case "oboyan_open":
            case "steppe":
                return 1;
            case "rolling_steppe":
                return tankLike ? 1 : 2;
            case "ridge_highland":
                if (tankLike || artilleryLike) return 2;
                return infantryLike ? 1 : 2;
            case "forest_steppe":
                if (tankLike || artilleryLike) return 3;
                return 2;
            case "river_valley":
                if (tankLike) return 3;
                return 2;
            case "water":
            case "river":
                return 99;
            default:
                return 1;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String gridKey(Integer q, Integer r) {
        return q + "," + r;
    }

    @AllArgsConstructor
    private static class GridNode {
        int q;
        int r;
        int cost;
    }
}
