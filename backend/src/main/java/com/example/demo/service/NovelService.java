package com.example.demo.service;

import com.example.demo.com.Novel;

import java.util.List;

public interface NovelService {
    List<Novel> getNovelPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy);
    Novel getNovelById(int id);
    int NovelCount(int id);
    String getNovelUrlById(int id,int no);
    List<Novel> getNovelHot();
}
