package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.dto.Live2dAgentChatRequest;
import com.example.demo.dto.Live2dAgentChatResponse;
import com.example.demo.service.Live2dAgentService;
import com.example.demo.service.agent.AgentRun;
import com.example.demo.service.agent.AgentIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Live2dAgentController {

    private final Live2dAgentService live2dAgentService;
    private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), runnable -> {
                Thread thread = new Thread(runnable, "pilgrimage-agent");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pilgrimage-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public Live2dAgentController(Live2dAgentService live2dAgentService) {
        this.live2dAgentService = live2dAgentService;
    }

    /**
     * Compatibility endpoint for the existing Live2D bundle; API keys remain server-side.
     * 路径含 /acgn，与 {@link com.example.demo.Interceptor.LoginCheakInterceptor} 放行规则一致。
     */
    @PostMapping("/acgn/live2d/agent/chat")
    public Result<Live2dAgentChatResponse> chat(@RequestBody Live2dAgentChatRequest request, HttpServletRequest http) {
        request.setOwnerKey(AgentIdentity.conversationOwner(http));
        try {
            Live2dAgentChatResponse data = live2dAgentService.chat(request);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.warn("live2d agent chat bad request: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("live2d agent chat failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/acgn/live2d/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Live2dAgentChatRequest request, HttpServletResponse response, HttpServletRequest http) {
        request.setOwnerKey(AgentIdentity.conversationOwner(http));
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(95_000L);
        AgentRun run = new AgentRun();
        AtomicReference<Future<?>> future = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();
        Runnable cancel = () -> {
            run.cancel();
            var work = future.get();
            if (work != null) work.cancel(true);
            var ping = heartbeat.get();
            if (ping != null) ping.cancel(false);
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(() -> {
            send(emitter, "error", Map.of("message", "规划超时，请缩小范围后重试"));
            cancel.run();
            emitter.complete();
        });
        emitter.onError(error -> cancel.run());
        heartbeat.set(heartbeats.scheduleAtFixedRate(() -> {
            try { emitter.send(SseEmitter.event().comment("keepalive")); }
            catch (Exception error) { cancel.run(); emitter.complete(); }
        }, 10, 10, TimeUnit.SECONDS));
        try {
            future.set(workers.submit(() -> {
                try {
                    run.check();
                    var result = live2dAgentService.run(request, run, (event, payload) -> {
                        run.check();
                        if (!send(emitter, event, payload)) {
                            run.cancel();
                            throw new CancellationException();
                        }
                    });
                    run.check();
                    send(emitter, "done", result);
                } catch (CancellationException ignored) {
                    // Browser aborts do not become fake assistant replies.
                } catch (IllegalArgumentException | IllegalStateException error) {
                    send(emitter, "error", Map.of("message", error.getMessage()));
                } catch (Exception error) {
                    log.error("pilgrimage agent failed", error);
                    send(emitter, "error", Map.of("message", "巡礼服务暂时不可用，请稍后重试"));
                } finally {
                    var ping = heartbeat.get();
                    if (ping != null) ping.cancel(false);
                    emitter.complete();
                }
            }));
        } catch (RejectedExecutionException error) {
            send(emitter, "error", Map.of("message", "当前规划请求较多，请稍后重试"));
            cancel.run();
            emitter.complete();
        }
        return emitter;
    }

    private boolean send(SseEmitter emitter, String event, Object payload) {
        try { emitter.send(SseEmitter.event().name(event).data(payload, MediaType.APPLICATION_JSON)); return true; }
        catch (IOException | IllegalStateException error) { return false; }
    }

    @PreDestroy
    public void shutdown() { workers.shutdownNow(); heartbeats.shutdownNow(); }
}
