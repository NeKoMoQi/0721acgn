package com.example.demo.service.impl;

import com.example.demo.com.Animation;
import com.example.demo.com.PublicVideoClipVo;
import com.example.demo.mapper.mysql.AnimationMapper;
import com.example.demo.service.AnimationPublicClipsService;
import com.example.demo.util.RelatedSeriesTitleUtil;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AnimationPublicClipsServiceImpl implements AnimationPublicClipsService {

    /** 系列主标题，与「间谍过家家」各版题名匹配时使用 */
    private static final String SPY_FAMILY_SERIES_ROOT = "间谍过家家";

    /**
     * 公开可嵌入的 YouTube PV/先导片 ID（正版宣传片，请在页面注明来源）。
     * 不涉及整集或未授权转载。
     */
    private static final List<PublicVideoClipVo> SPY_FAMILY_CLIPS = List.of(
            new PublicVideoClipVo("SPY×FAMILY MAIN TRAILER", "YouTube / 官方先导", "ofXigq9aIpo"),
            new PublicVideoClipVo("SPY×FAMILY — OFFICIAL TRAILER", "YouTube / 预告", "Zk4pENWxhaM"),
            new PublicVideoClipVo("CODE: White | Official Trailer", "YouTube / 剧场版预告", "boLFW-C1wF4"),
            new PublicVideoClipVo("SPY×FAMILY | PV", "YouTube / PV（示例）", "WmQVMeNgV8k")
    );

    private final AnimationMapper animationMapper;

    public AnimationPublicClipsServiceImpl(AnimationMapper animationMapper) {
        this.animationMapper = animationMapper;
    }

    @Override
    public List<PublicVideoClipVo> listPublicClipsByAnimationId(int animationId) {
        Animation a = animationMapper.getAnimationbyid(animationId);
        if (a == null || a.getName() == null) {
            return Collections.emptyList();
        }
        String name = a.getName().trim();
        if (RelatedSeriesTitleUtil.isSameIpFamily(name, SPY_FAMILY_SERIES_ROOT)
                || SPY_FAMILY_SERIES_ROOT.equals(RelatedSeriesTitleUtil.baseSeriesTitle(name))) {
            return SPY_FAMILY_CLIPS;
        }
        return Collections.emptyList();
    }
}
