package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.service.AnimationService;
import com.example.demo.service.AnimationVideoUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class AnimationPreviewVideoController {

    private final AnimationVideoUploadService animationVideoUploadService;
    private final AnimationService animationService;

    public AnimationPreviewVideoController(
            AnimationVideoUploadService animationVideoUploadService,
            AnimationService animationService) {
        this.animationVideoUploadService = animationVideoUploadService;
        this.animationService = animationService;
    }

    @PostMapping("/api/animation/preview-video")
    public Result<Map<String, String>> uploadPreviewVideo(
            @RequestParam int animationId,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = animationVideoUploadService.uploadPreviewVideo(file);
            animationService.updatePreviewVideoUrl(animationId, url);
            return urlResult(url);
        } catch (IllegalArgumentException e) {
            log.warn("preview-video rejected: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("preview-video failed", e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    @PostMapping("/api/animation/preview-video/local")
    public Result<Map<String, String>> importPreviewVideo(
            @RequestParam int animationId,
            @RequestParam String sourcePath) {
        try {
            String url = animationVideoUploadService.importPreviewVideo(Path.of(sourcePath));
            animationService.updatePreviewVideoUrl(animationId, url);
            return urlResult(url);
        } catch (IllegalArgumentException e) {
            log.warn("preview-video import rejected: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("preview-video import failed", e);
            return Result.error("导入失败，请稍后重试");
        }
    }

    @PostMapping("/api/animation/episode-video")
    public Result<Map<String, String>> uploadEpisodeVideo(
            @RequestParam int animationId,
            @RequestParam int episodeNo,
            @RequestParam(required = false) String title,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = animationVideoUploadService.uploadPreviewVideo(file);
            animationService.upsertEpisodeVideo(animationId, episodeNo, title, url);
            return urlResult(url);
        } catch (IllegalArgumentException e) {
            log.warn("episode-video rejected: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("episode-video failed", e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    @PostMapping("/api/animation/episode-video/local")
    public Result<Map<String, String>> importEpisodeVideo(
            @RequestParam int animationId,
            @RequestParam int episodeNo,
            @RequestParam(required = false) String title,
            @RequestParam String sourcePath) {
        try {
            String url = animationVideoUploadService.importPreviewVideo(Path.of(sourcePath));
            animationService.upsertEpisodeVideo(animationId, episodeNo, title, url);
            return urlResult(url);
        } catch (IllegalArgumentException e) {
            log.warn("episode-video import rejected: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("episode-video import failed", e);
            return Result.error("导入失败，请稍后重试");
        }
    }

    private static Result<Map<String, String>> urlResult(String url) {
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }
}
