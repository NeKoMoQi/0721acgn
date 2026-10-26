package com.example.demo.service.impl;

import com.example.demo.com.Animation;
import com.example.demo.com.AnimationEpisode;

import com.example.demo.mapper.mysql.AnimationMapper;
import com.example.demo.service.AnimationService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AnimationServiceImpl implements AnimationService {
    private AnimationMapper animationMapper;
    public AnimationServiceImpl(AnimationMapper animationMapper) {
        this.animationMapper = animationMapper;
    }
    public List<Animation> getAnimationPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy) {
        return animationMapper.getAnimation(offset, genre, status, yearMin, yearMax, sortBy);
    }
    public List<Animation> getAnimationHot() {return animationMapper.getAnimationHot();}
    public Animation getAnimationbyid(int id){return animationMapper.getAnimationbyid(id);}

    @Override
    public void updatePreviewVideoUrl(int id, String url) {
        animationMapper.updatePreviewVideoUrl(id, url);
    }

    @Override
    public List<AnimationEpisode> listEpisodesByAnimationId(int animationId) {
        return animationMapper.listEpisodesByAnimationId(animationId);
    }

    @Override
    public AnimationEpisode getEpisodeById(int id) {
        return animationMapper.getEpisodeById(id);
    }

    @Override
    public void upsertEpisodeVideo(int animationId, int episodeNo, String title, String videoUrl) {
        String effectiveTitle = (title == null || title.isBlank()) ? "第 " + episodeNo + " 集" : title.trim();
        animationMapper.upsertEpisodeVideo(animationId, episodeNo, effectiveTitle, videoUrl);
    }
}
