package com.example.demo.service;

import com.example.demo.dto.Live2dAgentChatRequest;
import com.example.demo.dto.Live2dAgentChatResponse;
import com.example.demo.dto.Live2dAgentLive2dHints;
import com.example.demo.dto.XunliAgentSpot;
import com.example.demo.util.XunliAgentSpotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Live2dAgentService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XunliService xunliService;

    @Value("${live2d.agent.appbuilder.url:https://qianfan.baidubce.com/v2/app/conversation/runs}")
    private String qianfanUrl;

    @Value("${live2d.agent.appbuilder.app-id:}")
    private String appId;

    @Value("${live2d.agent.appbuilder.token:}")
    private String appBuilderToken;

    public Live2dAgentService(XunliService xunliService) {
        this.xunliService = xunliService;
    }

    public Live2dAgentChatResponse chat(Live2dAgentChatRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        if (appId == null || appId.isBlank() || appBuilderToken == null || appBuilderToken.isBlank()) {
            return buildUnconfiguredFallback(request.getQuery(), request.getConversationId());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        String query = request.getQuery();
        body.put("app_id", appId);
        body.put("query", enrichRouteQuery(query));
        body.put("stream", false);
        if (!shouldStartFreshConversation(query)
                && request.getConversationId() != null && !request.getConversationId().isBlank()) {
            body.put("conversation_id", request.getConversationId());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(appBuilderToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        final String raw;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    qianfanUrl, HttpMethod.POST, entity, String.class);
            raw = response.getBody();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Qianfan API error HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(),
                    e);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(raw != null ? raw : "{}");
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON from Qianfan", e);
        }

        JsonNode assistantPayload = extractAssistantPayload(root);
        Live2dAgentChatResponse out = new Live2dAgentChatResponse();
        String reply = extractReply(root, assistantPayload);
        JsonNode routePlan = extractRoutePlan(root, assistantPayload, reply);
        out.setReply(stripRoutePlanFromReply(reply, routePlan));
        if (root.hasNonNull("conversation_id")) {
            out.setConversationId(root.get("conversation_id").asText());
        }
        out.setLive2d(extractLive2dHints(root, assistantPayload, out.getReply()));
        out.setRoutePlan(routePlan);

        return out;
    }

    private Live2dAgentChatResponse buildUnconfiguredFallback(String query, String conversationId) {
        Live2dAgentChatResponse out = new Live2dAgentChatResponse();
        out.setConversationId(conversationId);
        out.setReply(buildFallbackReply(query));
        out.setLive2d(guessLive2dHints(out.getReply()));
        out.setRoutePlan(null);
        return out;
    }

    private String enrichRouteQuery(String query) {
        if (!isRouteRequest(query)) {
            return query;
        }

        List<XunliAgentSpot> concreteSpots;
        try {
            concreteSpots = xunliService.getPickerXunliPointList()
                    .stream()
                    .map(XunliAgentSpotMapper::fromPoint)
                    .filter(this::isNavigableSpot)
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return query;
        }

        if (concreteSpots.isEmpty()) {
            return query;
        }

        List<XunliAgentSpot> selected = selectRouteContextSpots(query, concreteSpots);
        if (selected.isEmpty()) {
            selected = concreteSpots.stream()
                    .filter(this::hasImage)
                    .limit(80)
                    .collect(Collectors.toList());
        }
        if (selected.isEmpty()) {
            selected = concreteSpots.stream().limit(80).collect(Collectors.toList());
        }

        StringBuilder context = new StringBuilder(query);
        context.append("\n\n【站内具体巡礼点候选清单】\n");
        context.append("下面每一行都是小窝数据库里可导航的具体巡礼点，格式为：id | workTitle | name | city/address | imageUrl。\n");
        context.append("你只能从这份清单里选择进入路线的点；不要使用作品名、线路名、泛地点、未在清单中的点作为 pointIds。\n");
        context.append("聊天正文里的每一站必须用清单中的具体 name，并紧跟展示对应 imageUrl；如果清单里有图，不要写“暂时没有站内图片”。\n");
        context.append("ROUTE_PLAN 的 days[].pointIds 和 endPointId 必须使用清单里的 id，禁止使用地点名当 ID。\n");
        context.append("多日巡礼每天尽量选 3 到 5 个具体巡礼点；单日不足时也要优先用清单具体点，不要退化成作品级路线。\n");
        context.append("不要在正文里宣称“共多少个点/扩充到多少个点”，点数统计由前端按 ROUTE_PLAN 实际导入结果显示。\n");
        if (isBroadKyoAniQuery(query)) {
            context.append("候选清单作品覆盖统计：").append(buildWorkCoverageStats(selected)).append('\n');
            context.append("这是广义京阿尼巡礼请求：必须优先覆盖多个不同 workTitle，不要让 CLANNAD 或任何单一作品占满全部天数；只有清单里确实只有单一作品时才说明原因。\n");
            context.append("不要主动选择 DVD 表纸、OP 画面、重复补拍这类低优先级点，除非用户明确要求封面/OP 巡礼。\n");
        }
        for (XunliAgentSpot spot : selected) {
            context.append(spot.getId()).append(" | ")
                    .append(safeText(spot.getWorkTitle())).append(" | ")
                    .append(safeText(spot.getName())).append(" | ")
                    .append(safeText(firstNonBlank(spot.getCity(), spot.getAddress()))).append(" | ")
                    .append(safeText(spot.getImageUrl())).append('\n');
        }
        return context.toString();
    }

    private String buildWorkCoverageStats(List<XunliAgentSpot> spots) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (XunliAgentSpot spot : spots) {
            String work = safeText(spot.getWorkTitle());
            if (work.isBlank()) {
                work = "其他";
            }
            counts.put(work, counts.getOrDefault(work, 0) + 1);
        }
        return counts.entrySet().stream()
                .map((entry) -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private List<XunliAgentSpot> selectRouteContextSpots(String query, List<XunliAgentSpot> spots) {
        List<XunliAgentSpot> withImages = spots.stream()
                .filter(this::hasImage)
                .collect(Collectors.toList());
        List<XunliAgentSpot> pool = withImages.isEmpty() ? spots : withImages;

        if (isBroadKyoAniQuery(query)) {
            List<XunliAgentSpot> kyoAniSpots = pool.stream()
                    .filter(this::matchesKyoAniSpot)
                    .sorted(routeContextComparator(query))
                    .collect(Collectors.toList());
            if (!kyoAniSpots.isEmpty()) {
                return balanceByWorkTitle(kyoAniSpots, 80, 8);
            }
        }

        List<XunliAgentSpot> matched = pool.stream()
                .filter((spot) -> matchesQuerySpot(query, spot))
                .sorted(routeContextComparator(query))
                .limit(60)
                .collect(Collectors.toList());

        if (matched.size() >= 12) {
            return matched;
        }

        List<XunliAgentSpot> broader = pool.stream()
                .filter((spot) -> isKyoAniQuery(query) && matchesKyoAniSpot(spot))
                .sorted(routeContextComparator(query))
                .limit(60)
                .collect(Collectors.toList());
        if (broader.size() >= 12) {
            return broader;
        }

        List<XunliAgentSpot> merged = new ArrayList<>(matched);
        pool.stream()
                .sorted(routeContextComparator(query))
                .limit(60)
                .forEach((spot) -> {
                    if (merged.stream().noneMatch((item) -> safeText(item.getId()).equals(safeText(spot.getId())))) {
                        merged.add(spot);
                    }
                });
        return merged.stream().limit(60).collect(Collectors.toList());
    }

    private List<XunliAgentSpot> balanceByWorkTitle(List<XunliAgentSpot> spots, int limit, int maxPerWork) {
        Map<String, List<XunliAgentSpot>> grouped = new LinkedHashMap<>();
        for (XunliAgentSpot spot : spots) {
            String work = safeText(spot.getWorkTitle());
            if (work.isBlank()) {
                work = "其他";
            }
            grouped.computeIfAbsent(work, (key) -> new ArrayList<>()).add(spot);
        }

        List<XunliAgentSpot> balanced = new ArrayList<>();
        int round = 0;
        boolean added;
        do {
            added = false;
            for (List<XunliAgentSpot> group : grouped.values()) {
                if (round < group.size() && round < maxPerWork && balanced.size() < limit) {
                    balanced.add(group.get(round));
                    added = true;
                }
            }
            round++;
        } while (added && balanced.size() < limit);

        if (balanced.size() < Math.min(limit, spots.size())) {
            for (XunliAgentSpot spot : spots) {
                if (balanced.size() >= limit) {
                    break;
                }
                String id = safeText(spot.getId());
                if (balanced.stream().noneMatch((item) -> safeText(item.getId()).equals(id))) {
                    balanced.add(spot);
                }
            }
        }
        return balanced;
    }

    private Comparator<XunliAgentSpot> routeContextComparator(String query) {
        return Comparator
                .comparing((XunliAgentSpot spot) -> !matchesQuerySpot(query, spot))
                .thenComparing((XunliAgentSpot spot) -> !matchesKyoAniSpot(spot))
                .thenComparing((XunliAgentSpot spot) -> safeText(spot.getWorkTitle()))
                .thenComparing((XunliAgentSpot spot) -> safeText(spot.getName()));
    }

    private boolean isRouteRequest(String query) {
        return containsAny(query,
                "圣地", "巡礼", "导航", "路线", "途经点", "驾驶", "开车", "自驾",
                "京阿尼", "京都动画", "CLANNAD", "冰菓", "轻音", "凉宫", "上低音号");
    }

    private boolean shouldStartFreshConversation(String query) {
        return isBroadKyoAniQuery(query) && containsAny(query, "五天", "5天", "多日", "重新", "重排", "规划", "路线");
    }

    private boolean matchesQuerySpot(String query, XunliAgentSpot spot) {
        String normalizedQuery = safeText(query).toLowerCase(Locale.ROOT);
        String text = (safeText(spot.getWorkTitle()) + " " + safeText(spot.getName()) + " "
                + safeText(spot.getCity()) + " " + safeText(spot.getAddress())).toLowerCase(Locale.ROOT);
        if (isKyoAniQuery(query) && matchesKyoAniSpot(spot)) {
            return true;
        }
        for (String token : normalizedQuery.split("[\\s,，。/、]+")) {
            if (token.length() >= 2 && text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKyoAniQuery(String query) {
        return containsAny(query,
                "京阿尼", "京都动画", "京都アニメーション", "kyoani",
                "CLANNAD", "凉宫", "涼宮", "轻音", "けいおん", "K-ON",
                "冰菓", "氷菓", "玉子市场", "たまこ", "中二病", "Free",
                "境界的彼方", "吹响", "響け", "上低音号", "紫罗兰", "ヴァイオレット", "甘城");
    }

    private boolean isBroadKyoAniQuery(String query) {
        return containsAny(query, "京阿尼", "京都动画", "京都アニメーション", "kyoani");
    }

    private boolean matchesKyoAniSpot(XunliAgentSpot spot) {
        String text = (safeText(spot.getWorkTitle()) + " " + safeText(spot.getName())).toLowerCase(Locale.ROOT);
        return text.contains("clannad")
                || text.contains("凉宫")
                || text.contains("涼宮")
                || text.contains("轻音")
                || text.contains("けいおん")
                || text.contains("k-on")
                || text.contains("冰菓")
                || text.contains("氷菓")
                || text.contains("玉子")
                || text.contains("たまこ")
                || text.contains("中二病")
                || text.contains("free")
                || text.contains("境界")
                || text.contains("吹响")
                || text.contains("響け")
                || text.contains("上低音")
                || text.contains("紫罗兰")
                || text.contains("ヴァイオレット")
                || text.contains("甘城");
    }

    private boolean isNavigableSpot(XunliAgentSpot spot) {
        return spot != null
                && spot.getId() != null
                && spot.getLng() != null
                && spot.getLat() != null;
    }

    private boolean hasImage(XunliAgentSpot spot) {
        return spot != null && spot.getImageUrl() != null && !spot.getImageUrl().isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String safeText(String text) {
        return text == null ? "" : text.replace('\n', ' ').replace('|', '/').trim();
    }

    private String buildFallbackReply(String query) {
        if (containsAny(query, "京阿尼", "京都动画", "京都", "圣地巡礼", "巡礼")) {
            return "后端已经接到你的请求啦。当前 AppBuilder 的 app-id/token 还没配置，所以我先给一版基础路线：\n"
                    + "第 1 天：东京出发到京都，下午走京都站、鸭川、祇园一带适应节奏。\n"
                    + "第 2 天：宇治线，重点放《吹响！上低音号》相关地点，宇治桥、县神社、宇治川沿线可以串起来。\n"
                    + "第 3 天：京都市内学校与街景线，安排出町柳、下鸭神社、河合桥、商店街取景点。\n"
                    + "第 4 天：伏见稻荷、木幡方向，节奏放慢，多留时间拍摄对照。\n"
                    + "第 5 天：补漏和返程，上午挑最想二刷的点，下午回东京。\n"
                    + "要启用真正的 AI 规划，把 live2d.agent.appbuilder.app-id 和 token 填进后端 application.properties 后重启服务就行。";
        }

        return "后端已经收到消息，但 AppBuilder 的 app-id/token 还没配置，所以现在先用本地兜底回复。"
                + "配置 live2d.agent.appbuilder.app-id 和 live2d.agent.appbuilder.token 后重启后端，我就能接上真正的 agent。";
    }

    private JsonNode extractAssistantPayload(JsonNode root) {
        JsonNode textNode = firstTextNode(root, "answer", "content", "result");
        if (textNode == null) {
            return null;
        }

        String text = stripJsonFence(textNode.asText());
        if (!text.startsWith("{")) {
            return null;
        }

        try {
            JsonNode parsed = objectMapper.readTree(text);
            return parsed.isObject() ? parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode firstTextNode(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isTextual()) {
                return value;
            }
        }
        return null;
    }

    private static String stripJsonFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
            return trimmed.substring(firstLineEnd + 1, lastFence).trim();
        }
        return trimmed;
    }

    private static String extractReply(JsonNode root, JsonNode assistantPayload) {
        if (assistantPayload != null && assistantPayload.hasNonNull("reply")) {
            return assistantPayload.get("reply").asText();
        }
        if (root.hasNonNull("answer")) {
            return root.get("answer").asText();
        }
        if (root.hasNonNull("content")) {
            return root.get("content").asText();
        }
        if (root.hasNonNull("result")) {
            JsonNode result = root.get("result");
            if (result.isTextual()) {
                return result.asText();
            }
            return result.toString();
        }
        return root.isMissingNode() || root.isNull() ? "" : root.toString();
    }

    private Live2dAgentLive2dHints extractLive2dHints(JsonNode root, JsonNode assistantPayload, String reply) {
        JsonNode live2d = findLive2dNode(assistantPayload);
        if (live2d == null) {
            live2d = findLive2dNode(root);
        }
        if (live2d != null && live2d.isObject()) {
            Live2dAgentLive2dHints hints = new Live2dAgentLive2dHints();
            if (live2d.hasNonNull("motionGroup")) {
                hints.setMotionGroup(live2d.get("motionGroup").asText());
            }
            if (live2d.hasNonNull("motionIndex")) {
                hints.setMotionIndex(live2d.get("motionIndex").asInt());
            }
            if (live2d.hasNonNull("expressionId")) {
                hints.setExpressionId(live2d.get("expressionId").asText());
            }
            if (live2d.hasNonNull("messageDurationMs")) {
                hints.setMessageDurationMs(live2d.get("messageDurationMs").asInt());
            }
            return hints;
        }

        return guessLive2dHints(reply);
    }

    private JsonNode findLive2dNode(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        if (root.has("live2d")) {
            return root.get("live2d");
        }
        JsonNode result = root.get("result");
        if (result != null && result.isObject() && result.has("live2d")) {
            return result.get("live2d");
        }
        JsonNode data = root.get("data");
        if (data != null && data.isObject() && data.has("live2d")) {
            return data.get("live2d");
        }
        return null;
    }

    private JsonNode extractRoutePlan(JsonNode root, JsonNode assistantPayload, String reply) {
        JsonNode direct = findRoutePlanNode(assistantPayload);
        if (direct == null) {
            direct = findRoutePlanNode(root);
        }
        if (direct != null && direct.isObject()) {
            return direct;
        }

        JsonNode fromReply = parseRoutePlanFromText(reply);
        return fromReply != null && fromReply.isObject() ? fromReply : null;
    }

    private JsonNode findRoutePlanNode(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode routePlan = root.get("routePlan");
        if (isRoutePlan(routePlan)) {
            return routePlan;
        }
        JsonNode navigationRoute = root.get("navigationRoute");
        if (isRoutePlan(navigationRoute)) {
            return navigationRoute;
        }
        JsonNode result = root.get("result");
        if (result != null && result.isObject()) {
            JsonNode nested = findRoutePlanNode(result);
            if (nested != null) {
                return nested;
            }
        }
        JsonNode data = root.get("data");
        if (data != null && data.isObject()) {
            JsonNode nested = findRoutePlanNode(data);
            if (nested != null) {
                return nested;
            }
        }
        return isRoutePlan(root) ? root : null;
    }

    private boolean isRoutePlan(JsonNode node) {
        return node != null
                && node.isObject()
                && node.hasNonNull("intent")
                && "shengdi_route_plan".equals(node.get("intent").asText());
    }

    private JsonNode parseRoutePlanFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = stripJsonFence(text);
        JsonNode whole = tryReadJson(cleaned);
        JsonNode wholeRoute = findRoutePlanNode(whole);
        if (wholeRoute != null) {
            return wholeRoute;
        }

        int intentAt = cleaned.indexOf("\"shengdi_route_plan\"");
        if (intentAt < 0) {
            return null;
        }
        int start = cleaned.lastIndexOf('{', intentAt);
        while (start >= 0) {
            String candidate = balancedJsonObject(cleaned, start);
            JsonNode parsed = tryReadJson(candidate);
            JsonNode route = findRoutePlanNode(parsed);
            if (route != null) {
                return route;
            }
            start = cleaned.lastIndexOf('{', start - 1);
        }
        return null;
    }

    private JsonNode tryReadJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String balancedJsonObject(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String stripRoutePlanFromReply(String reply, JsonNode routePlan) {
        if (reply == null || reply.isBlank() || routePlan == null) {
            return reply;
        }
        String cleaned = reply;
        int intentAt = cleaned.indexOf("\"shengdi_route_plan\"");
        while (intentAt >= 0) {
            int start = cleaned.lastIndexOf('{', intentAt);
            String objectText = null;
            while (start >= 0) {
                String candidate = balancedJsonObject(cleaned, start);
                JsonNode parsed = tryReadJson(candidate);
                JsonNode route = findRoutePlanNode(parsed);
                if (route != null) {
                    objectText = candidate;
                    break;
                }
                start = cleaned.lastIndexOf('{', start - 1);
            }
            if (objectText == null) {
                break;
            }
            cleaned = cleaned.replace(objectText, "");
            intentAt = cleaned.indexOf("\"shengdi_route_plan\"");
        }
        cleaned = cleaned
                .replaceAll("(?im)^\\s*ROUTE_PLAN\\s*:\\s*$", "")
                .replaceAll("(?is)```(?:json|text)?\\s*\\n\\s*```", "")
                .replaceAll("(?is)```\\s*\\n\\s*```", "")
                .replaceAll("(?m)^\\s*下面我把这条路线交给前端.*$", "我会把这份路线交给前端，导航面板会自动填入途经点并按驾驶模式算路。")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return cleaned.isBlank() ? reply : cleaned;
    }

    private Live2dAgentLive2dHints guessLive2dHints(String reply) {
        Live2dAgentLive2dHints hints = new Live2dAgentLive2dHints();
        String text = reply == null ? "" : reply;

        hints.setMotionGroup("TapBody");
        hints.setMotionIndex(0);
        hints.setMessageDurationMs(Math.min(9000, Math.max(3500, text.length() * 120)));

        if (containsAny(text, "开心", "高兴", "喜欢", "太好了", "棒", "哈哈", "嘿嘿", "喵")) {
            hints.setExpressionId("Smile");
        } else if (containsAny(text, "抱歉", "难过", "伤心", "失败", "出错", "不太行")) {
            hints.setExpressionId("Sad");
        } else if (containsAny(text, "惊", "等等", "真的吗", "居然", "哇")) {
            hints.setExpressionId("Surprised");
        } else if (containsAny(text, "生气", "不许", "讨厌")) {
            hints.setExpressionId("Angry");
        } else {
            hints.setExpressionId("Normal");
            hints.setMotionGroup("Idle");
        }

        return hints;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
