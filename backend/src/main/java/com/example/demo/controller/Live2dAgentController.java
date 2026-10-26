package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.dto.Live2dAgentChatRequest;
import com.example.demo.dto.Live2dAgentChatResponse;
import com.example.demo.service.Live2dAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Live2dAgentController {

    private final Live2dAgentService live2dAgentService;

    public Live2dAgentController(Live2dAgentService live2dAgentService) {
        this.live2dAgentService = live2dAgentService;
    }

    /**
     * Live2D 页对话：前端只调此接口；千帆 Token 仅在后端配置。
     * 路径含 /acgn，与 {@link com.example.demo.Interceptor.LoginCheakInterceptor} 放行规则一致。
     */
    @PostMapping("/acgn/live2d/agent/chat")
    public Result<Live2dAgentChatResponse> chat(@RequestBody Live2dAgentChatRequest request) {
        try {
            Live2dAgentChatResponse data = live2dAgentService.chat(request);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.warn("live2d agent chat bad request: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("live2d agent chat failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
