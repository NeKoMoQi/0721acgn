package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WarGrid {
    private List<List<Double>>gridgeometry;     // 多边形 WKT
    private Integer q;           // 网格 X 索引
    private Integer r;           // 网格 Y 索引
    private String  id;      // 原始 id 字段（如 "-2,-4"）
    private Double  elevation;   // 海拔
    private String  terrain;     // 地形描述
    private String  eleclass;// 地形分类
    private Integer bigmap;// 大图编号
    private List<Double>  center;//中心点
}