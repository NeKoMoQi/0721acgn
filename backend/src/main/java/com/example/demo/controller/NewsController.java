package com.example.demo.controller;

import com.example.demo.com.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the worker's atomic public snapshot; drafts and credentials never enter the web root. */
@RestController
public class NewsController {
    private final Path snapshot;
    private final ObjectMapper mapper;

    public NewsController(@Value("${newsroom.data-dir:../newsroom/data}") String directory, ObjectMapper mapper) {
        this.snapshot = Path.of(directory).toAbsolutePath().normalize().resolve("published.json");
        this.mapper = mapper;
    }

    private JsonNode articles() {
        if (!Files.exists(snapshot)) return mapper.createArrayNode();
        try {
            JsonNode articles = mapper.readTree(Files.readString(snapshot)).path("articles");
            if (!articles.isArray()) throw new IOException("invalid snapshot");
            return articles;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "资讯暂时无法读取");
        }
    }

    @GetMapping("/api/news")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "") String featured) {
        if (page < 1 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        JsonNode all = articles();
        List<JsonNode> pool = new ArrayList<>();
        for (JsonNode article : all) {
            // featured=1 只返回精选（按 ACGN 相关度打过标的），其余仍走普通分页。
            if ("1".equals(featured) && !article.path("featured").asBoolean(false)) continue;
            pool.add(article);
        }
        List<JsonNode> items = new ArrayList<>();
        int start = (page - 1) * 12;
        for (int i = start; i < Math.min(start + 12, pool.size()); i++) {
            var summary = pool.get(i).deepCopy();
            if (summary.isObject()) ((com.fasterxml.jackson.databind.node.ObjectNode) summary).remove("paragraphs");
            items.add(summary);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", pool.size());
        return Result.success(data);
    }

    @GetMapping("/api/news/byid")
    public Result<JsonNode> detail(@RequestParam String id) {
        if (!id.matches("[a-f0-9]{24}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        for (JsonNode article : articles()) {
            if (id.equals(article.path("id").asText())) return Result.success(article);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资讯不存在或已撤下");
    }

    @GetMapping("/api/news/image")
    public ResponseEntity<byte[]> image(@RequestParam String id, @RequestParam String role) {
        if (!id.matches("[a-f0-9]{24}") || !(role.equals("cover") || role.equals("stats"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // Do not serve artwork for drafts/retracted articles, even if its filename is guessed.
        JsonNode article = detail(id).getData();
        boolean known = false;
        for (JsonNode asset : article.path("images")) {
            if (role.equals(asset.path("role").asText())) known = true;
        }
        if (!known) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // 优先用 worker 生成的 WebP（同一张封面 PNG 常有 600–900KB，WebP 约 80KB）
        Path media = snapshot.getParent().resolve("media");
        Path webp = media.resolve(id + "-" + role + ".webp");
        Path png = media.resolve(id + "-" + role + ".png");
        Path image = Files.isRegularFile(webp) ? webp : png;
        try {
            if (!Files.isRegularFile(image) || Files.size(image) > 8_000_000) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            MediaType type = image.equals(webp)
                    ? MediaType.parseMediaType("image/webp")
                    : MediaType.IMAGE_PNG;
            // 文件名是内容 id，内容不会变，可以长缓存
            return ResponseEntity.ok().contentType(type)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .header("X-Content-Type-Options", "nosniff").body(Files.readAllBytes(image));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
