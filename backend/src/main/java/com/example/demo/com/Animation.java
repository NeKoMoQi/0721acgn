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
public class Animation {
    private int id;
    private String name;
    private String shortcomment;
    private String longcomment;
    private BigDecimal rating;
    private String releasedate;
    private String status;
    private String bcimg;
    /** 试看/PV 视频（OSS 或外链），对应 preview_video_url */
    private String previewVideoUrl;
    private String countpage;
    private String voicer;
    private String hengpic;
    /** 题材（迁移脚本 acgn_extend_filters_related.sql） */
    private String genre;
    private LocalDateTime updatedAt;
}
