package com.example.demo.controller;

import com.example.demo.com.PublicVideoClipVo;
import com.example.demo.com.Result;
import com.example.demo.service.AnimationPublicClipsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开宣传片列表（正版 PV / 先导片外链），不涉及正片或未授权爬虫。
 */
@RestController
public class AnimationPublicClipsController {

    private final AnimationPublicClipsService clipsService;

    public AnimationPublicClipsController(AnimationPublicClipsService clipsService) {
        this.clipsService = clipsService;
    }

    @GetMapping("/api/animation/public-clips")
    public Result<List<PublicVideoClipVo>> listClips(@RequestParam int animationId) {
        return Result.success(clipsService.listPublicClipsByAnimationId(animationId));
    }
}
