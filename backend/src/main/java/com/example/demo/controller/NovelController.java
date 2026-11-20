package com.example.demo.controller;

import com.example.demo.com.Novel;
import com.example.demo.com.NovelChapter;
import com.example.demo.com.Result;
import com.example.demo.service.NovelChapterService;
import com.example.demo.service.NovelFileService;
import com.example.demo.service.NovelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
public class NovelController {
    @Autowired
    private NovelService novelService;
    @Autowired
    private NovelChapterService novelChapterService;
    @Autowired
    private NovelFileService novelFileService;
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

    /**
     * 同源返回整卷 epub 字节。浏览器直连 OSS 会被 CORS 拦（存储桶没配跨域），
     * 前端改用这个接口取 ArrayBuffer 交给 epub.js。
     */
    @GetMapping("/api/novel/content")
    public ResponseEntity<Object> getNovelFile(@RequestParam int id, @RequestParam int no) {
        String url = novelService.getNovelUrlById(id, no);
        if (url == null || url.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error("暂无该卷正文"));
        }
        try {
            byte[] body = novelFileService.fetch(url);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/epub+zip"))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(body);
        } catch (IllegalArgumentException e) {
            log.warn("novel file rejected: id={} no={} {}", id, no, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error("正文地址不可用"));
        } catch (Exception e) {
            log.warn("novel file fetch failed: id={} no={} {}", id, no, e.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Result.error("正文文件获取失败，请稍后重试"));
        }
    }
    @GetMapping("/api/novel/hot")
    public Result getNovelHot(){
        List<Novel> novels = novelService.getNovelHot();
        return Result.success(novels);
    }
}
