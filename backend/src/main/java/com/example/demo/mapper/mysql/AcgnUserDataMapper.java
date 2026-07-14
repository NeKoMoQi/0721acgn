package com.example.demo.mapper.mysql;

import com.example.demo.com.AcgnLibraryRow;
import com.example.demo.com.AcgnProgressRow;
import com.example.demo.com.AcgnSubscribeRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AcgnUserDataMapper {

    List<AcgnLibraryRow> listLibrary(@Param("clientId") String clientId);

    AcgnLibraryRow selectLibrary(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId);

    int insertLibrary(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("title") String title,
            @Param("pic") String pic,
            @Param("starred") int starred,
            @Param("starredAt") Long starredAt,
            @Param("lastVisitAt") long lastVisitAt);

    int upsertVisit(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("title") String title,
            @Param("pic") String pic,
            @Param("now") long now);

    int updateStar(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("starred") int starred,
            @Param("starredAt") Long starredAt);

    List<AcgnSubscribeRow> listSubscribe(@Param("clientId") String clientId);

    int insertSubscribe(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("title") String title,
            @Param("pic") String pic,
            @Param("subscribedAt") long subscribedAt);

    int deleteSubscribe(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId);

    int countSubscribe(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId);

    List<AcgnProgressRow> listProgress(@Param("clientId") String clientId);

    int upsertProgress(
            @Param("clientId") String clientId,
            @Param("mediaType") String mediaType,
            @Param("mediaId") int mediaId,
            @Param("payloadJson") String payloadJson,
            @Param("updatedAt") long updatedAt);
}
