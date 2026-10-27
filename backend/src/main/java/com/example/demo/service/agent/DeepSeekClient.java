package com.example.demo.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Direct streaming DeepSeek transport with cancellable asynchronous HTTP and no secret-bearing errors. */
@Component
public class DeepSeekClient {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    @Value("${live2d.agent.deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String apiKey;
    @Value("${live2d.agent.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${live2d.agent.deepseek.model:deepseek-flash}")
    private String model;

    public boolean configured() { return apiKey != null && !apiKey.isBlank(); }

    public ObjectNode complete(List<JsonNode> messages, ArrayNode tools, AgentRun run, Consumer<String> onText) {
        if (!configured()) throw new IllegalStateException("DeepSeek 尚未配置，请在服务器设置 DEEPSEEK_API_KEY");
        run.check();
        ObjectNode body = json.createObjectNode().put("model", model).put("stream", true)
                .put("max_tokens", 4096).put("temperature", 0.4);
        body.putObject("thinking").put("type", "disabled");
        body.set("messages", json.valueToTree(messages));
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }
        var request = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                .timeout(Duration.ofSeconds(85)).header("Content-Type", "application/json")
                .header("Accept", "text/event-stream").header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        StreamReply reply = new StreamReply(run, onText);
        CompletableFuture<HttpResponse<Void>> exchange = transport.sendAsync(request, info -> {
            if (info.statusCode() != 200) reply.completion.completeExceptionally(providerError(info.statusCode()));
            return HttpResponse.BodySubscribers.fromLineSubscriber(reply);
        });
        exchange.whenComplete((response, error) -> {
            if (error != null) reply.completion.completeExceptionally(error);
        });
        try {
            run.attach(() -> {
                reply.completion.completeExceptionally(new CancellationException());
                reply.cancelSubscription();
                exchange.cancel(true);
            });
            while (true) {
                run.check();
                if (System.nanoTime() - reply.lastActivity > 20_000_000_000L)
                    throw new IllegalStateException("连接 DeepSeek 超时或中断，请稍后重试");
                try { return reply.completion.get(200, TimeUnit.MILLISECONDS); }
                catch (TimeoutException ignored) { /* Poll the request deadline without blocking cancellation. */ }
            }
        } catch (ExecutionException error) {
            run.check();
            Throwable cause = error.getCause();
            if (cause instanceof IllegalStateException failure) throw failure;
            if (cause instanceof CancellationException) throw new CancellationException();
            throw new IllegalStateException("连接 DeepSeek 超时或中断，请稍后重试", cause);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CancellationException();
        } finally {
            reply.cancelSubscription();
            exchange.cancel(true);
            run.detach();
        }
    }

    private IllegalStateException providerError(int status) {
        String message = switch (status) {
            case 401, 403 -> "DeepSeek 密钥或访问权限无效，请检查服务器配置";
            case 402 -> "DeepSeek 账户余额不足";
            case 429 -> "DeepSeek 当前请求较多，请稍后重试";
            default -> "DeepSeek 服务暂时不可用（HTTP " + status + "）";
        };
        return new IllegalStateException(message);
    }

    private final class StreamReply implements Flow.Subscriber<String> {
        final CompletableFuture<ObjectNode> completion = new CompletableFuture<>();
        final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();
        final StringBuilder content = new StringBuilder();
        final Map<Integer, ObjectNode> calls = new LinkedHashMap<>();
        final AgentRun run;
        final Consumer<String> onText;
        volatile long lastActivity = System.nanoTime();
        String finishReason;
        int received;
        StreamReply(AgentRun run, Consumer<String> onText) { this.run = run; this.onText = onText; }
        public void onSubscribe(Flow.Subscription value) {
            if (!subscription.compareAndSet(null, value) || completion.isDone()) value.cancel();
            else value.request(Long.MAX_VALUE);
        }
        void cancelSubscription() {
            var current = subscription.get();
            if (current != null) current.cancel();
        }
        public void onNext(String line) {
            if (completion.isDone()) return;
            try {
                run.check();
                lastActivity = System.nanoTime();
                if ((received += line.length()) > 1_000_000) throw new IllegalStateException("模型响应过长，请缩短行程");
                if (!line.startsWith("data:")) return;
                String data = line.substring(5).trim();
                if (data.isEmpty()) return;
                if ("[DONE]".equals(data)) {
                    if (!("stop".equals(finishReason) || "tool_calls".equals(finishReason)))
                        throw new IllegalStateException("模型回复未完整结束，请重试或缩短行程");
                    ObjectNode answer = json.createObjectNode().put("role", "assistant").put("content", content.toString());
                    if (!calls.isEmpty()) {
                        var output = answer.putArray("tool_calls");
                        for (var call : calls.values()) {
                            if (call.path("id").asText().isBlank()) throw new IllegalStateException("模型工具调用缺少标识");
                            output.add(call);
                        }
                    }
                    completion.complete(answer);
                    cancelSubscription();
                    return;
                }
                JsonNode chunk = json.readTree(data);
                if (chunk.has("error")) throw new IllegalStateException("DeepSeek 返回了错误，请稍后重试");
                for (JsonNode choice : chunk.path("choices")) {
                    if (choice.hasNonNull("finish_reason")) finishReason = choice.get("finish_reason").asText();
                    JsonNode delta = choice.path("delta");
                    if (delta.path("content").isTextual()) {
                        String text = delta.get("content").asText();
                        content.append(text);
                        onText.accept(text);
                    }
                    for (JsonNode part : delta.path("tool_calls")) {
                        int index = part.path("index").asInt(-1);
                        if (index < 0 || index > 7) throw new IllegalStateException("模型工具调用过多");
                        ObjectNode call = calls.computeIfAbsent(index, key -> {
                            var node = json.createObjectNode().put("type", "function");
                            node.putObject("function").put("name", "").put("arguments", "");
                            return node;
                        });
                        if (part.hasNonNull("id")) call.put("id", part.get("id").asText());
                        ObjectNode fn = (ObjectNode) call.get("function");
                        for (String field : List.of("name", "arguments"))
                            if (part.path("function").hasNonNull(field))
                                fn.put(field, fn.path(field).asText() + part.get("function").get(field).asText());
                    }
                }
            } catch (Exception error) {
                completion.completeExceptionally(error);
                cancelSubscription();
            }
        }
        public void onError(Throwable error) { completion.completeExceptionally(error); }
        public void onComplete() {
            completion.completeExceptionally(new IllegalStateException("模型回复未完整结束，请重试或缩短行程"));
        }
    }
}
