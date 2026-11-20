package com.example.demo.controller;

import com.example.demo.utils.PublicAccessPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NewsControllerTests {
    @TempDir Path directory;

    @Test void missingSnapshotIsEmptyAndGuestCanRead() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new NewsController(directory.toString(), new ObjectMapper())).build();
        mvc.perform(get("/api/news")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/news"));
        assertTrue(PublicAccessPolicy.isPublic("GET", "/api/news/byid"));
        assertFalse(PublicAccessPolicy.isPublic("POST", "/api/news"));
    }

    @Test void onlySnapshotIsReadAndDetailRequiresKnownId() throws Exception {
        Files.writeString(directory.resolve("published.json"), "{\"articles\":[{\"id\":\"0123456789abcdef01234567\",\"title\":\"新闻\",\"paragraphs\":[\"正文\"]}]}");
        Files.writeString(directory.resolve("draft.json"), "secret draft");
        var mvc = MockMvcBuilders.standaloneSetup(new NewsController(directory.toString(), new ObjectMapper())).build();
        mvc.perform(get("/api/news")).andExpect(jsonPath("$.data.items[0].paragraphs").doesNotExist());
        mvc.perform(get("/api/news/byid").param("id", "0123456789abcdef01234567"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.paragraphs[0]").value("正文"));
        mvc.perform(get("/api/news/byid").param("id", "../../draft.json")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/news/byid").param("id", "aaaaaaaaaaaaaaaaaaaaaaaa")).andExpect(status().isNotFound());
        mvc.perform(get("/api/news").param("page", "0")).andExpect(status().isBadRequest());
    }

    @Test void malformedSnapshotIsUnavailable() throws Exception {
        Files.writeString(directory.resolve("published.json"), "broken");
        var mvc = MockMvcBuilders.standaloneSetup(new NewsController(directory.toString(), new ObjectMapper())).build();
        mvc.perform(get("/api/news")).andExpect(status().isServiceUnavailable());
    }

    @Test void imagesAreOnlyAccessibleForPublishedKnownArticles() throws Exception {
        String id = "0123456789abcdef01234567";
        Files.writeString(directory.resolve("published.json"), "{\"articles\":[{\"id\":\"" + id + "\",\"images\":[{\"role\":\"cover\"}]}]}");
        Files.createDirectories(directory.resolve("media"));
        Files.write(directory.resolve("media/" + id + "-cover.png"), new byte[]{1, 2, 3});
        Files.write(directory.resolve("media/" + id + "-cover-thumb.webp"), new byte[]{4, 5, 6});
        var mvc = MockMvcBuilders.standaloneSetup(new NewsController(directory.toString(), new ObjectMapper())).build();
        mvc.perform(get("/api/news/image").param("id", id).param("role", "cover"))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
            .andExpect(header().string("Cache-Control", "max-age=2592000, public"));
        Files.write(directory.resolve("media/" + id + "-cover.webp"), new byte[]{7, 8, 9});
        mvc.perform(get("/api/news/image").param("id", id).param("role", "cover"))
            .andExpect(status().isOk()).andExpect(content().contentType("image/webp"))
            .andExpect(content().bytes(new byte[]{7, 8, 9}));
        var thumbnail = get("/api/news/image").param("id", id).param("role", "cover").param("variant", "thumb");
        String etag = mvc.perform(thumbnail)
            .andExpect(status().isOk()).andExpect(content().contentType("image/webp"))
            .andExpect(header().string("Cache-Control", "max-age=300, public"))
            .andExpect(content().bytes(new byte[]{4, 5, 6})).andReturn().getResponse().getHeader("ETag");
        mvc.perform(thumbnail.header("If-None-Match", etag)).andExpect(status().isNotModified());
        mvc.perform(get("/api/news/image").param("id", id).param("role", "../secrets"))
            .andExpect(status().isBadRequest());
        Files.writeString(directory.resolve("published.json"), "{\"articles\":[]}");
        mvc.perform(get("/api/news/image").param("id", id).param("role", "cover"))
            .andExpect(status().isNotFound());
        mvc.perform(thumbnail).andExpect(status().isNotFound());
    }
}
