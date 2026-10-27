package com.example.demo.service.agent;

import com.example.demo.mapper.postgresql.PilgrimageAgentMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

@Component
public class PilgrimageDataStore {
    private final PilgrimageAgentMapper mapper;
    private final ObjectMapper json = new ObjectMapper();
    public PilgrimageDataStore(PilgrimageAgentMapper mapper) { this.mapper = mapper; }
    public List<Map<String, Object>> aliases() { return database(mapper::aliases); }
    public List<Map<String, Object>> spotInfo() { return database(mapper::spotInfo); }
    public List<Map<String, Object>> routes(String owner) { return database(() -> mapper.routes(owner)); }
    public Map<String, Object> route(String id, String owner) {
        requireUuid(id); return database(() -> mapper.route(id, owner));
    }
    public boolean delete(String id, String owner) {
        requireUuid(id); return database(() -> mapper.deleteRoute(id, owner)) > 0;
    }
    public String save(String owner, String title, JsonNode plan) {
        try {
            String body = plan.toString();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            return database(() -> mapper.saveRoute(UUID.randomUUID().toString(), owner, title, body, hash));
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    public Map<String, Object> session(String id, String owner, String mode) {
        requireUuid(id); return database(() -> mapper.session(id, owner, mode));
    }
    public long saveSession(String id, String owner, String mode, JsonNode payload, long version) {
        int affected = database(() -> version == 0 ? mapper.insertSession(id, owner, mode, payload.toString())
                : mapper.updateSession(id, owner, payload.toString(), version));
        if (affected != 1) throw new IllegalStateException("会话已更新或过期，请开启新对话后重试");
        return version + 1;
    }
    public JsonNode parse(String text) {
        try { return json.readTree(text); }
        catch (Exception error) { throw new IllegalStateException("保存的巡礼数据格式无效", error); }
    }
    private static void requireUuid(String id) {
        try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
        catch (Exception error) { throw new IllegalArgumentException("无效的行程或会话编号"); }
    }
    private <T> T database(Supplier<T> operation) {
        try { return operation.get(); }
        catch (DataAccessException error) {
            throw new IllegalStateException("巡礼数据库暂不可用，请确认已执行 pilgrimage_agent_v2.sql 并检查数据库连接", error);
        }
    }
}
