package com.example.demo.service.impl;

import com.example.demo.com.Novel;
import com.example.demo.mapper.mysql.NovelMapper;
import com.example.demo.service.NovelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NovelServiceImpl implements NovelService {
    private NovelMapper novelMapper;
    public NovelServiceImpl (NovelMapper novelMapper) {
        this.novelMapper = novelMapper;
    }
    public List<Novel> getNovelPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy) {
        return novelMapper.getNovelAll(offset, genre, status, yearMin, yearMax, sortBy);
    }
    public Novel getNovelById(int id) {
        return novelMapper.getNovelById(id);
    }
    public int NovelCount(int id){
        return novelMapper.NovelCount(id);
    }
    public String getNovelUrlById(int id,int no){
        return novelMapper.getNovelUrlById(id,no);
    }
    public List<Novel> getNovelHot(){
        return novelMapper.getNovelHot();
    }
}
