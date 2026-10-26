package com.example.demo.controller;

import com.example.demo.com.Novel;
import com.example.demo.com.NovelChapter;
import com.example.demo.com.Result;
import com.example.demo.service.NovelChapterService;
import com.example.demo.service.NovelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class NovelController {
    @Autowired
    private NovelService novelService;
    @Autowired
    private NovelChapterService novelChapterService;
    @GetMapping("/api/novel")
    public Result getNovelAll(
            @RequestParam int page,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "default") String sortBy) {
        int offset = (page - 1) * 40;
        List<Novel> novels = novelService.getNovelPage(
                offset,
                emptyToNull(genre),
                emptyToNull(status),
                yearMin,
                yearMax,
                normalizeSort(sortBy));
        return Result.success(novels);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "default";
        }
        String s = sortBy.trim().toLowerCase();
        return ("hot".equals(s) || "update".equals(s)) ? s : "default";
    }
    @GetMapping("/api/novel/chapter")
    public Result getNovelChapter(@RequestParam int id){
        List<NovelChapter> chapters = novelChapterService.getNovelChapters(id);
        return Result.success(chapters);
    }
    @GetMapping("/api/novel/byid")
    public Result getNovelById(@RequestParam int id){
        Novel novel = novelService.getNovelById(id);
        return Result.success(novel);
    }
    @GetMapping("/api/novel/count")
    public Result getNovelCount(@RequestParam int id){
        int count = novelService.NovelCount(id);
        return Result.success(count);
    }
    @GetMapping("/api/novel/chapter/byid")
    public Result getNovelChapterById(@RequestParam int id,int no){
        String novelurl = novelService.getNovelUrlById(id,no);
        return Result.success(novelurl);
    }
    @GetMapping("/api/novel/hot")
    public Result getNovelHot(){
        List<Novel> novels = novelService.getNovelHot();
        return Result.success(novels);
    }
}
