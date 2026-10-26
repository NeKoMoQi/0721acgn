package com.example.demo.service;

import com.example.demo.com.Animation;
import com.example.demo.com.AnimationEpisode;

import java.util.List;

public interface AnimationService {
    List<Animation> getAnimationPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy);
    List<Animation> getAnimationHot();
    Animation getAnimationbyid(int id);

    void updatePreviewVideoUrl(int id, String url);

    List<AnimationEpisode> listEpisodesByAnimationId(int animationId);

    AnimationEpisode getEpisodeById(int id);

    void upsertEpisodeVideo(int animationId, int episodeNo, String title, String videoUrl);
}
