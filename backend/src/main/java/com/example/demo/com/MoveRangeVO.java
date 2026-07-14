package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRangeVO {
    private String chessId;           // 棋子ID
    private Set<String> rangeGridIds; // 可到达的格子ID集合，如 ["2,-3", "3,-3"]
    private Integer gridCount;        // 格子数量
    private Long costTimeMs;        // 计算耗时
}