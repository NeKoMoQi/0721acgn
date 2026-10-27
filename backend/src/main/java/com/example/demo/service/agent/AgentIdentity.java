package com.example.demo.service.agent;

import com.example.demo.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Account identity comes only from a verified token, never from request JSON. */
public final class AgentIdentity {
    private AgentIdentity() {}
    public static String account(HttpServletRequest request) {
        try {
            Object id = JwtUtils.parseJwt(request.getHeader("token")).get("id");
            if (id == null || !id.toString().matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException();
            return "user:" + id;
        } catch (Exception error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后保存或读取行程"); }
    }
    public static String conversationOwner(HttpServletRequest request) {
        if (request.getHeader("token") != null && !request.getHeader("token").isBlank()) return account(request);
        String device = request.getHeader("X-Acgn-Client");
        return device != null && device.matches("[A-Za-z0-9_-]{8,100}") ? "guest:" + device : "legacy";
    }
}
