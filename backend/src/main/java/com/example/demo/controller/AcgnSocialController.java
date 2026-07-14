package com.example.demo.controller;

import com.example.demo.com.AcgnEngagementVo;
import com.example.demo.com.AcgnSocialCommentVo;
import com.example.demo.com.Result;
import com.example.demo.dto.AcgnCommentPostDto;
import com.example.demo.dto.AcgnVotePostDto;
import com.example.demo.service.AcgnSocialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class AcgnSocialController {

    private final AcgnSocialService acgnSocialService;

    public AcgnSocialController(AcgnSocialService acgnSocialService) {
        this.acgnSocialService = acgnSocialService;
    }

    @GetMapping("/api/acgn/social/engagement")
    public Result<AcgnEngagementVo> engagement(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestParam String mediaType,
            @RequestParam int mediaId) {
        AcgnEngagementVo vo = acgnSocialService.getEngagement(clientId, mediaType, mediaId);
        return Result.success(vo);
    }

    @PostMapping("/api/acgn/social/vote")
    public Result<AcgnEngagementVo> vote(
            @RequestHeader(value = "X-Acgn-Client", required = false) String clientId,
            @RequestBody AcgnVotePostDto body) {
        try {
            AcgnEngagementVo vo =
                    acgnSocialService.voteToggle(clientId, body.getMediaType(), body.getMediaId(), body.getSide());
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            log.warn("vote: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/acgn/social/comments")
    public Result<List<AcgnSocialCommentVo>> comments(
            @RequestParam String mediaType,
            @RequestParam int mediaId) {
        return Result.success(acgnSocialService.listComments(mediaType, mediaId));
    }

    @PostMapping("/api/acgn/social/comments")
    public Result<Void> postComment(@RequestBody AcgnCommentPostDto body) {
        boolean ok =
                acgnSocialService.addComment(
                        body.getMediaType(), body.getMediaId(), body.getNickname(), body.getAvatar(), body.getBody());
        if (!ok) {
            return Result.error("发表评论失败（内容或题材不合法）");
        }
        return Result.success();
    }
}
