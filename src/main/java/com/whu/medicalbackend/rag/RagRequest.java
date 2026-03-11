package com.whu.medicalbackend.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagRequest {
    
    private String question;
    
    @JsonProperty("with_trace")
    private boolean withTrace = false;
    
    @JsonProperty("with_timing")
    private boolean withTiming = false;
    
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
}