package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagRequest {
    
    private String question;
    
    @JsonProperty("with_trace")
    private boolean withTrace = false;
    
    @JsonProperty("with_timing")
    private boolean withTiming = false;

    @JsonProperty("top_k")
    private Integer topK;

    private String strategy;

    @JsonProperty("knowledge_base_version")
    private String knowledgeBaseVersion;

    @JsonProperty("trace_id")
    private String traceId;
    
    // 构造函数
    public RagRequest() {}
    
    public RagRequest(String question) {
        this.question = question;
    }
    
    // Getter和Setter
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public boolean isWithTrace() {
        return withTrace;
    }
    
    public void setWithTrace(boolean withTrace) {
        this.withTrace = withTrace;
    }
    
    public boolean isWithTiming() {
        return withTiming;
    }
    
    public void setWithTiming(boolean withTiming) {
        this.withTiming = withTiming;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getKnowledgeBaseVersion() {
        return knowledgeBaseVersion;
    }

    public void setKnowledgeBaseVersion(String knowledgeBaseVersion) {
        this.knowledgeBaseVersion = knowledgeBaseVersion;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
