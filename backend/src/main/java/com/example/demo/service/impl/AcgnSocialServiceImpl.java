package com.example.demo.service.impl;

import com.example.demo.com.AcgnEngagementVo;
import com.example.demo.com.AcgnSocialCommentRow;
import com.example.demo.com.AcgnSocialCommentVo;
import com.example.demo.mapper.mysql.AcgnSocialMapper;
import com.example.demo.service.AcgnSocialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AcgnSocialServiceImpl implements AcgnSocialService {

    private final AcgnSocialMapper mapper;

    public AcgnSocialServiceImpl(AcgnSocialMapper mapper) {
        this.mapper = mapper;
    }

    private static String normType(String mt) {
        if (mt == null) return "";
        return mt.trim().toLowerCase(Locale.ROOT);
    }

    private static String mapMyVote(Integer v) {
        if (v == null) return null;
        if (v == 1) return "up";
        if (v == -1) return "down";
        return null;
    }

    @Override
    public AcgnEngagementVo getEngagement(String clientId, String mediaType, int mediaId) {
        String mt = normType(mediaType);
        int likes = mapper.countVotes(mt, mediaId, 1);
        int dislikes = mapper.countVotes(mt, mediaId, -1);
        Integer mine = null;
        if (clientId != null && !clientId.isBlank()) {
            mine = mapper.selectMyVote(sanitizeClient(clientId), mt, mediaId);
        }
        return new AcgnEngagementVo(likes, dislikes, mapMyVote(mine));
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public AcgnEngagementVo voteToggle(String clientIdRaw, String mediaType, int mediaId, String side) {
        String clientId = sanitizeClient(clientIdRaw);
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("缺少匿名设备标识 X-Acgn-Client");
        }
        String mt = normType(mediaType);
        if (!mt.equals("animation") && !mt.equals("comic") && !mt.equals("novel")) {
            throw new IllegalArgumentException("mediaType 非法");
        }
        String s = side == null ? "" : side.trim().toLowerCase(Locale.ROOT);
        if (!s.equals("up") && !s.equals("down")) {
            throw new IllegalArgumentException("side 需为 up/down");
        }
        Integer cur = mapper.selectMyVote(clientId, mt, mediaId);
        int targetVote = s.equals("up") ? 1 : -1;
        applyToggle(clientId, mt, mediaId, cur, targetVote);
        return getEngagement(clientId, mt, mediaId);
    }

    private void applyToggle(String clientId, String mt, int mediaId, Integer cur, int targetVote) {
        if (cur == null) {
            mapper.insertVote(clientId, mt, mediaId, targetVote);
            return;
        }
        if (cur == targetVote) {
            mapper.deleteMyVote(clientId, mt, mediaId);
            return;
        }
        mapper.updateMyVote(clientId, mt, mediaId, targetVote);
    }

    private static String sanitizeClient(String cid) {
        if (cid == null) return "";
        String s = cid.trim();
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    @Override
    public List<AcgnSocialCommentVo> listComments(String mediaType, int mediaId) {
        String mt = normType(mediaType);
        List<AcgnSocialCommentRow> rows = mapper.listComments(mt, mediaId, 80);
        ZoneId z = ZoneId.systemDefault();
        return rows.stream()
        .map((r) -> {
            long ms = r.getCreatedAt() == null
                    ? System.currentTimeMillis()
                    : r.getCreatedAt().atZone(z).toInstant().toEpochMilli();
            return new AcgnSocialCommentVo(r.getNickname(), r.getAvatar(), r.getBody(), ms);
        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public boolean addComment(String mediaType, int mediaId, String nickname, String avatar, String body) {
        String mt = normType(mediaType);
        if (!mt.equals("animation") && !mt.equals("comic") && !mt.equals("novel")) {
            return false;
        }
        String b = body == null ? "" : body.trim();
        if (b.isEmpty() || b.length() > 480) {
            return false;
        }
        String nick = nickname == null || nickname.isBlank() ? "访客" : nickname.trim();
        if (nick.length() > 64) {
            nick = nick.substring(0, 64);
        }
        String av = avatar == null ? "" : avatar.trim();
        if (av.length() > 1024) {
            av = av.substring(0, 1024);
        }
        return mapper.insertComment(mt, mediaId, nick, av, b) > 0;
    }
}
