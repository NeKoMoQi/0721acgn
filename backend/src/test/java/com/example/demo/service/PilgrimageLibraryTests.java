package com.example.demo.service;

import com.example.demo.dto.XunliAgentSpot;
import com.example.demo.service.agent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PilgrimageLibraryTests {
    private final ObjectMapper json = new ObjectMapper();
    private PilgrimageDataStore store;
    private PilgrimageCatalog catalog;
    private PilgrimageRouteLibrary library;
    private XunliAgentSpot point;
    @BeforeEach void setup() {
        store = mock(PilgrimageDataStore.class); catalog = mock(PilgrimageCatalog.class);
        point = new XunliAgentSpot(); point.setId("1"); point.setName("站内真实地点"); point.setLng(135.8); point.setLat(34.9);
        point.setImageUrl("https://example.test/trusted.jpg");
        when(catalog.all()).thenReturn(List.of(point));
        library = new PilgrimageRouteLibrary(store, catalog, new PilgrimagePlanValidator(), new AgentAssets(""));
    }
    private com.fasterxml.jackson.databind.JsonNode plan() throws Exception {
        return json.readTree("{\"version\":2,\"travelMode\":\"walking\",\"days\":[{\"title\":\"当天\",\"note\":\"少走\",\"pointIds\":[\"1\"],\"spots\":[{\"name\":\"伪造名称\",\"coordinates\":[0,0]}]}]}");
    }
    @Test void savingRebuildsCoordinatesNamesAndImagesFromExistingDatabase() throws Exception {
        when(store.save(eq("user:1"), eq("我的行程"), any())).thenAnswer(invocation -> {
            com.fasterxml.jackson.databind.JsonNode saved = invocation.getArgument(2);
            assertEquals("站内真实地点", saved.at("/days/0/spots/0/name").asText());
            assertEquals(135.8, saved.at("/days/0/spots/0/coordinates/0").asDouble());
            return "saved-id";
        });
        var body = json.createObjectNode().put("title", "我的行程"); body.set("plan", plan());
        assertEquals("saved-id", library.save("user:1", body).get("id"));
    }
    @Test void readAndDeleteAreScopedToOwner() {
        assertThrows(IllegalArgumentException.class, () -> library.load("user:2", "other-id"));
        verify(store).route("other-id", "user:2");
        assertThrows(IllegalArgumentException.class, () -> library.delete("user:2", "other-id"));
        verify(store).delete("other-id", "user:2");
    }
    @Test void newlyClosedPointCannotBeRestoredOrSaved() throws Exception {
        point.setAccessStatus("closed");
        var body = json.createObjectNode().put("title", "我的行程"); body.set("plan", plan());
        assertThrows(IllegalArgumentException.class, () -> library.save("user:1", body));
        verify(store, never()).save(anyString(), anyString(), any());
        when(store.route("id", "user:1")).thenReturn(Map.of("plan", plan().toString(), "title", "旧行程"));
        when(store.parse(anyString())).thenReturn(plan());
        assertThrows(IllegalArgumentException.class, () -> library.load("user:1", "id"));
    }
}
