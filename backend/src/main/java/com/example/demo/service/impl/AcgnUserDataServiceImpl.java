package com.example.demo.service.impl;

import com.example.demo.com.AcgnLibraryItemVo;
import com.example.demo.com.AcgnLibraryRow;
import com.example.demo.com.AcgnProgressRow;
import com.example.demo.com.AcgnSubscribeItemVo;
import com.example.demo.com.AcgnSubscribeRow;
import com.example.demo.com.AcgnUserBundleVo;
import com.example.demo.dto.AcgnMediaTouchDto;
import com.example.demo.mapper.mysql.AcgnUserDataMapper;
import com.example.demo.service.AcgnUserDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AcgnUserDataServiceImpl implements AcgnUserDataService {

    private final AcgnUserDataMapper mapper;
    private final ObjectMapper objectMapper;

    public AcgnUserDataServiceImpl(AcgnUserDataMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    private static String sanitizeClient(String cid) {
        if (cid == null) return "";
        String s = cid.trim();
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static String normType(String mediaType) {
        if (mediaType == null) return "";
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private void requireLegalMedia(String mt, int mid) {
        if (!mt.equals("animation") && !mt.equals("comic") && !mt.equals("novel")) {
            throw new IllegalArgumentException("mediaType 非法");
        }
        if (mid <= 0) {
            throw new IllegalArgumentException("mediaId 非法");
        }
    }

    @Override
    public AcgnUserBundleVo getBundle(String clientIdRaw) {
        String cid = sanitizeClient(clientIdRaw);
        AcgnUserBundleVo bundle = new AcgnUserBundleVo();
        bundle.setLibrary(List.of());
        bundle.setSubscriptions(List.of());
        bundle.setProgress(new HashMap<>());
        if (cid.isBlank()) {
            return bundle;
        }

        bundle.setLibrary(
                mapper.listLibrary(cid).stream()
                        .map(this::libraryToVo)
                        .collect(Collectors.toList()));

        bundle.setSubscriptions(
                mapper.listSubscribe(cid).stream()
                        .map(this::subscribeToVo)
                        .collect(Collectors.toList()));

        bundle.setProgress(buildProgressMap(cid));
        return bundle;
    }

    private AcgnLibraryItemVo libraryToVo(AcgnLibraryRow r) {
        return new AcgnLibraryItemVo(
                r.getMediaType(),
                r.getMediaId(),
                r.getTitle() == null ? "" : r.getTitle(),
                r.getPic(),
                r.getLastVisitAt(),
                r.getStarred() >= 1,
                r.getStarredAt());
    }

    private AcgnSubscribeItemVo subscribeToVo(AcgnSubscribeRow r) {
        return new AcgnSubscribeItemVo(
                r.getMediaType(),
                r.getMediaId(),
                r.getTitle() == null ? "" : r.getTitle(),
                r.getPic(),
                r.getSubscribedAt());
    }

    private Map<String, Object> buildProgressMap(String clientId) {
        Map<String, Object> outer = new HashMap<>();
        Map<String, Integer> anim = new HashMap<>();
        Map<String, Map<String, Object>> comic = new HashMap<>();
        Map<String, Map<String, Integer>> nov = new HashMap<>();

        List<AcgnProgressRow> rows = mapper.listProgress(clientId);
        for (AcgnProgressRow pr : rows) {
            Map<String, Object> pay;
            try {
                pay =
                        objectMapper.readValue(
                                pr.getPayloadJson() == null ? "{}" : pr.getPayloadJson(),
                                new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                continue;
            }
            String key = Integer.toString(pr.getMediaId());
            String mt = normType(pr.getMediaType());
            if (mt.equals("animation")) {
                Object ep = pay.get("episode");
                Integer n = null;
                if (ep instanceof Number) {
                    n = ((Number) ep).intValue();
                } else if (ep != null) {
                    try {
                        n = Integer.parseInt(String.valueOf(ep));
                    } catch (NumberFormatException ignored) {
                        //
                    }
                }
                if (n == null || n < 0) {
                    continue;
                }
                anim.put(key, n);
            } else if (mt.equals("comic")) {
                comic.put(key, pay);
            } else if (mt.equals("novel")) {
                Object ch = pay.get("chapterno");
                int cn =
                        ch instanceof Number
                                ? ((Number) ch).intValue()
                                : Integer.parseInt(String.valueOf(ch));
                nov.put(key, Map.of("chapterno", cn));
            }
        }
        outer.put("animation", anim);
        outer.put("comic", comic);
        outer.put("novel", nov);
        return outer;
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public void recordVisit(String clientIdRaw, AcgnMediaTouchDto body) {
        String cid = sanitizeClient(clientIdRaw);
        if (cid.isBlank()) {
            throw new IllegalArgumentException("缺少匿名设备标识 X-Acgn-Client");
        }
        String mt = normType(body.getMediaType());
        int mid =
                body.getMediaId() == null ? -1 : body.getMediaId();
        requireLegalMedia(mt, mid);
        long now = System.currentTimeMillis();
        mapper.upsertVisit(cid, mt, mid, body.getName(), body.getPic(), now);
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public boolean toggleFavorite(String clientIdRaw, AcgnMediaTouchDto body) {
        String cid = sanitizeClient(clientIdRaw);
        if (cid.isBlank()) {
            throw new IllegalArgumentException("缺少匿名设备标识 X-Acgn-Client");
        }
        String mt = normType(body.getMediaType());
        int mid =
                body.getMediaId() == null ? -1 : body.getMediaId();
        requireLegalMedia(mt, mid);
        long now = System.currentTimeMillis();
        AcgnLibraryRow row = mapper.selectLibrary(cid, mt, mid);
        if (row == null) {
            mapper.insertLibrary(cid, mt, mid, body.getName(), body.getPic(), 1, now, now);
            return true;
        }
        int nextStar = row.getStarred() >= 1 ? 0 : 1;
        Long starAt = nextStar >= 1 ? now : null;
        mapper.updateStar(cid, mt, mid, nextStar, starAt);
        return nextStar >= 1;
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public boolean toggleSubscribe(String clientIdRaw, AcgnMediaTouchDto body) {
        String cid = sanitizeClient(clientIdRaw);
        if (cid.isBlank()) {
            throw new IllegalArgumentException("缺少匿名设备标识 X-Acgn-Client");
        }
        String mt = normType(body.getMediaType());
        int mid =
                body.getMediaId() == null ? -1 : body.getMediaId();
        requireLegalMedia(mt, mid);
        int had = mapper.countSubscribe(cid, mt, mid);
        long now = System.currentTimeMillis();
        if (had > 0) {
            mapper.deleteSubscribe(cid, mt, mid);
            return false;
        }
        mapper.insertSubscribe(cid, mt, mid, body.getName(), body.getPic(), now);
        return true;
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public void saveProgress(String clientIdRaw, String mediaType, int mediaId, Map<String, Object> payload) {
        String cid = sanitizeClient(clientIdRaw);
        if (cid.isBlank()) {
            throw new IllegalArgumentException("缺少匿名设备标识 X-Acgn-Client");
        }
        String mt = normType(mediaType);
        requireLegalMedia(mt, mediaId);
        try {
            String json = payload == null ? "{}" : objectMapper.writeValueAsString(payload);
            mapper.upsertProgress(cid, mt, mediaId, json, System.currentTimeMillis());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("进度载荷无法序列化");
        }
    }

    @Override
    @Transactional(transactionManager = "mysqlTransactionManager")
    public void importLegacyRows(String clientIdRaw, List<AcgnLibraryItemVo> rows) {
        String cid = sanitizeClient(clientIdRaw);
        if (cid.isBlank() || rows == null || rows.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (AcgnLibraryItemVo r : rows) {
            String mt = normType(r.getType());
            try {
                requireLegalMedia(mt, r.getId());
            } catch (IllegalArgumentException e) {
                continue;
            }
            long visitAt = r.getAt() > 0 ? r.getAt() : now;
            AcgnLibraryRow ex = mapper.selectLibrary(cid, mt, r.getId());
            if (ex == null) {
                Long starredAtTs =
                        r.isStarred()
                                ? Long.valueOf(
                                        r.getStarredAt() != null && r.getStarredAt() > 0
                                                ? r.getStarredAt()
                                                : visitAt)
                                : null;
                mapper.insertLibrary(
                        cid,
                        mt,
                        r.getId(),
                        r.getName(),
                        r.getPic(),
                        r.isStarred() ? 1 : 0,
                        starredAtTs,
                        visitAt);
            } else {
                long mergedVisit = Math.max(visitAt, ex.getLastVisitAt());
                mapper.upsertVisit(cid, mt, r.getId(), r.getName(), r.getPic(), mergedVisit);
                if (r.isStarred()) {
                    long starTm =
                            r.getStarredAt() != null && r.getStarredAt() > 0
                                    ? r.getStarredAt()
                                    : mergedVisit;
                    mapper.updateStar(cid, mt, r.getId(), 1, starTm);
                }
            }
        }
    }
}
