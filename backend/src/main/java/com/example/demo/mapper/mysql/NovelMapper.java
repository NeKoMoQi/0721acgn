package com.example.demo.mapper.mysql;

import com.example.demo.com.Novel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelMapper {
    List<Novel> getNovelAll(
            @Param("offset") int offset,
            @Param("genre") String genre,
            @Param("status") String status,
            @Param("yearMin") Integer yearMin,
            @Param("yearMax") Integer yearMax,
            @Param("sortBy") String sortBy);
    Novel getNovelById(int id);
    int NovelCount(int id);
    String getNovelUrlById(int id,int no);
    List<Novel> getNovelHot();

    List<Novel> listByExactTrimmedName(@Param("name") String name, @Param("excludeId") Integer excludeId);

    List<Novel> listSameSeriesCandidates(
            @Param("seriesPrefix") String seriesPrefix,
            @Param("seriesPrefixEsc") String seriesPrefixEsc,
            @Param("excludeId") Integer excludeId);
}
