package com.example.demo.service;

import com.example.demo.com.PublicVideoClipVo;

import java.util.List;

/** 正版公开宣传片（预览/PV），按番剧条目返回可嵌入外链，不做正片爬虫。 */
public interface AnimationPublicClipsService {

    List<PublicVideoClipVo> listPublicClipsByAnimationId(int animationId);
}
