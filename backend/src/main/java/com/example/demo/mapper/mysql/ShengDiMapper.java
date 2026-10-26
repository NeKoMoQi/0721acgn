package com.example.demo.mapper.mysql;

import com.example.demo.com.Shengdi;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShengDiMapper {
    List<Shengdi> selectALLhot();
}
