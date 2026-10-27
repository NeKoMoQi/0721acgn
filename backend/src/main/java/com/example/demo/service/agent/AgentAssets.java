package com.example.demo.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/** Editable character card and small, source-labelled editorial knowledge collection. */
@Component
public class AgentAssets {
    private final ObjectMapper json = new ObjectMapper();
    private final Path directory;
    private volatile Snapshot snapshot;
    private long expiresAt;

    public AgentAssets(@Value("${live2d.agent.assets-dir:}") String directory) {
        this.directory = directory == null || directory.isBlank() ? null : Path.of(directory).toAbsolutePath().normalize();
        current();
    }

    public String character() { return current().character; }

    public ArrayNode search(List<String> keywords, int limit) {
        var terms = keywords.stream().filter(Objects::nonNull).map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty()).limit(8).toList();
        ArrayNode result = json.createArrayNode();
        if (terms.isEmpty()) return result;
        current().documents.stream().map(doc -> Map.entry(doc, score(doc, terms)))
                .filter(e -> e.getValue() > 0).sorted(Comparator.<Map.Entry<Document, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(e -> e.getKey().id)).limit(Math.max(1, Math.min(limit, 5)))
                .forEach(e -> {
                    Document d = e.getKey();
                    var node = result.addObject().put("id", d.id).put("title", d.title).put("source", d.source)
                            .put("updatedAt", d.updatedAt).put("scope", "站内编辑参考，非实时核验");
                    node.put("content", d.content.substring(0, Math.min(2200, d.content.length())));
                });
        return result;
    }

    public ArrayNode references(JsonNode matches) {
        ArrayNode output = json.createArrayNode();
        for (JsonNode node : matches) {
            var ref = node.deepCopy();
            if (ref.isObject()) ((com.fasterxml.jackson.databind.node.ObjectNode) ref).remove("content");
            output.add(ref);
        }
        return output;
    }

    public ArrayNode knownReferences(JsonNode supplied) {
        Set<String> ids = new HashSet<>();
        for (JsonNode node : supplied) ids.add(node.path("id").asText());
        ArrayNode output = json.createArrayNode();
        for (Document d : current().documents) if (ids.contains(d.id))
            output.addObject().put("id", d.id).put("title", d.title).put("source", d.source)
                    .put("updatedAt", d.updatedAt).put("scope", "站内编辑参考，非实时核验");
        return output;
    }

    private int score(Document document, List<String> terms) {
        int score = 0;
        for (String term : terms) {
            if (document.title.toLowerCase(Locale.ROOT).contains(term)) score += 5;
            for (String tag : document.tags) if (term.contains(tag) || tag.contains(term)) score += 4;
            if (document.content.toLowerCase(Locale.ROOT).contains(term)) score++;
        }
        return score;
    }

    private synchronized Snapshot current() {
        if (snapshot != null && System.currentTimeMillis() < expiresAt) return snapshot;
        try {
            String character = read("character.md", 16000);
            if (character.isBlank()) throw new IllegalArgumentException("角色卡不能为空");
            JsonNode index = json.readTree(read("knowledge/index.json", 32000));
            if (!index.path("documents").isArray() || index.path("documents").size() > 64)
                throw new IllegalArgumentException("知识目录必须包含不超过64篇 documents");
            List<Document> documents = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonNode node : index.path("documents")) {
                String id = node.path("id").asText();
                String file = node.path("file").asText();
                if (!id.matches("[a-z0-9-]{1,64}") || !ids.add(id) || !file.matches("[a-z0-9-]+\\.md"))
                    throw new IllegalArgumentException("知识条目的 ID 或文件名无效/重复");
                String title = node.path("title").asText();
                String source = node.path("source").asText();
                String date = node.path("updatedAt").asText();
                LocalDate.parse(date);
                if (title.isBlank() || source.isBlank()) throw new IllegalArgumentException("知识条目需要标题和来源");
                List<String> tags = new ArrayList<>();
                for (JsonNode tag : node.path("tags")) if (!tag.asText().isBlank()) tags.add(tag.asText().toLowerCase(Locale.ROOT));
                documents.add(new Document(id, title, source, date, tags, read("knowledge/" + file, 32000)));
            }
            snapshot = new Snapshot(character, List.copyOf(documents));
            expiresAt = System.currentTimeMillis() + 30_000;
            return snapshot;
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("巡礼角色卡或知识文件配置有误，请检查 agent 资源目录", error);
        }
    }

    private String read(String name, int maxBytes) throws IOException {
        byte[] bytes;
        if (directory == null) {
            try (var stream = getClass().getResourceAsStream("/agent/" + name)) {
                if (stream == null) throw new IOException("Missing asset: " + name);
                bytes = stream.readNBytes(maxBytes + 1);
            }
        } else {
            Path target = directory.resolve(name).normalize();
            if (!target.startsWith(directory)) throw new IOException("Invalid asset path");
            try (var stream = Files.newInputStream(target)) { bytes = stream.readNBytes(maxBytes + 1); }
        }
        if (bytes.length > maxBytes) throw new IOException("Asset too large: " + name);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private record Document(String id, String title, String source, String updatedAt, List<String> tags, String content) {}
    private record Snapshot(String character, List<Document> documents) {}
}
