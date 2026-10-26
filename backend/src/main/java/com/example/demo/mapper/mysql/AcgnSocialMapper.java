package com.example.demo.mapper.mysql;

import com.example.demo.com.AcgnSocialCommentRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AcgnSocialMapper {

    Integer selectMyVote(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId);

    int deleteMyVote(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId);

    int insertVote(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("vote") int vote);

    int updateMyVote(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("vote") int vote);

    int countVotes(
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("vote") int vote);

    List<AcgnSocialCommentRow> listComments(
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("limit") int limit);

    int insertComment(
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("nickname") String nickname,
            @Param("avatar") String avatar,
            @Param("body") String body);
}
