package com.example.demo.service.agent;

import com.example.demo.dto.XunliAgentSpot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** A model proposes IDs; only this validator creates a map-importable plan. */
@Component
public class PilgrimagePlanValidator {
    private final ObjectMapper json = new ObjectMapper();

    public ObjectNode validate(JsonNode proposal, Map<String, XunliAgentSpot> allowed) {
        String mode = proposal.path("travelMode").asText();
        if (!List.of("walking", "cycling", "driving").contains(mode))
            throw new IllegalArgumentException("只支持 walking/cycling/driving，公共交通尚未接入，不能假装已算出电车路线");
        JsonNode days = proposal.path("days");
        if (!days.isArray() || days.isEmpty() || days.size() > 10)
            throw new IllegalArgumentException("请提交 1～10 天的安排，过长行程分批规划");
        ObjectNode plan = json.createObjectNode().put("intent", "shengdi_route_plan")
                .put("version", 2).put("travelMode", mode);
        var output = plan.putArray("days");
        int number = 0;
        for (JsonNode day : days) {
            JsonNode ids = day.path("pointIds");
            if (!ids.isArray() || ids.isEmpty() || ids.size() > 6)
                throw new IllegalArgumentException("每天应包含 1～6 个具体地点，不要静默丢弃或截断地点");
            ObjectNode target = output.addObject().put("day", ++number)
                    .put("title", limited(day.path("title").asText("巡礼路线"), 100))
                    .put("note", limited(day.path("note").asText(""), 500));
            var points = target.putArray("pointIds");
            var spots = target.putArray("spots");
            var warnings = target.putArray("warnings");
            HashSet<String> seen = new HashSet<>();
            XunliAgentSpot previous = null;
            double distance = 0;
            for (JsonNode idNode : ids) {
                if (!idNode.isTextual()) throw new IllegalArgumentException("pointIds 必须是检索工具返回的字符串 ID");
                String id = idNode.asText();
                XunliAgentSpot spot = allowed.get(id);
                if (spot == null) throw new IllegalArgumentException("地点 ID " + limited(id, 60) + " 不在已检索候选中，请先检索，不能编造地点");
                if (List.of("closed", "restricted").contains(spot.getAccessStatus()))
                    throw new IllegalArgumentException("地点已标记为关闭或限制进入，请更换地点");
                if (spot.getLng() == null || spot.getLat() == null || !Double.isFinite(spot.getLng())
                        || !Double.isFinite(spot.getLat()) || Math.abs(spot.getLng()) > 180 || Math.abs(spot.getLat()) > 90)
                    throw new IllegalArgumentException("地点坐标无效，请更换地点");
                String coordinates = String.format(Locale.ROOT, "%.5f,%.5f", spot.getLng(), spot.getLat());
                if (!seen.add(coordinates)) throw new IllegalArgumentException("同一天含重复坐标，请合并同地点的截图后重新提交");
                if (previous != null) {
                    double km = distance(previous, spot);
                    double maximum = "walking".equals(mode) ? 20 : "cycling".equals(mode) ? 80 : 600;
                    if (km > maximum) throw new IllegalArgumentException("第 " + number + " 天有不合理的长距离移动（直线约 "
                            + Math.round(km) + " 公里），请更换出行方式、减少地点或拆成不同天");
                    distance += km;
                }
                ObjectNode enriched = json.valueToTree(spot);
                enriched.put("dbPointId", id);
                enriched.set("coordinates", json.valueToTree(List.of(spot.getLng(), spot.getLat())));
                spots.add(enriched);
                points.add(id);
                previous = spot;
            }
            target.put("straightLineKm", Math.round(distance * 10) / 10.0);
            if (distance > ("walking".equals(mode) ? 10 : "cycling".equals(mode) ? 40 : 250))
                warnings.add("当天移动距离较长；直线距离只是下限，请结合地图实际路程调整。");
            warnings.add("开放状态、拍摄限制和交通班次未实时核验；实际路程以地图结果为准。");
        }
        return plan;
    }

    private static String limited(String text, int max) { return text.substring(0, Math.min(text.length(), max)); }

    private double distance(XunliAgentSpot a, XunliAgentSpot b) {
        double lat = Math.toRadians(b.getLat() - a.getLat());
        double lng = Math.toRadians(b.getLng() - a.getLng());
        double h = Math.pow(Math.sin(lat / 2), 2) + Math.cos(Math.toRadians(a.getLat()))
                * Math.cos(Math.toRadians(b.getLat())) * Math.pow(Math.sin(lng / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }
}
