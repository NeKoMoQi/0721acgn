package com.example.demo.mapper.mysql;

import com.example.demo.com.NovelChapter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelChaptersMapper {
    List<NovelChapter> getNovelChapters(int id);
}
