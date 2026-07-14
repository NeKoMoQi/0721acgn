package com.example.demo.mapper.mysql;

import com.example.demo.com.Animation;
import com.example.demo.com.AnimationEpisode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnimationMapper {
    List<Animation> getAnimation(
            @Param("offset") int offset,
            @Param("genre") String genre,
            @Param("status") String status,
            @Param("yearMin") Integer yearMin,
            @Param("yearMax") Integer yearMax,
            @Param("sortBy") String sortBy);
    List<Animation> getAnimationHot();
    Animation getAnimationbyid(int id);

    void updatePreviewVideoUrl(@Param("id") int id, @Param("url") String url);

    List<AnimationEpisode> listEpisodesByAnimationId(@Param("animationId") int animationId);

    AnimationEpisode getEpisodeById(@Param("id") int id);

    void upsertEpisodeVideo(
            @Param("animationId") int animationId,
            @Param("episodeNo") int episodeNo,
            @Param("title") String title,
            @Param("videoUrl") String videoUrl);

    List<Animation> listByExactTrimmedName(@Param("name") String name, @Param("excludeId") Integer excludeId);

    List<Animation> listSameSeriesCandidates(
            @Param("seriesPrefix") String seriesPrefix,
            @Param("seriesPrefixEsc") String seriesPrefixEsc,
            @Param("excludeId") Integer excludeId);
}
