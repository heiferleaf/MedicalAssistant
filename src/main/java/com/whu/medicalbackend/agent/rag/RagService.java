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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final AiHttpClient aiHttpClient;
    private final RagCacheService ragCacheService;
    private final RagProperties properties;
    private final MeterRegistry meterRegistry;
    private final Semaphore bulkhead;

    public RagService(AiHttpClient aiHttpClient,
                      RagCacheService ragCacheService,
                      RagProperties properties,
                      MeterRegistry meterRegistry) {
        this.aiHttpClient = aiHttpClient;
        this.ragCacheService = ragCacheService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.bulkhead = new Semaphore(Math.max(1, properties.getBulkheadMaxConcurrent()));
    }

    public RagResponse queryRag(RagRequest request) {
        validateAndNormalize(request);
        applyDefaults(request);

        String question = request.getQuestion();
        String questionHash = ragCacheService.questionHash(question);
        String cacheKey = ragCacheService.cacheKey(request);
        long startedAt = System.nanoTime();
        boolean success = false;
        boolean cacheHit = false;
        String resultTag = "failure";

        try {
            RagResponse cached = ragCacheService.get(cacheKey);
            if (cached != null) {
                cacheHit = true;
                success = cached.isSuccess();
                resultTag = success ? "cache_hit" : "cache_hit_failure";
                cached.setElapsedMs(elapsedMs(startedAt));
                return cached;
            }

            RagResponse response = ragCacheService.isCacheEnabled()
                    ? queryProviderWithHotspotProtection(request, cacheKey)
                    : queryProviderWithBulkhead(request);
            success = response.isSuccess();
            if (Boolean.TRUE.equals(response.getCacheHit())) {
                cacheHit = true;
                resultTag = success ? "cache_hit_after_wait" : "cache_hit_failure";
                response.setElapsedMs(elapsedMs(startedAt));
                return response;
            }

            resultTag = success ? "success" : "provider_failure";
            response.setCacheHit(false);
            response.setElapsedMs(elapsedMs(startedAt));
            response.setProviderStatus("provider");

            if (success) {
                ragCacheService.put(cacheKey, response);
            }
            return response;
        } catch (RagServiceException e) {
            resultTag = e.getErrorCode();
            throw e;
        } catch (RestClientResponseException e) {
            resultTag = "provider_http_error";
            throw new RagServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG_PROVIDER_HTTP_ERROR",
                    "RAG服务返回异常状态: " + e.getStatusCode().value(),
                    e
            );
        } catch (ResourceAccessException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                resultTag = "provider_timeout";
                throw new RagServiceException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "RAG_PROVIDER_TIMEOUT",
                        "RAG服务调用超时",
                        e
                );
            }
            resultTag = "provider_unavailable";
            throw new RagServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAG_PROVIDER_UNAVAILABLE",
                    "RAG服务不可用: " + e.getMessage(),
                    e
            );
        } catch (Exception e) {
            resultTag = "bad_response";
            throw new RagServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG_PROVIDER_BAD_RESPONSE",
                    "RAG服务响应异常: " + e.getMessage(),
                    e
            );
        } finally {
            long elapsedMs = elapsedMs(startedAt);
            recordMetrics(resultTag, cacheHit, elapsedMs);
            logger.info("RAG query finished questionHash={}, elapsedMs={}, success={}, cacheHit={}, result={}",
                    questionHash, elapsedMs, success, cacheHit, resultTag);
        }
    }

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
            return queryProviderWithBulkhead(request);
        } finally {
            ragCacheService.unlock(lock);
        }
    }

    private RagResponse queryProviderWithBulkhead(RagRequest request) {
        boolean acquired = false;
        try {
            acquired = bulkhead.tryAcquire(properties.getBulkheadMaxWaitMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
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

    private RagResponse queryProvider(RagRequest request) {
        RagResponse response = aiHttpClient.postJson("/rag/query", request, RagResponse.class);

        if (response == null) {
            throw new RagServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG_EMPTY_RESPONSE",
                    "RAG服务返回空响应"
            );
        }
        return response;
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
        if (question.length() > properties.getMaxQuestionLength()) {
            throw new RagServiceException(
                    HttpStatus.BAD_REQUEST,
                    "RAG_QUESTION_TOO_LONG",
                    "question 长度不能超过 " + properties.getMaxQuestionLength()
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
        } else {
            request.setKnowledgeBaseVersion(request.getKnowledgeBaseVersion().trim());
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

    private void recordMetrics(String result, boolean cacheHit, long elapsedMs) {
        meterRegistry.counter("ai.rag.request",
                "result", result,
                "cache", cacheHit ? "hit" : "miss").increment();
        meterRegistry.counter("ai.rag.cache",
                "result", cacheHit ? "hit" : "miss").increment();
        if ("RAG_BUSY".equals(result)) {
            meterRegistry.counter("ai.rag.bulkhead.rejected").increment();
        }
        if ("provider_timeout".equals(result)) {
            meterRegistry.counter("ai.rag.timeout").increment();
        }
        Timer.builder("ai.rag.duration")
                .tag("result", result)
                .tag("cache", cacheHit ? "hit" : "miss")
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
    }
}
