package com.example.demo.service.impl;

import com.example.demo.com.NovelChapter;
import com.example.demo.mapper.mysql.NovelChaptersMapper;
import com.example.demo.service.NovelChapterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NovelChaptersServiceImpl implements NovelChapterService {
    NovelChaptersMapper novelChaptersMapper;
    public NovelChaptersServiceImpl (NovelChaptersMapper novelChaptersMapper) {
        this.novelChaptersMapper = novelChaptersMapper;
    }
    public List<NovelChapter> getNovelChapters(int id){
        return novelChaptersMapper.getNovelChapters(id);
    }
}
