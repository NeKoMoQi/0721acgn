package com.example.demo.mapper.mysql;

import com.example.demo.com.SearchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchResultMapper {
    List<SearchResult> getSearchResult(@Param("search") String search);

    List<SearchResult> searchComicsGlobal(@Param("search") String search,
                                          @Param("genre") String genre,
                                          @Param("status") String status,
                                          @Param("yearMin") Integer yearMin,
                                          @Param("yearMax") Integer yearMax);

    List<SearchResult> searchAnimationGlobal(@Param("search") String search,
                                               @Param("genre") String genre,
                                               @Param("status") String status,
                                               @Param("yearMin") Integer yearMin,
                                               @Param("yearMax") Integer yearMax);

    List<SearchResult> searchNovelGlobal(@Param("search") String search,
                                          @Param("genre") String genre,
                                          @Param("status") String status,
                                          @Param("yearMin") Integer yearMin,
                                          @Param("yearMax") Integer yearMax);
}
