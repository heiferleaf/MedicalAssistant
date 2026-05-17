package com.whu.medicalbackend.agent.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RagProperties {

    @Value("${ai.rag.request.max-question-length:1000}")
    private int maxQuestionLength;

    @Value("${ai.rag.request.default-top-k:5}")
    private int defaultTopK;

    @Value("${ai.rag.request.max-top-k:10}")
    private int maxTopK;

    @Value("${ai.rag.request.default-strategy:hybrid}")
    private String defaultStrategy;

    @Value("${ai.rag.request.knowledge-base-version:default}")
    private String knowledgeBaseVersion;

    @Value("${ai.rag.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${ai.rag.cache.ttl-seconds:1800}")
    private long cacheTtlSeconds;

    @Value("${ai.rag.cache.ttl-jitter-seconds:300}")
    private long cacheTtlJitterSeconds;

    @Value("${ai.rag.cache.null-ttl-seconds:300}")
    private long nullCacheTtlSeconds;

    @Value("${ai.rag.cache.lock-wait-seconds:1}")
    private long cacheLockWaitSeconds;

    @Value("${ai.rag.cache.lock-lease-seconds:10}")
    private long cacheLockLeaseSeconds;

    @Value("${ai.rag.bulkhead.max-concurrent:20}")
    private int bulkheadMaxConcurrent;

    @Value("${ai.rag.bulkhead.max-wait-ms:100}")
    private long bulkheadMaxWaitMs;

    public int getMaxQuestionLength() {
        return maxQuestionLength;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public int getMaxTopK() {
        return maxTopK;
    }

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public String getKnowledgeBaseVersion() {
        return knowledgeBaseVersion;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public long getCacheTtlJitterSeconds() {
        return cacheTtlJitterSeconds;
    }

    public long getNullCacheTtlSeconds() {
        return nullCacheTtlSeconds;
    }

    public long getCacheLockWaitSeconds() {
        return cacheLockWaitSeconds;
    }

    public long getCacheLockLeaseSeconds() {
        return cacheLockLeaseSeconds;
    }

    public int getBulkheadMaxConcurrent() {
        return bulkheadMaxConcurrent;
    }

    public long getBulkheadMaxWaitMs() {
        return bulkheadMaxWaitMs;
    }
}
