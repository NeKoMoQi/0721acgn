package com.example.demo.service;

import com.example.demo.com.AcgnEngagementVo;
import com.example.demo.com.AcgnSocialCommentVo;

import java.util.List;

public interface AcgnSocialService {

    AcgnEngagementVo getEngagement(String clientId, String mediaType, int mediaId);

    AcgnEngagementVo voteToggle(String clientId, String mediaType, int mediaId, String side);

    List<AcgnSocialCommentVo> listComments(String mediaType, int mediaId);

    boolean addComment(String mediaType, int mediaId, String nickname, String avatar, String body);
}
