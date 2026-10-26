package com.example.demo.mapper.mysql;

import com.example.demo.com.Cartoonchapters;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CartoonChaptersMapper {
    List<Cartoonchapters> getAllCartoonChapters(int id);
}
