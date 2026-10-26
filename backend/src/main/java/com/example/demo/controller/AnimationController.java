package com.example.demo.controller;

import com.example.demo.com.Animation;
import com.example.demo.com.AnimationEpisode;
import com.example.demo.com.Result;
import com.example.demo.service.AnimationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class AnimationController {
    @Autowired
    private AnimationService animationService;

    @GetMapping("/api/animation")
    public Result getAnimation(
            @RequestParam int page,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "default") String sortBy) {
        int offset = (page - 1) * 20;
        List<Animation> animations = animationService.getAnimationPage(
                offset,
                emptyToNull(genre),
                emptyToNull(status),
                yearMin,
                yearMax,
                normalizeSort(sortBy));
        return Result.success(animations);
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
    @GetMapping("/api/animation/hot")
    public Result getHotAnimation() {
        List<Animation> b = animationService.getAnimationHot();
        return Result.success(b);
    }
    @GetMapping("/api/animation/byid")
    public Result getAnimationById(@RequestParam int id) {
        Animation animation = animationService.getAnimationbyid(id);
        return Result.success(animation);
    }

    @GetMapping("/api/animation/episodes")
    public Result getAnimationEpisodes(@RequestParam int animationId) {
        List<AnimationEpisode> episodes = animationService.listEpisodesByAnimationId(animationId);
        return Result.success(episodes);
    }

    @GetMapping("/api/animation/episode")
    public Result getAnimationEpisode(@RequestParam int id) {
        AnimationEpisode episode = animationService.getEpisodeById(id);
        return Result.success(episode);
    }
}
