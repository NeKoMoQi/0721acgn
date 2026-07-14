package com.example.demo.service;

import com.example.demo.com.AcgnLibraryItemVo;
import com.example.demo.com.AcgnSubscribeItemVo;
import com.example.demo.com.AcgnUserBundleVo;
import com.example.demo.dto.AcgnMediaTouchDto;

import java.util.Map;

public interface AcgnUserDataService {

    AcgnUserBundleVo getBundle(String clientIdRaw);

    void recordVisit(String clientIdRaw, AcgnMediaTouchDto body);

    boolean toggleFavorite(String clientIdRaw, AcgnMediaTouchDto body);

    /** @return true when now subscribed after toggle */
    boolean toggleSubscribe(String clientIdRaw, AcgnMediaTouchDto body);

    void saveProgress(String clientIdRaw, String mediaType, int mediaId, Map<String, Object> payload);

    /** One-time migration from legacy localStorage array */
    void importLegacyRows(String clientIdRaw, java.util.List<AcgnLibraryItemVo> rows);
}
