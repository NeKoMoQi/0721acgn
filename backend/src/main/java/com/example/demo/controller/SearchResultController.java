package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.com.SearchResult;
import com.example.demo.service.SearchResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class SearchResultController {
    @Autowired
    private SearchResultService searchResultService;

    /**
     * 全站检索。仅传 {@code search} 时沿用旧 UNION 语句（与原行为一致）。
     * 传入题材/状态/年代/排序/类型/limit 等任一进阶参数时，走分支查询 + Java 排序截断。
     */
    @GetMapping("/api/searchresult")
    public Result searchResult(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "default") String sortBy,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) Integer limit) {
        List<SearchResult> rows =
                searchResultService.getSearchResultsAdvanced(
                        search, genre, status, yearMin, yearMax, sortBy, mediaType, limit);
        return Result.success(rows);
    }
}
