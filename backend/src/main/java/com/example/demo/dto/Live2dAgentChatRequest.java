package com.example.demo.dto;

/**
 * Front-end → backend: one turn of the Live2D companion agent.
 */
public class Live2dAgentChatRequest {
    private String query;
    /** Opaque server-owned session; omitted for a new conversation. */
    private String conversationId;
    private String mode = "route";
    private com.fasterxml.jackson.databind.JsonNode contextPlan;
    public com.fasterxml.jackson.databind.JsonNode getContextPlan() { return contextPlan; }
    public void setContextPlan(com.fasterxml.jackson.databind.JsonNode value) { contextPlan = value; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String ownerKey = "legacy";
    public String getOwnerKey() { return ownerKey; }
    public void setOwnerKey(String value) { ownerKey = value; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
