package com.example.demo.mapper.postgresql;

import com.example.demo.com.WarGrid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarGridMapper {
    List<WarGrid> getWarGrid();

    // 添加这个方法
    WarGrid selectByQAndR(@Param("q") Integer q, @Param("r") Integer r);
}
