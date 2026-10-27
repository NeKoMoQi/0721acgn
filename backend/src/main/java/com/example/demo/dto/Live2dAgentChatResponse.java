package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Backend → front-end: assistant reply and session id for multi-turn chat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Live2dAgentChatResponse {
    private String reply;
    private String conversationId;
    private Live2dAgentLive2dHints live2d;
    private JsonNode routePlan;
    private JsonNode knowledgeSources;
    public JsonNode getKnowledgeSources() { return knowledgeSources; }
    public void setKnowledgeSources(JsonNode value) { knowledgeSources = value; }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Live2dAgentLive2dHints getLive2d() {
        return live2d;
    }

    public void setLive2d(Live2dAgentLive2dHints live2d) {
        this.live2d = live2d;
    }

    public JsonNode getRoutePlan() {
        return routePlan;
    }

    public void setRoutePlan(JsonNode routePlan) {
        this.routePlan = routePlan;
    }
}
