package com.example.demo.service;

import com.example.demo.service.agent.AgentRun;
import com.example.demo.service.agent.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekClientTests {
    private HttpServer server;
    private DeepSeekClient client;
    private final ObjectMapper json = new ObjectMapper();
    private String captured;
    private String response;
    private int status;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        response = ""; status = 200;
        server.createContext("/chat/completions", exchange -> {
            captured = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(status, 0);
            try (var body = exchange.getResponseBody()) {
                // Fragment the network writes as a real streaming provider does.
                for (byte value : response.getBytes(StandardCharsets.UTF_8)) body.write(value);
            }
        });
        server.start();
        client = new DeepSeekClient();
        ReflectionTestUtils.setField(client, "apiKey", "test-secret-never-returned");
        ReflectionTestUtils.setField(client, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(client, "model", "deepseek-flash");
    }

    @AfterEach void stop() { server.stop(0); }

    private String chunk(Object delta, String finish) throws Exception {
        var choice = json.createObjectNode(); choice.set("delta", json.valueToTree(delta));
        if (finish == null) choice.putNull("finish_reason"); else choice.put("finish_reason", finish);
        return "data: " + json.createObjectNode().set("choices", json.createArrayNode().add(choice)) + "\n\n";
    }

    @Test void streamsChineseAndExplicitlyDisablesThinking() throws Exception {
        response = chunk(Map.of("content", "京都"), null) + chunk(Map.of("content", "巡礼"), null)
                + chunk(Map.of(), "stop") + "data: [DONE]\n\n";
        List<String> deltas = new ArrayList<>();
        var result = client.complete(List.of(json.createObjectNode().put("role", "user").put("content", "你好")), null, new AgentRun(), deltas::add);
        assertEquals("京都巡礼", result.path("content").asText());
        assertEquals(List.of("京都", "巡礼"), deltas);
        JsonNode sent = json.readTree(captured);
        assertTrue(sent.path("stream").asBoolean());
        assertEquals("disabled", sent.at("/thinking/type").asText());
        assertFalse(captured.contains("test-secret"));
    }

    @Test void assemblesFragmentedToolArgumentsWithoutDisplayingJson() throws Exception {
        response = chunk(Map.of("tool_calls", List.of(Map.of("index", 0, "id", "c1", "function", Map.of("name", "search_spots", "arguments", "{\"keywords\":")))), null)
                + chunk(Map.of("tool_calls", List.of(Map.of("index", 0, "function", Map.of("arguments", "[\"宇治\"]}")))), null)
                + chunk(Map.of(), "tool_calls") + "data: [DONE]\n\n";
        var result = client.complete(List.of(), json.createArrayNode(), new AgentRun(), value -> fail("Tool JSON must not be shown"));
        assertEquals("search_spots", result.at("/tool_calls/0/function/name").asText());
        assertEquals("宇治", json.readTree(result.at("/tool_calls/0/function/arguments").asText()).at("/keywords/0").asText());
    }

    @Test void truncatedAndLengthLimitedRepliesCannotCommit() throws Exception {
        response = chunk(Map.of("content", "半条"), null);
        assertThrows(IllegalStateException.class, () -> client.complete(List.of(), null, new AgentRun(), s -> {}));
        response = chunk(Map.of(), "length") + "data: [DONE]\n\n";
        assertThrows(IllegalStateException.class, () -> client.complete(List.of(), null, new AgentRun(), s -> {}));
    }

    @Test void providerErrorBodyAndSecretAreNotExposed() {
        status = 401; response = "sensitive upstream diagnostic test-secret-never-returned";
        var error = assertThrows(IllegalStateException.class, () -> client.complete(List.of(), null, new AgentRun(), s -> {}));
        assertTrue(error.getMessage().contains("密钥"));
        assertFalse(error.getMessage().contains("test-secret"));
    }

    @Test void cancellingClosesAnActiveUpstreamRead() throws Exception {
        var ready = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        server.createContext("/cancel/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("data: {\"choices\":[{\"delta\":{\"content\":\"开始\"}}]}\n\n".getBytes(StandardCharsets.UTF_8));
                body.flush(); ready.countDown();
                try { release.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        ReflectionTestUtils.setField(client, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/cancel");
        var executor = Executors.newSingleThreadExecutor();
        var run = new AgentRun();
        try {
            var result = executor.submit(() -> client.complete(List.of(), null, run, s -> {}));
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), run::cancel);
            var error = assertThrows(ExecutionException.class, () -> result.get(3, TimeUnit.SECONDS));
            assertInstanceOf(CancellationException.class, error.getCause());
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
