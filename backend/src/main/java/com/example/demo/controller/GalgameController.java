package com.example.demo.controller;

import com.example.demo.com.Galgame;
import com.example.demo.com.Result;
import com.example.demo.service.GalgameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GalgameController {
    private final GalgameService galgameService;

    public GalgameController(GalgameService galgameService) {
        this.galgameService = galgameService;
    }

    @GetMapping("/api/galgame")
    public Result getGalgame(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "default") String sortBy) {
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * 20;
        List<Galgame> galgames = galgameService.getGalgamePage(
                offset,
                emptyToNull(genre),
                yearMin,
                yearMax,
                normalizeSort(sortBy));
        return Result.success(galgames);
    }

    @GetMapping("/api/galgame/hot")
    public Result getHotGalgame() {
        return Result.success(galgameService.getGalgameHot());
    }

    @GetMapping("/api/galgame/byid")
    public Result getGalgameById(@RequestParam int id) {
        return Result.success(galgameService.getGalgameById(id));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "default";
        }
        String s = sortBy.trim().toLowerCase();
        return ("rating".equals(s) || "release".equals(s)) ? s : "default";
    }
}
