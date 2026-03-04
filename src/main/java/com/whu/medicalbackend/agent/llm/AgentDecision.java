package com.whu.medicalbackend.agent.llm;

import java.util.Map;

public class AgentDecision {

    public enum IntentType {
        DIRECT_ANSWER,
        TOOL_CALL,
        NEED_CONFIRM,
        CLARIFICATION,
        UNKNOWN
    }

    private final IntentType intentType;
    private final String message;
    private final String toolName;
    private final Map<String, Object> toolArgs;
    private final Map<String, Object> preview;
    private final String rawResponse;

    public AgentDecision(IntentType intentType, String message) {
        this(intentType, message, null, null, null, null);
    }

    public AgentDecision(IntentType intentType, String message, String toolName, Map<String, Object> toolArgs) {
        this(intentType, message, toolName, toolArgs, null, null);
    }

    public AgentDecision(IntentType intentType, String message, String toolName, Map<String, Object> toolArgs, Map<String, Object> preview, String rawResponse) {
        this.intentType = intentType;
        this.message = message;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.preview = preview;
        this.rawResponse = rawResponse;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public String getMessage() {
        return message;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getToolArgs() {
        return toolArgs;
    }

    public Map<String, Object> getPreview() {
        return preview;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public boolean isToolCall() {
        return intentType == IntentType.TOOL_CALL;
    }

    public boolean isNeedConfirm() {
        return intentType == IntentType.NEED_CONFIRM;
    }

    public boolean isDirectAnswer() {
        return intentType == IntentType.DIRECT_ANSWER;
    }

    public boolean isClarification() {
        return intentType == IntentType.CLARIFICATION;
    }

    public static AgentDecision directAnswer(String message) {
        return new AgentDecision(IntentType.DIRECT_ANSWER, message);
    }

    public static AgentDecision toolCall(String toolName, Map<String, Object> toolArgs, String message) {
        return new AgentDecision(IntentType.TOOL_CALL, message, toolName, toolArgs);
    }

    public static AgentDecision needConfirm(Map<String, Object> preview, String message) {
        return new AgentDecision(IntentType.NEED_CONFIRM, message, null, null, preview, null);
    }

    public static AgentDecision clarification(String message) {
        return new AgentDecision(IntentType.CLARIFICATION, message);
    }

    public static AgentDecision unknown(String message) {
        return new AgentDecision(IntentType.UNKNOWN, message);
    }
}
