package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Galgame 作品资料。字段来自 VNDB / 萌娘百科公开资料，由
 * backend/scripts/collect_galgame.py 清洗后写入 tb_galgame。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Galgame {
    private int id;
    /** 稳定标识，用于去重与封面文件名 */
    private String slug;
    /** 中文名 */
    private String nameZh;
    /** 原名 */
    private String nameOriginal;
    /** 别名，/ 分隔 */
    private String aliases;
    /** 列表用简短介绍 */
    private String shortIntro;
    /** 详情用无剧透简介 */
    private String intro;
    /** 封面地址（本地 /galgame/*.jpg 或来源地址） */
    private String cover;
    /** 开发商 */
    private String developer;
    /** 首发日期（字符串，兼容只有年份的条目） */
    private String releaseDate;
    /** 发行年份，用于年代筛选 */
    private Integer releaseYear;
    /** 版本/平台发行说明 */
    private String releaseNote;
    /** 支持平台 */
    private String platforms;
    /** 题材/标签，逗号分隔 */
    private String genres;
    /** 评分（十分制） */
    private BigDecimal rating;
    /** 评分人数 */
    private Integer ratingCount;
    /** 评分来源，例如 VNDB */
    private String ratingSource;
    /** 评分来源链接 */
    private String ratingSourceUrl;
    private String moegirlUrl;
    private String bangumiUrl;
    private String vndbUrl;
    private String officialUrl;
    private String steamUrl;
    /** 来源链接、采集时间与字段说明的 JSON 字符串 */
    private String sourcesJson;
    /** 来源限制/版本说明 */
    private String sourceNote;
    private Integer featured;
    private Integer sortOrder;
    private LocalDateTime fetchedAt;
    private LocalDateTime updatedAt;
}
