package com.example.demo.mapper.mysql;

import com.example.demo.com.Galgame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GalgameMapper {
    List<Galgame> getGalgame(
            @Param("offset") int offset,
            @Param("genre") String genre,
            @Param("yearMin") Integer yearMin,
            @Param("yearMax") Integer yearMax,
            @Param("sortBy") String sortBy);

    List<Galgame> getGalgameHot();

    Galgame getGalgameById(@Param("id") int id);
}
