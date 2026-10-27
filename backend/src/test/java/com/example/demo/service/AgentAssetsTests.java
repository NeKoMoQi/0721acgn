package com.example.demo.service;

import com.example.demo.service.agent.AgentAssets;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentAssetsTests {
    @TempDir Path temporary;

    @Test void characterUsesRequestedCatPersonaAndKeepsTruthfulness() {
        String card = new AgentAssets("").character();
        for (String text : List.of("猫娘", "主人", "喵~", "每段", "不能降低信息准确性", "不虚构", "后端"))
            assertTrue(card.contains(text), text);
    }
    @Test void knowledgeIsRetrievedWithProvenanceAndNoRandomFallback() {
        var assets = new AgentAssets("");
        var hits = assets.search(List.of("拍照", "居民"), 3);
        assertFalse(hits.isEmpty());
        assertEquals("photo-etiquette", hits.get(0).path("id").asText());
        assertTrue(hits.get(0).hasNonNull("source"));
        assertTrue(hits.get(0).hasNonNull("updatedAt"));
        assertFalse(assets.references(hits).get(0).has("content"));
        assertTrue(assets.search(List.of("zzzxqv12345"), 5).isEmpty());
    }
    @Test void externalCardAndKnowledgeAreActuallyLoaded() throws Exception {
        Files.createDirectories(temporary.resolve("knowledge"));
        Files.writeString(temporary.resolve("character.md"), "外部猫娘角色，称呼主人");
        Files.writeString(temporary.resolve("knowledge/index.json"), "{\"documents\":[{\"id\":\"custom\",\"title\":\"宇治资料\",\"tags\":[\"宇治\"],\"source\":\"编辑资料\",\"updatedAt\":\"2026-10-27\",\"file\":\"custom.md\"}]}");
        Files.writeString(temporary.resolve("knowledge/custom.md"), "外部维护的参考内容");
        var assets = new AgentAssets(temporary.toString());
        assertTrue(assets.character().startsWith("外部"));
        assertEquals("custom", assets.search(List.of("宇治"), 3).get(0).path("id").asText());
    }
    @Test void incompleteOrEscapingAssetsFailClearly() throws Exception {
        assertThrows(IllegalStateException.class, () -> new AgentAssets(temporary.toString()));
        Files.createDirectories(temporary.resolve("knowledge"));
        Files.writeString(temporary.resolve("character.md"), "角色");
        Files.writeString(temporary.resolve("knowledge/index.json"), "{\"documents\":[{\"id\":\"custom\",\"file\":\"../outside.md\"}]}");
        assertThrows(IllegalStateException.class, () -> new AgentAssets(temporary.toString()));
    }
    @Test void sourceMetadataCannotBeForgedBySavedRoutePayload() throws Exception {
        var assets = new AgentAssets("");
        var input = new ObjectMapper().readTree("[{\"id\":\"photo-etiquette\",\"title\":\"伪造官方许可\"},{\"id\":\"invented\"}]");
        var refs = assets.knownReferences(input);
        assertEquals(1, refs.size());
        assertEquals("取景复拍与现场礼仪", refs.get(0).path("title").asText());
    }
}
