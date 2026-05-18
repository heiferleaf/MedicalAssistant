package com.whu.medicalbackend.agent.flask;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 Flask AI 服务客户端
 * 封装所有 Flask 调用，集成 Resilience4j 限流/熔断/重试
 */
@Service
public class UnifiedFlaskClient {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedFlaskClient.class);

    private final RestClient flaskRestClient;
    private final ObjectMapper objectMapper;

    public UnifiedFlaskClient(RestClient flaskRestClient, ObjectMapper objectMapper) {
        this.flaskRestClient = flaskRestClient;
        this.objectMapper = objectMapper;
    }

    // ===== OCR =====

    @RateLimiter(name = "flaskOcr", fallbackMethod = "ocrFallback")
    @CircuitBreaker(name = "flaskOcr", fallbackMethod = "ocrFallback")
    @Retry(name = "flaskOcr", fallbackMethod = "ocrFallback")
    public Map<String, Object> recognizeDrug(byte[] imageBytes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("image", imageBytes);

        Object resp = flaskRestClient.post()
                .uri("/api/ocr/recognize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);

        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    @SuppressWarnings("unused")
    private Map<String, Object> ocrFallback(byte[] imageBytes, Throwable t) {
        logger.warn("OCR 服务降级: {}", t.getMessage());
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "error");
        fallback.put("message", "OCR 服务暂时不可用");
        return fallback;
    }

    // ===== RAG =====

    @RateLimiter(name = "flaskRag", fallbackMethod = "ragFallback")
    @CircuitBreaker(name = "flaskRag", fallbackMethod = "ragFallback")
    @Retry(name = "flaskRag", fallbackMethod = "ragFallback")
    public Map<String, Object> queryRag(String question, boolean withTrace, boolean withTiming) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("question", question);
        payload.put("with_trace", withTrace);
        payload.put("with_timing", withTiming);

        Object resp = flaskRestClient.post()
                .uri("/api/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);

        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    @SuppressWarnings("unused")
    private Map<String, Object> ragFallback(String question, boolean withTrace, boolean withTiming, Throwable t) {
        logger.warn("RAG 服务降级: {}", t.getMessage());
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "error");
        fallback.put("message", "RAG 知识库服务暂时不可用");
        return fallback;
    }

    // ===== Predict =====

    @RateLimiter(name = "flaskPredict", fallbackMethod = "predictFallback")
    @CircuitBreaker(name = "flaskPredict", fallbackMethod = "predictFallback")
    @Retry(name = "flaskPredict", fallbackMethod = "predictFallback")
    public Map<String, Object> analyzePredict(String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", text);

        Object resp = flaskRestClient.post()
                .uri("/api/predict/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);

        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    @SuppressWarnings("unused")
    private Map<String, Object> predictFallback(String text, Throwable t) {
        logger.warn("Predict 服务降级: {}", t.getMessage());
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "error");
        fallback.put("message", "Predict 服务暂时不可用");
        return fallback;
    }

    // ===== 服务健康检查 =====

    public boolean isFlaskAvailable() {
        try {
            var resp = flaskRestClient.get()
                    .uri("/api/agent/health")
                    .retrieve()
                    .body(Object.class);
            return resp != null;
        } catch (Exception e) {
            logger.warn("Flask 服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
