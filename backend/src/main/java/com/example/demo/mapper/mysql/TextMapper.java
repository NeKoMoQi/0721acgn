package com.example.demo.mapper.mysql;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TextMapper {
    String selectText();
}
