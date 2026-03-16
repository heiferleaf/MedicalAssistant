package com.whu.medicalbackend.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RagResponse {
    
    private String answer;
    private boolean success;
    private String error;
    private Map<String, Object> timings;
    private Map<String, Object> trace;
    
    // 构造函数
    public RagResponse() {}
    
    // Getter和Setter
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public Map<String, Object> getTimings() {
        return timings;
    }
    
    public void setTimings(Map<String, Object> timings) {
        this.timings = timings;
    }
    
    public Map<String, Object> getTrace() {
        return trace;
    }
    
    public void setTrace(Map<String, Object> trace) {
        this.trace = trace;
    }
}