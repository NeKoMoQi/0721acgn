package com.example.demo.service.agent;

import com.example.demo.dto.XunliAgentSpot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Account-owned itinerary snapshots, revalidated against current point data on save and restore. */
@Service
public class PilgrimageRouteLibrary {
    private final PilgrimageDataStore store;
    private final PilgrimageCatalog catalog;
    private final PilgrimagePlanValidator validator;
    private final AgentAssets assets;
    public PilgrimageRouteLibrary(PilgrimageDataStore store, PilgrimageCatalog catalog,
                                  PilgrimagePlanValidator validator, AgentAssets assets) {
        this.store = store; this.catalog = catalog; this.validator = validator; this.assets = assets;
    }
    public List<Map<String, Object>> list(String owner) { return store.routes(owner); }
    public Map<String, String> save(String owner, JsonNode body) {
        String title = body.path("title").asText().trim();
        if (title.isEmpty() || title.length() > 100) throw new IllegalArgumentException("行程名称需要1～100字");
        JsonNode input = body.path("plan");
        if (input.toString().length() > 100_000 || input.path("version").asInt() != 2)
            throw new IllegalArgumentException("无效或过长的行程数据");
        ObjectNode plan = revalidate(input);
        return Map.of("id", store.save(owner, title, plan), "title", title);
    }
    public Map<String, Object> load(String owner, String id) {
        var row = store.route(id, owner);
        if (row == null || row.isEmpty()) throw new IllegalArgumentException("未找到属于当前账号的行程");
        var input = store.parse((String) row.get("plan"));
        return Map.of("id", id, "title", row.get("title"), "plan", revalidate(input));
    }
    public void delete(String owner, String id) {
        if (!store.delete(id, owner)) throw new IllegalArgumentException("未找到属于当前账号的行程");
    }
    private ObjectNode revalidate(JsonNode input) {
        Map<String, XunliAgentSpot> points = new LinkedHashMap<>();
        for (var point : catalog.all()) points.put(point.getId(), point);
        var plan = validator.validate(input, points);
        plan.set("knowledgeSources", assets.knownReferences(input.path("knowledgeSources")));
        return plan;
    }
}
