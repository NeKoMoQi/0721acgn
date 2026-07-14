package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Optional hints for the Cubism front-end (motion group / expression ids from model3.json).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Live2dAgentLive2dHints {
    private String motionGroup;
    private Integer motionIndex;
    private String expressionId;
    private Integer messageDurationMs;

    public String getMotionGroup() {
        return motionGroup;
    }

    public void setMotionGroup(String motionGroup) {
        this.motionGroup = motionGroup;
    }

    public Integer getMotionIndex() {
        return motionIndex;
    }

    public void setMotionIndex(Integer motionIndex) {
        this.motionIndex = motionIndex;
    }

    public String getExpressionId() {
        return expressionId;
    }

    public void setExpressionId(String expressionId) {
        this.expressionId = expressionId;
    }

    public Integer getMessageDurationMs() {
        return messageDurationMs;
    }

    public void setMessageDurationMs(Integer messageDurationMs) {
        this.messageDurationMs = messageDurationMs;
    }
}
