package com.example.demo.dto;

/**
 * Front-end → backend: one turn of the Live2D companion agent.
 */
public class Live2dAgentChatRequest {
    private String query;
    /** When null/blank, Qianfan request omits conversation_id (stateless turn). */
    private String conversationId;

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
