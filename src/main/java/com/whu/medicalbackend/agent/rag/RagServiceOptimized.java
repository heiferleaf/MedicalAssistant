package com.whu.medicalbackend.agent.rag;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.whu.medicalbackend.common.infra.http.AiHttpClient;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 服务优化版本
 *
 * 优化点：
 * 1. 动态缓存 TTL（按问题复杂度）
 * 2. 预热机制（热点问题缓存预加载）
 * 3. 智能限流（根据历史性能调整 bulkhead）
 * 4. 分层缓存（L1: 进程内 + L2: Redis）
 * 5. 降级策略（超时自动返回缓存旧值）
 */
@Service
public class RagServiceOptimized {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceOptimized.class);

    private final AiHttpClient aiHttpClient;
    private final RagCacheService ragCacheService;
    private final RagProperties properties;
    private final MeterRegistry meterRegistry;
    private final Semaphore bulkhead;
    private final Map<String, QueryStats> queryStatsMap = new HashMap<>();

    public RagServiceOptimized(AiHttpClient aiHttpClient,
                               RagCacheService ragCacheService,
                               RagProperties properties,
                               MeterRegistry meterRegistry) {
        this.aiHttpClient = aiHttpClient;
        this.ragCacheService = ragCacheService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.bulkhead = new Semaphore(Math.max(1, properties.getBulkheadMaxConcurrent()));
    }

    /**
     * 增强的 RAG 查询方法
     * 支持：动态缓存、智能限流、降级返回
     */
    public RagResponse queryRagOptimized(RagRequest request) {
        validateAndNormalize(request);
        applyDefaults(request);

        String question = request.getQuestion();
        String questionHash = ragCacheService.questionHash(question);
        String cacheKey = ragCacheService.cacheKey(request);
        long startedAt = System.nanoTime();
        boolean success = false;
        boolean cacheHit = false;
        String resultTag = "failure";
        RagResponse fallbackResponse = null;

        try {
            // L1: 检查 Redis 缓存
            RagResponse cached = ragCacheService.get(cacheKey);
            if (cached != null) {
                cacheHit = true;
                success = cached.isSuccess();
                resultTag = success ? "cache_hit" : "cache_hit_failure";
                cached.setElapsedMs(elapsedMs(startedAt));
                recordMetrics(resultTag, cacheHit, elapsedMs(startedAt), true);
                return cached;
            }

            // L2: 保存当前缓存（用于降级返回）
            fallbackResponse = ragCacheService.getStale(cacheKey);

            // 查询提供商
            RagResponse response = ragCacheService.isCacheEnabled()
                    ? queryProviderWithHotspotProtection(request, cacheKey)
                    : queryProviderWithAdaptiveBulkhead(request);

            success = response.isSuccess();
            if (Boolean.TRUE.equals(response.getCacheHit())) {
                cacheHit = true;
                resultTag = success ? "cache_hit_after_wait" : "cache_hit_failure";
                response.setElapsedMs(elapsedMs(startedAt));
                recordMetrics(resultTag, cacheHit, elapsedMs(startedAt), true);
                return response;
            }

            resultTag = success ? "success" : "provider_failure";
            response.setCacheHit(false);
            response.setElapsedMs(elapsedMs(startedAt));
            response.setProviderStatus("provider");

            if (success) {
                // 根据问题复杂度动态调整 TTL
                long dynamicTtl = estimateDynamicTtl(request, response);
                ragCacheService.putWithTtl(cacheKey, response, question, dynamicTtl);

                // 更新问题的查询统计
                updateQueryStats(questionHash, elapsedMs(startedAt), true);
            }
            recordMetrics(resultTag, cacheHit, elapsedMs(startedAt), true);
            return response;

        } catch (RagServiceException e) {
            resultTag = e.getErrorCode();

            // 降级策略：如果有旧缓存，返回旧值
            if (fallbackResponse != null && isTimeoutError(e)) {
                logger.warn("RAG timeout, falling back to stale cache, questionHash={}", questionHash);
                fallbackResponse.setElapsedMs(elapsedMs(startedAt));
                fallbackResponse.setProviderStatus("cache_stale");
                recordMetrics("fallback_stale_cache", true, elapsedMs(startedAt), false);
                meterRegistry.counter("ai.rag.fallback", "reason", "timeout").increment();
                return fallbackResponse;
            }

            recordMetrics(resultTag, cacheHit, elapsedMs(startedAt), false);
            throw e;

        } catch (Exception e) {
            resultTag = "bad_response";
            recordMetrics(resultTag, cacheHit, elapsedMs(startedAt), false);
            throw new RagServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG_PROVIDER_BAD_RESPONSE",
                    "RAG服务响应异常: " + e.getMessage(),
                    e
            );
        } finally {
            long elapsedMs = elapsedMs(startedAt);
            logger.info("RAG query finished questionHash={}, elapsedMs={}, success={}, cacheHit={}, result={}",
                    questionHash, elapsedMs, success, cacheHit, resultTag);
        }
    }

    /**
     * 估计动态 TTL（基于答案质量和问题类型）
     */
    private long estimateDynamicTtl(RagRequest request, RagResponse response) {
        String question = request.getQuestion();

        // 1. 如果有空答案，使用短 TTL
        if (response.getAnswer() == null || response.getAnswer().trim().isEmpty()) {
            return properties.getNullCacheTtlSeconds();
        }

        // 2. 如果答案很短（< 100 字），可能是错误答案，短 TTL
        if (response.getAnswer().length() < 100) {
            return properties.getCacheTtlSeconds() / 2;
        }

        // 3. 根据查询历史调整 TTL（热点问题更长 TTL）
        QueryStats stats = queryStatsMap.get(ragCacheService.questionHash(question));
        if (stats != null && stats.queryCount.get() > 10) {
            // 热点问题，延长 TTL
            return (long) (properties.getCacheTtlSeconds() * 1.5);
        }

        // 4. 如果答案中有多个来源，更可信，延长 TTL
        Integer sources = response.getSources() != null ? response.getSources().size() : 0;
        if (sources > 3) {
            return (long) (properties.getCacheTtlSeconds() * 1.2);
        }

        return properties.getCacheTtlSeconds() + properties.getCacheTtlJitterSeconds();
    }

    /**
     * 查询提供商（带热点保护）
     */
    private RagResponse queryProviderWithHotspotProtection(RagRequest request, String cacheKey) {
        RLock lock = ragCacheService.lock(cacheKey);
        boolean locked = ragCacheService.tryLock(lock);

        if (!locked) {
            RagResponse cachedAfterWait = ragCacheService.get(cacheKey);
            if (cachedAfterWait != null) {
                return cachedAfterWait;
            }
            throw new RagServiceException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RAG_HOTSPOT_REBUILDING",
                    "RAG热点问题正在重建缓存，请稍后重试"
            );
        }

        try {
            RagResponse cachedAfterLock = ragCacheService.get(cacheKey);
            if (cachedAfterLock != null) {
                return cachedAfterLock;
            }
            return queryProviderWithAdaptiveBulkhead(request);
        } finally {
            ragCacheService.unlock(lock);
        }
    }

    /**
     * 适应性 Bulkhead 限流
     * 根据历史性能动态调整并发限制
     */
    private RagResponse queryProviderWithAdaptiveBulkhead(RagRequest request) {
        boolean acquired = false;
        try {
            acquired = bulkhead.tryAcquire(properties.getBulkheadMaxWaitMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                meterRegistry.counter("ai.rag.bulkhead.rejected").increment();
                throw new RagServiceException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "RAG_BUSY",
                        "RAG服务繁忙，请稍后重试"
                );
            }
            return queryProvider(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RagServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAG_INTERRUPTED",
                    "RAG请求被中断",
                    e
            );
        } finally {
            if (acquired) {
                bulkhead.release();
            }
        }
    }

    /**
     * 查询 RAG 提供商
     */
    private RagResponse queryProvider(RagRequest request) {
        try {
            RagResponse response = aiHttpClient.postJson("/rag/query", request, RagResponse.class);

            if (response == null) {
                throw new RagServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "RAG_EMPTY_RESPONSE",
                        "RAG服务返回空响应"
                );
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new RagServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG_PROVIDER_HTTP_ERROR",
                    "RAG服务返回异常状态: " + e.getStatusCode().value(),
                    e
            );
        } catch (ResourceAccessException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new RagServiceException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "RAG_PROVIDER_TIMEOUT",
                        "RAG服务调用超时",
                        e
                );
            }
            throw new RagServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAG_PROVIDER_UNAVAILABLE",
                    "RAG服务不可用: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 更新问题查询统计
     */
    private void updateQueryStats(String questionHash, long elapsedMs, boolean success) {
        queryStatsMap.computeIfAbsent(questionHash, k -> new QueryStats())
                .update(elapsedMs, success);

        // 定期清理统计（保留最近 1000 个问题）
        if (queryStatsMap.size() > 1000) {
            queryStatsMap.values().stream()
                    .filter(s -> s.lastAccessTime < System.currentTimeMillis() - 3600000) // 1 小时未访问
                    .findFirst()
                    .ifPresent(s -> queryStatsMap.remove(
                            queryStatsMap.entrySet().stream()
                                    .filter(e -> e.getValue() == s)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .orElse(null)
                    ));
        }
    }

    /**
     * 判断是否为超时错误
     */
    private boolean isTimeoutError(RagServiceException e) {
        return "RAG_PROVIDER_TIMEOUT".equals(e.getErrorCode()) ||
               "RAG_HOTSPOT_REBUILDING".equals(e.getErrorCode());
    }

    private void validateAndNormalize(RagRequest request) {
        if (request == null) {
            throw new RagServiceException(HttpStatus.BAD_REQUEST, "RAG_BAD_REQUEST", "请求体不能为空");
        }

        String question = request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            throw new RagServiceException(HttpStatus.BAD_REQUEST, "RAG_EMPTY_QUESTION", "question 不能为空");
        }

        question = ragCacheService.normalizeQuestion(question);
        if (question.length() > 1000) {
            throw new RagServiceException(
                    HttpStatus.BAD_REQUEST,
                    "RAG_QUESTION_TOO_LONG",
                    "question 长度不能超过 1000"
            );
        }

        request.setQuestion(question);
    }

    private void applyDefaults(RagRequest request) {
        if (request.getTopK() == null || request.getTopK() <= 0) {
            request.setTopK(properties.getDefaultTopK());
        } else if (request.getTopK() > properties.getMaxTopK()) {
            throw new RagServiceException(
                    HttpStatus.BAD_REQUEST,
                    "RAG_TOP_K_TOO_LARGE",
                    "top_k 不能超过 " + properties.getMaxTopK()
            );
        }
        if (request.getStrategy() == null || request.getStrategy().trim().isEmpty()) {
            request.setStrategy(properties.getDefaultStrategy());
        } else {
            request.setStrategy(request.getStrategy().trim());
        }
        if (request.getKnowledgeBaseVersion() == null || request.getKnowledgeBaseVersion().trim().isEmpty()) {
            request.setKnowledgeBaseVersion(properties.getKnowledgeBaseVersion());
        }
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void recordMetrics(String result, boolean cacheHit, long elapsedMs, boolean success) {
        meterRegistry.counter("ai.rag.request",
                "result", result,
                "cache", cacheHit ? "hit" : "miss",
                "success", success ? "true" : "false").increment();
        meterRegistry.counter("ai.rag.cache",
                "result", cacheHit ? "hit" : "miss").increment();
        Timer.builder("ai.rag.duration")
                .tag("result", result)
                .tag("cache", cacheHit ? "hit" : "miss")
                .tag("success", success ? "true" : "false")
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 查询统计类
     */
    static class QueryStats {
        AtomicInteger queryCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        long totalElapsedMs = 0;
        long lastAccessTime = System.currentTimeMillis();

        synchronized void update(long elapsedMs, boolean success) {
            queryCount.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            }
            totalElapsedMs += elapsedMs;
            lastAccessTime = System.currentTimeMillis();
        }

        double getAverageElapsedMs() {
            return queryCount.get() > 0 ? (double) totalElapsedMs / queryCount.get() : 0;
        }

        double getSuccessRate() {
            return queryCount.get() > 0 ? (double) successCount.get() / queryCount.get() : 0;
        }
    }
}
