package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RagResponse {
    
    private String answer;
    private boolean success;
    private String error;
    private Map<String, Object> timings;
    private Map<String, Object> trace;
    private List<Map<String, Object>> sources;
    @JsonProperty("cache_hit")
    private Boolean cacheHit;
    @JsonProperty("elapsed_ms")
    private Long elapsedMs;
    @JsonProperty("error_code")
    private String errorCode;
    @JsonProperty("provider_status")
    private String providerStatus;
    
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

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public void setSources(List<Map<String, Object>> sources) {
        this.sources = sources;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public void setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
    }
}
