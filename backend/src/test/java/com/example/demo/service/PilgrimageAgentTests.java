package com.example.demo.service;

import com.example.demo.com.XunliPoint;
import com.example.demo.dto.Live2dAgentChatRequest;
import com.example.demo.dto.XunliAgentSpot;
import com.example.demo.service.agent.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PilgrimageAgentTests {
    private final ObjectMapper json = new ObjectMapper();
    private final PilgrimagePlanValidator validator = new PilgrimagePlanValidator();
    private DeepSeekClient client;
    private XunliService source;
    private PilgrimageCatalog catalog;
    private Live2dAgentService service;
    private PilgrimageDataStore store;

    @BeforeEach void setup() {
        client = mock(DeepSeekClient.class);
        when(client.configured()).thenReturn(true);
        source = mock(XunliService.class);
        when(source.getPickerXunliPointList()).thenReturn(List.of(point(1, "宇治桥", 135.806, 34.891), point(2, "宇治川", 135.808, 34.893)));
        store = mock(PilgrimageDataStore.class);
        catalog = new PilgrimageCatalog(source, store);
        service = new Live2dAgentService(client, catalog, validator, new AgentAssets(""), store);
    }

    private XunliPoint point(int id, String name, double lng, double lat) {
        var p = new XunliPoint(); p.setId(id); p.setXunliid(7); p.setXunliname("吹响！上低音号");
        p.setPointXunliname(name); p.setCity("京都府宇治市"); p.setGeopoint(List.of(lng, lat)); p.setXunlipic("https://example.test/" + id + ".jpg");
        return p;
    }
    private JsonNode tree(String s) throws Exception { return json.readTree(s); }
    private Live2dAgentChatRequest request(String query, String id, String mode) {
        var r = new Live2dAgentChatRequest(); r.setQuery(query); r.setConversationId(id); r.setMode(mode); return r;
    }
    private ObjectNode call(String name, JsonNode args) {
        var a = json.createObjectNode().put("role", "assistant").put("content", "");
        a.putArray("tool_calls").addObject().put("id", "call1").put("type", "function").putObject("function").put("name", name).put("arguments", args.toString());
        return a;
    }
    private JsonNode proposal() throws Exception {
        return tree("{\"travelMode\":\"walking\",\"days\":[{\"title\":\"宇治\",\"note\":\"沿河少折返\",\"pointIds\":[\"1\",\"2\"]}]}");
    }
    private Map<String, XunliAgentSpot> candidates() throws Exception {
        var map = new LinkedHashMap<String, XunliAgentSpot>();
        catalog.search(tree("{\"keywords\":[\"上低音号\"],\"city\":\"宇治\",\"offset\":0}"), map);
        return map;
    }

    @Test void searchThenSubmitProducesEnrichedDailyPlanAndSupportsFollowup() throws Exception {
        when(client.complete(anyList(), any(), any(), any())).thenReturn(
                call("search_spots", tree("{\"keywords\":[\"上低音号\"],\"city\":\"宇治\",\"offset\":0}")), call("submit_route", proposal()));
        var events = new ArrayList<String>();
        var first = service.run(request("宇治一天步行", null, "route"), new AgentRun(), (event, data) -> events.add(event));
        assertEquals(2, first.getRoutePlan().path("version").asInt());
        assertEquals("宇治桥", first.getRoutePlan().at("/days/0/spots/0/name").asText());
        assertEquals("https://example.test/1.jpg", first.getRoutePlan().at("/days/0/spots/0/imageUrl").asText());
        assertTrue(events.contains("status"));
        reset(client); when(client.configured()).thenReturn(true);
        when(client.complete(anyList(), any(), any(), any())).thenAnswer(invocation -> {
            List<JsonNode> messages = invocation.getArgument(0);
            assertTrue(messages.toString().contains("上一版已验证路线"));
            assertFalse(messages.toString().contains("https://example.test"));
            return call("submit_route", proposal());
        });
        var second = service.chat(request("保持第一天", first.getConversationId(), "route"));
        assertEquals(first.getConversationId(), second.getConversationId());
        assertNotNull(second.getRoutePlan());
    }

    @Test void searchNoMatchDoesNotReturnRandomPointsAndUsesCache() throws Exception {
        var allowed = new LinkedHashMap<String, XunliAgentSpot>();
        var result = catalog.search(tree("{\"keywords\":[\"不存在的番剧\"],\"city\":\"\"}"), allowed);
        assertEquals(0, result.path("spots").size()); assertTrue(allowed.isEmpty());
        candidates(); candidates(); verify(source, times(1)).getPickerXunliPointList();
    }

    @Test void discoveryDoesNotAuthorizeAnyPoints() throws Exception {
        var allowed = new LinkedHashMap<String, XunliAgentSpot>();
        var result = catalog.search(tree("{\"keywords\":[],\"city\":\"\"}"), allowed);
        assertTrue(result.has("catalogue")); assertTrue(allowed.isEmpty());
    }

    @Test void workNameFallbackCannotPretendToBeANamedLandmark() throws Exception {
        var unnamed = point(3, null, 135.81, 34.9);
        when(source.getPickerXunliPointList()).thenReturn(List.of(unnamed));
        assertTrue(candidates().isEmpty());
    }

    @Test void unknownPointAndNumericIdAreRejected() throws Exception {
        var unknown = proposal().deepCopy(); ((com.fasterxml.jackson.databind.node.ArrayNode) unknown.at("/days/0/pointIds")).add("999");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(unknown, candidates()));
        var numeric = proposal().deepCopy(); ((com.fasterxml.jackson.databind.node.ArrayNode) numeric.at("/days/0/pointIds")).set(0, json.getNodeFactory().numberNode(1));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(numeric, candidates()));
    }

    @Test void duplicateCoordinatesAndImpossibleWalkingLegAreRejected() throws Exception {
        var map = candidates(); map.get("2").setLng(map.get("1").getLng()); map.get("2").setLat(map.get("1").getLat());
        assertThrows(IllegalArgumentException.class, () -> validator.validate(proposal(), map));
        map.get("2").setLng(map.get("1").getLng() + 0.0000001);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(proposal(), map));
        map.get("2").setLng(139.7); map.get("2").setLat(35.6);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(proposal(), map));
    }

    @Test void dayBoundariesSurviveAndMissingTransportIsNotInvented() throws Exception {
        var input = proposal().deepCopy(); ((com.fasterxml.jackson.databind.node.ArrayNode) input.path("days")).add(input.path("days").get(0).deepCopy());
        assertEquals(2, validator.validate(input, candidates()).path("days").size());
        ((ObjectNode) input).put("travelMode", "transit");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(input, candidates()));
    }

    @Test void invalidModelProposalReturnsToolErrorForRepairInsteadOfMapData() throws Exception {
        var invalid = call("submit_route", proposal());
        when(client.complete(anyList(), any(), any(), any())).thenReturn(invalid,
                json.createObjectNode().put("role", "assistant").put("content", "还需要先确认作品或城市。"));
        var result = service.chat(request("帮我安排", null, "route"));
        assertNull(result.getRoutePlan());
        verify(client, times(2)).complete(anyList(), any(), any(), any());
    }

    @Test void chatModeHasNoToolsAndCannotReuseRouteSession() throws Exception {
        when(client.complete(anyList(), isNull(), any(), any())).thenReturn(json.createObjectNode().put("role", "assistant").put("content", "你好"));
        var result = service.chat(request("你好", null, "chat"));
        assertNull(result.getRoutePlan());
        assertThrows(IllegalArgumentException.class, () -> service.chat(request("路线", result.getConversationId(), "route")));
        verifyNoInteractions(source);
    }

    @Test void unconfiguredExpiredAndCancelledRequestsFailHonestly() {
        when(client.configured()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.chat(request("你好", null, "chat")));
        when(client.configured()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.chat(request("你好", "missing", "chat")));
        var run = new AgentRun(); run.cancel();
        assertThrows(CancellationException.class, () -> service.run(request("你好", null, "chat"), run, (a,b) -> {}));
        verify(client, never()).complete(anyList(), any(), any(), any());
    }

    @Test void cardAndKnowledgeAreInModelContextAndSourcesAreReturned() {
        when(client.complete(anyList(), any(), any(), any())).thenAnswer(invocation -> {
            List<JsonNode> messages = invocation.getArgument(0);
            assertTrue(messages.get(0).path("content").asText().contains("称呼用户为「主人」"));
            assertTrue(messages.toString().contains("photo-etiquette"));
            return json.createObjectNode().put("content", "主人，先确认允许拍摄的范围喵~");
        });
        var response = service.chat(request("居民区拍照", null, "route"));
        assertFalse(response.getKnowledgeSources().isEmpty());
    }

    @Test void accountSessionRestoresAfterServiceRestartAndRejectsOtherOwner() {
        when(client.complete(anyList(), any(), any(), any())).thenReturn(json.createObjectNode().put("content", "主人，你好喵~"));
        final JsonNode[] persisted = new JsonNode[1];
        when(store.saveSession(anyString(), eq("user:1"), eq("chat"), any(), anyLong())).thenAnswer(invocation -> {
            persisted[0] = invocation.getArgument(3);
            return ((Long) invocation.getArgument(4)) + 1;
        });
        var firstRequest = request("你好", null, "chat"); firstRequest.setOwnerKey("user:1");
        var first = service.chat(firstRequest);
        when(store.session(first.getConversationId(), "user:1", "chat")).thenReturn(Map.of("payload", persisted[0].toString(), "version", 1L));
        when(store.parse(anyString())).thenAnswer(invocation -> json.readTree((String) invocation.getArgument(0)));
        var restarted = new Live2dAgentService(client, catalog, validator, new AgentAssets(""), store);
        var followup = request("继续聊", first.getConversationId(), "chat"); followup.setOwnerKey("user:1");
        assertEquals(first.getConversationId(), restarted.chat(followup).getConversationId());
        var other = request("继续聊", first.getConversationId(), "chat"); other.setOwnerKey("user:2");
        assertThrows(IllegalArgumentException.class, () -> restarted.chat(other));
    }

    @Test void databaseAliasesAndRestrictedSpotMetadataAffectRetrieval() throws Exception {
        when(store.aliases()).thenReturn(List.of(Map.of("kind", "work", "alias", "京吹", "canonical", "上低音号")));
        when(store.spotInfo()).thenReturn(List.of(Map.of("id", "2", "status", "closed", "note", "暂不开放")));
        var allowed = new LinkedHashMap<String, XunliAgentSpot>();
        var result = catalog.search(tree("{\"keywords\":[\"京吹\"],\"city\":\"宇治\"}"), allowed);
        assertEquals(1, result.path("spots").size());
        assertTrue(allowed.containsKey("1")); assertFalse(allowed.containsKey("2"));
    }
}
