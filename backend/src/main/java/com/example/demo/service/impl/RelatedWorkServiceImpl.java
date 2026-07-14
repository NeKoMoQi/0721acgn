package com.example.demo.service.impl;

import com.example.demo.com.Animation;
import com.example.demo.com.Cartoons;
import com.example.demo.com.Novel;
import com.example.demo.com.RelatedLink;
import com.example.demo.com.RelatedWorkVo;
import com.example.demo.mapper.mysql.AnimationMapper;
import com.example.demo.mapper.mysql.CartoonMapper;
import com.example.demo.mapper.mysql.NovelMapper;
import com.example.demo.mapper.mysql.RelatedWorkMapper;
import com.example.demo.service.RelatedWorkService;
import com.example.demo.util.RelatedSeriesTitleUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RelatedWorkServiceImpl implements RelatedWorkService {

    private final RelatedWorkMapper relatedWorkMapper;
    private final AnimationMapper animationMapper;
    private final CartoonMapper cartoonMapper;
    private final NovelMapper novelMapper;

    public RelatedWorkServiceImpl(
            RelatedWorkMapper relatedWorkMapper,
            AnimationMapper animationMapper,
            CartoonMapper cartoonMapper,
            NovelMapper novelMapper) {
        this.relatedWorkMapper = relatedWorkMapper;
        this.animationMapper = animationMapper;
        this.cartoonMapper = cartoonMapper;
        this.novelMapper = novelMapper;
    }

    @Override
    public List<RelatedWorkVo> listBySource(String srcType, int srcId) {
        String st = srcType.trim().toLowerCase(Locale.ROOT);
        List<RelatedWorkVo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        List<RelatedLink> links = relatedWorkMapper.listBySource(st, srcId);
        for (RelatedLink link : links) {
            RelatedWorkVo vo = resolve(link.getDstType(), link.getDstId());
            if (vo != null && seen.add(key(vo))) {
                out.add(vo);
            }
        }

        String title = resolveSourceTitle(st, srcId);
        if (title != null && !title.isBlank()) {
            String t = title.trim();
            appendSameNameWorks(t, st, srcId, out, seen);
            appendSameSeriesPeers(t, st, srcId, out, seen);
        }
        return out;
    }

    /**
     * 同一 IP：题名以系列主名为前缀（含多季、各配音版、代号：白 等），不再要求归一化题名完全一致。
     */
    private void appendSameSeriesPeers(
            String title, String st, int srcId, List<RelatedWorkVo> out, Set<String> seen) {
        String seriesKey = RelatedSeriesTitleUtil.baseSeriesTitle(title);
        if (seriesKey.length() < 3) {
            return;
        }
        String seriesKeyEsc = RelatedSeriesTitleUtil.escapeLike(seriesKey);
        Integer exAnim = "animation".equals(st) ? srcId : null;
        Integer exComic = "comic".equals(st) ? srcId : null;
        Integer exNovel = "novel".equals(st) ? srcId : null;

        for (Animation a : animationMapper.listSameSeriesCandidates(seriesKey, seriesKeyEsc, exAnim)) {
            if (!RelatedSeriesTitleUtil.isSameIpFamily(a.getName(), seriesKey)) {
                continue;
            }
            RelatedWorkVo vo = new RelatedWorkVo("animation", a.getId(), a.getName(), a.getBcimg());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
        for (Cartoons c : cartoonMapper.listSameSeriesCandidates(seriesKey, seriesKeyEsc, exComic)) {
            if (!RelatedSeriesTitleUtil.isSameIpFamily(c.getName(), seriesKey)) {
                continue;
            }
            RelatedWorkVo vo = new RelatedWorkVo("comic", c.getComicid(), c.getName(), c.getPicurl());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
        for (Novel n : novelMapper.listSameSeriesCandidates(seriesKey, seriesKeyEsc, exNovel)) {
            if (!RelatedSeriesTitleUtil.isSameIpFamily(n.getName(), seriesKey)) {
                continue;
            }
            RelatedWorkVo vo = new RelatedWorkVo("novel", n.getNovelid(), n.getName(), n.getPicurl());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
    }

    private void appendSameNameWorks(
            String title, String st, int srcId, List<RelatedWorkVo> out, Set<String> seen) {
        Integer exAnim = "animation".equals(st) ? srcId : null;
        Integer exComic = "comic".equals(st) ? srcId : null;
        Integer exNovel = "novel".equals(st) ? srcId : null;

        for (Animation a : animationMapper.listByExactTrimmedName(title, exAnim)) {
            RelatedWorkVo vo = new RelatedWorkVo("animation", a.getId(), a.getName(), a.getBcimg());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
        for (Cartoons c : cartoonMapper.listByExactTrimmedName(title, exComic)) {
            RelatedWorkVo vo = new RelatedWorkVo("comic", c.getComicid(), c.getName(), c.getPicurl());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
        for (Novel n : novelMapper.listByExactTrimmedName(title, exNovel)) {
            RelatedWorkVo vo = new RelatedWorkVo("novel", n.getNovelid(), n.getName(), n.getPicurl());
            if (seen.add(key(vo))) {
                out.add(vo);
            }
        }
    }

    private String resolveSourceTitle(String st, int srcId) {
        return switch (st) {
            case "animation" -> {
                Animation a = animationMapper.getAnimationbyid(srcId);
                yield a != null ? a.getName() : null;
            }
            case "comic" -> {
                Cartoons c = cartoonMapper.getCartoonsbyid(srcId);
                yield c != null ? c.getName() : null;
            }
            case "novel" -> {
                Novel n = novelMapper.getNovelById(srcId);
                yield n != null ? n.getName() : null;
            }
            default -> null;
        };
    }

    private static String key(RelatedWorkVo vo) {
        return vo.getType().toLowerCase(Locale.ROOT) + ":" + vo.getId();
    }

    private RelatedWorkVo resolve(String dstType, int dstId) {
        if (dstType == null) {
            return null;
        }
        switch (dstType.trim().toLowerCase(Locale.ROOT)) {
            case "animation" -> {
                Animation a = animationMapper.getAnimationbyid(dstId);
                if (a == null) {
                    return null;
                }
                return new RelatedWorkVo("animation", a.getId(), a.getName(), a.getBcimg());
            }
            case "comic" -> {
                Cartoons c = cartoonMapper.getCartoonsbyid(dstId);
                if (c == null) {
                    return null;
                }
                return new RelatedWorkVo("comic", c.getComicid(), c.getName(), c.getPicurl());
            }
            case "novel" -> {
                Novel n = novelMapper.getNovelById(dstId);
                if (n == null) {
                    return null;
                }
                return new RelatedWorkVo("novel", n.getNovelid(), n.getName(), n.getPicurl());
            }
            default -> {
                return null;
            }
        }
    }
}
