package com.example.demo.com;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SearchResult {
    private int id;
    private String name;
    private String picurl;
    private String sourcetype;
    /** 用于热搜排序展示；可为 null */
    private BigDecimal rating;
    private LocalDateTime updatedAt;
}
