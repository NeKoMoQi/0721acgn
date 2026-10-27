package com.example.demo.service.agent;

import com.example.demo.dto.XunliAgentSpot;
import com.example.demo.service.XunliService;
import com.example.demo.util.XunliAgentSpotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Short-lived in-process catalogue; only compact, matched candidates enter the model context. */
@Component
public class PilgrimageCatalog {
    private final XunliService service;
    private final PilgrimageDataStore store;
    private List<Map<String, Object>> aliases = List.of();
    private final ObjectMapper json = new ObjectMapper();
    private volatile List<XunliAgentSpot> cached = List.of();
    private volatile long expiresAt;

    public PilgrimageCatalog(XunliService service, PilgrimageDataStore store) { this.service = service; this.store = store; }

    public synchronized List<XunliAgentSpot> all() {
        if (System.currentTimeMillis() < expiresAt) return cached;
        var points = service.getPickerXunliPointList();
        Map<String, Map<String, Object>> info = new LinkedHashMap<>();
        for (var row : store.spotInfo()) info.put(String.valueOf(row.get("id")), row);
        aliases = store.aliases();
        cached = points.stream().map(XunliAgentSpotMapper::fromPoint)
                .peek(p -> {
                    var row = info.get(p.getId());
                    if (row == null) return;
                    p.setAccessStatus(String.valueOf(row.getOrDefault("status", "unknown")));
                    p.setPhotoNote(text((String) row.get("note")));
                    p.setSourceUrl(text((String) row.get("source")));
                    p.setVerifiedAt((String) row.get("verified"));
                })
                .filter(p -> !List.of("closed", "restricted").contains(p.getAccessStatus()))
                .filter(p -> p.getId() != null && p.getLng() != null && p.getLat() != null)
                .filter(p -> p.getName() != null && !p.getName().isBlank()
                        && !p.getName().equals(p.getWorkTitle()) && !"站内巡礼点".equals(p.getName()))
                .toList();
        expiresAt = System.currentTimeMillis() + 60_000;
        return cached;
    }

    public ObjectNode search(JsonNode args, Map<String, XunliAgentSpot> allowed) {
        if (!args.path("keywords").isArray() || args.path("keywords").size() > 8
                || !args.path("city").isTextual() || args.path("city").asText().length() > 60)
            throw new IllegalArgumentException("检索参数需要 keywords 数组（最多8个短词）和 city 字符串");
        List<String> keywords = new ArrayList<>();
        for (JsonNode keyword : args.path("keywords")) {
            String value = keyword.asText().trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty() && value.length() <= 60 && keywords.size() < 8) keywords.add(value);
        }
        List<XunliAgentSpot> pool = all();
        String city = args.path("city").asText("").trim().toLowerCase(Locale.ROOT);
        List<String> cities = expand(city, "city");
        ObjectNode result = json.createObjectNode();
        if (keywords.isEmpty() && city.isEmpty()) {
            Map<String, Integer> works = new LinkedHashMap<>();
            for (var spot : pool) works.merge(text(spot.getWorkTitle()) + " · " + text(spot.getCity()), 1, Integer::sum);
            var entries = result.putArray("catalogue");
            works.entrySet().stream().limit(100).forEach(e -> entries.addObject()
                    .put("workAndCity", e.getKey()).put("points", e.getValue()));
            result.put("note", "这是作品目录，不是可提交地点。用关键词或城市检索具体地点后才能提交路线。");
            return result;
        }
        int offset = Math.max(0, Math.min(500, args.path("offset").asInt(0)));
        List<XunliAgentSpot> matches = pool.stream()
                .filter(p -> city.isEmpty() || cities.stream().anyMatch(c -> text(p.getCity()).toLowerCase(Locale.ROOT).contains(c)))
                .filter(p -> keywords.isEmpty() || keywords.stream().anyMatch(k -> matches(k, p)))
                .sorted(Comparator.comparing((XunliAgentSpot p) -> text(p.getCity()))
                        .thenComparing(p -> text(p.getWorkTitle())).thenComparing(XunliAgentSpot::getId))
                .toList();
        result.put("total", matches.size());
        var spots = result.putArray("spots");
        matches.stream().skip(offset).limit(32).forEach(p -> {
            if (allowed.size() >= 128 && !allowed.containsKey(p.getId())) return;
            allowed.put(p.getId(), p);
            spots.addObject().put("id", p.getId()).put("name", p.getName())
                    .put("workTitle", p.getWorkTitle()).put("city", p.getCity())
                    .put("lng", p.getLng()).put("lat", p.getLat())
                    .put("accessStatus", p.getAccessStatus()).put("photoNote", p.getPhotoNote())
                    .put("verifiedAt", p.getVerifiedAt());
        });
        result.put("nextOffset", offset + 32 < matches.size() ? offset + 32 : -1);
        result.put("note", "候选为站内数据，仅坐标可用于地理粗筛；开放时间、交通时间和取景准确性尚未核实。");
        return result;
    }

    private boolean matches(String keyword, XunliAgentSpot spot) {
        if (searchable(spot).contains(keyword)) return true;
        if (expand(keyword, "work").stream().anyMatch(k -> searchable(spot).contains(k))) return true;
        if (List.of("京阿尼", "京都动画", "kyoani").contains(keyword)) {
            String work = text(spot.getWorkTitle()).toLowerCase(Locale.ROOT);
            return List.of("clannad", "冰菓", "氷菓", "轻音", "けいおん", "k-on", "凉宫", "涼宮",
                    "玉子", "たまこ", "中二病", "free", "境界的彼方", "上低音", "響け", "紫罗兰", "ヴァイオレット", "甘城")
                    .stream().anyMatch(work::contains);
        }
        return false;
    }

    private String searchable(XunliAgentSpot p) {
        return (text(p.getName()) + " " + text(p.getWorkTitle()) + " " + text(p.getCity())).toLowerCase(Locale.ROOT);
    }
    private List<String> expand(String value, String kind) {
        List<String> values = new ArrayList<>();
        values.add(value);
        for (var row : aliases) if (kind.equals(row.get("kind")) && value.equals(String.valueOf(row.get("alias")).toLowerCase(Locale.ROOT)))
            values.add(String.valueOf(row.get("canonical")).toLowerCase(Locale.ROOT));
        return values;
    }
    private static String text(String value) { return value == null ? "" : value; }
}
