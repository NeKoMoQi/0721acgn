package com.example.demo.controller;

import com.example.demo.dto.Live2dAgentChatResponse;
import com.example.demo.service.Live2dAgentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Map;
import java.util.function.BiConsumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class Live2dAgentControllerTests {
    private Live2dAgentService service;
    private Live2dAgentController controller;
    private MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(Live2dAgentService.class);
        controller = new Live2dAgentController(service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    @AfterEach void stop() { controller.shutdown(); }

    @Test void streamsTypedEventsAndDisablesProxyBuffering() throws Exception {
        when(service.run(any(), any(), any())).thenAnswer(invocation -> {
            BiConsumer<String, Object> events = invocation.getArgument(2);
            events.accept("meta", Map.of("conversationId", "fixture"));
            events.accept("status", Map.of("text", "正在检索"));
            events.accept("delta", Map.of("text", "你好"));
            var result = new Live2dAgentChatResponse(); result.setReply("你好"); return result;
        });
        var result = mvc.perform(post("/acgn/live2d/agent/stream").contentType("application/json")
                        .content("{\"query\":\"你好\",\"mode\":\"chat\"}"))
                .andExpect(request().asyncStarted()).andExpect(header().string("X-Accel-Buffering", "no")).andReturn();
        result.getAsyncResult(5_000);
        var response = mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn().getResponse();
        String body = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("event:meta")); assertTrue(body.contains("event:delta"));
        assertTrue(body.contains("event:done")); assertTrue(body.contains("你好"));
    }

    @Test void configurationErrorIsAnErrorEventNotAFakePlan() throws Exception {
        when(service.run(any(), any(), any())).thenThrow(new IllegalStateException("DeepSeek 尚未配置"));
        var result = mvc.perform(post("/acgn/live2d/agent/stream").contentType("application/json").content("{\"query\":\"宇治\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5_000);
        String body = mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("event:error")); assertFalse(body.contains("event:done"));
    }
}
