package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.service.agent.AgentIdentity;
import com.example.demo.service.agent.PilgrimageRouteLibrary;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/xunli/agent/routes")
public class PilgrimageRouteController {
    private final PilgrimageRouteLibrary library;
    public PilgrimageRouteController(PilgrimageRouteLibrary library) { this.library = library; }
    @GetMapping
    public Result<?> list(HttpServletRequest request) {
        String owner = AgentIdentity.account(request);
        return execute(() -> library.list(owner));
    }
    @PostMapping
    public Result<?> save(@RequestBody JsonNode body, HttpServletRequest request) {
        String owner = AgentIdentity.account(request);
        return execute(() -> library.save(owner, body));
    }
    @GetMapping("/{id}")
    public Result<?> load(@PathVariable String id, HttpServletRequest request) {
        String owner = AgentIdentity.account(request);
        return execute(() -> library.load(owner, id));
    }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id, HttpServletRequest request) {
        String owner = AgentIdentity.account(request);
        return execute(() -> { library.delete(owner, id); return true; });
    }
    private Result<?> execute(Supplier<Object> operation) {
        try { return Result.success(operation.get()); }
        catch (IllegalArgumentException | IllegalStateException error) { return Result.error(error.getMessage()); }
    }
}
