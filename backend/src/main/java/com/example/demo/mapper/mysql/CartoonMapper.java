package com.example.demo.mapper.mysql;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartoonMapper {
    List<Cartoons> getCartoons(
            @Param("offset") int offset,
            @Param("genre") String genre,
            @Param("status") String status,
            @Param("yearMin") Integer yearMin,
            @Param("yearMax") Integer yearMax,
            @Param("sortBy") String sortBy);
    Cartoons getCartoonsbyid(int id);
    List<CartoonPage> getCartoonpages(String chapter,int page);
    List<Cartoons> getCartoonsHot();

    List<Cartoons> listByExactTrimmedName(@Param("name") String name, @Param("excludeId") Integer excludeId);

    List<Cartoons> listSameSeriesCandidates(
            @Param("seriesPrefix") String seriesPrefix,
            @Param("seriesPrefixEsc") String seriesPrefixEsc,
            @Param("excludeId") Integer excludeId);
}
