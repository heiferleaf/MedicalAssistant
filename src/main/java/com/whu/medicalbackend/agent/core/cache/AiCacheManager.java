package com.whu.medicalbackend.agent.core.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * AI 结果缓存管理器
 * 缓存 OCR、RAG、Predict 的重复请求结果，降低外部服务调用
 */
@Component
@ConditionalOnProperty(prefix = "ai.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(AiCacheManager.class);

    private static final String PREFIX_OCR = "ai:cache:ocr:";
    private static final String PREFIX_RAG = "ai:cache:rag:";
    private static final String PREFIX_PREDICT = "ai:cache:predict:";
    private static final String PREFIX_CHAT = "ai:cache:chat:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int ocrTtlSeconds;
    private final int ragTtlSeconds;
    private final int predictTtlSeconds;
    private final int chatTtlSeconds;

    public AiCacheManager(
            StringRedisTemplate redisTemplate,
            @Value("${ai.cache.enabled:true}") boolean enabled,
            @Value("${ai.cache.ocr-ttl-seconds:3600}") int ocrTtlSeconds,
            @Value("${ai.cache.rag-ttl-seconds:1800}") int ragTtlSeconds,
            @Value("${ai.cache.predict-ttl-seconds:600}") int predictTtlSeconds,
            @Value("${ai.cache.chat-ttl-seconds:300}") int chatTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.ocrTtlSeconds = ocrTtlSeconds;
        this.ragTtlSeconds = ragTtlSeconds;
        this.predictTtlSeconds = predictTtlSeconds;
        this.chatTtlSeconds = chatTtlSeconds;
    }

    // ===== OCR 缓存 =====

    public void cacheOcrResult(String imageHash, String result) {
        if (!enabled) return;
        redisTemplate.opsForValue().set(PREFIX_OCR + imageHash, result, ocrTtlSeconds, TimeUnit.SECONDS);
        logger.debug("OCR 结果已缓存: hash={}, ttl={}s", imageHash, ocrTtlSeconds);
    }

    public String getCachedOcrResult(String imageHash) {
        if (!enabled) return null;
        return redisTemplate.opsForValue().get(PREFIX_OCR + imageHash);
    }

    // ===== RAG 缓存 =====

    public void cacheRagResult(String cacheKey, String result) {
        if (!enabled) return;
        redisTemplate.opsForValue().set(PREFIX_RAG + cacheKey, result, ragTtlSeconds, TimeUnit.SECONDS);
        logger.debug("RAG 结果已缓存: key={}, ttl={}s", cacheKey, ragTtlSeconds);
    }

    public String getCachedRagResult(String cacheKey) {
        if (!enabled) return null;
        return redisTemplate.opsForValue().get(PREFIX_RAG + cacheKey);
    }

    // ===== Predict 缓存 =====

    public void cachePredictResult(String cacheKey, String result) {
        if (!enabled) return;
        redisTemplate.opsForValue().set(PREFIX_PREDICT + cacheKey, result, predictTtlSeconds, TimeUnit.SECONDS);
        logger.debug("Predict 结果已缓存: key={}, ttl={}s", cacheKey, predictTtlSeconds);
    }

    public String getCachedPredictResult(String cacheKey) {
        if (!enabled) return null;
        return redisTemplate.opsForValue().get(PREFIX_PREDICT + cacheKey);
    }

    // ===== Chat 缓存 =====

    public void cacheChatResult(String cacheKey, String result) {
        if (!enabled) return;
        redisTemplate.opsForValue().set(PREFIX_CHAT + cacheKey, result, chatTtlSeconds, TimeUnit.SECONDS);
        logger.debug("Chat 结果已缓存: key={}, ttl={}s", cacheKey, chatTtlSeconds);
    }

    public String getCachedChatResult(String cacheKey) {
        if (!enabled) return null;
        return redisTemplate.opsForValue().get(PREFIX_CHAT + cacheKey);
    }

    // ===== 缓存预热入口 =====

    public void warmup(String category, String key, String result) {
        switch (category) {
            case "ocr" -> cacheOcrResult(key, result);
            case "rag" -> cacheRagResult(key, result);
            case "predict" -> cachePredictResult(key, result);
            default -> logger.warn("未知缓存分类: {}", category);
        }
    }

    // ===== 缓存失效 =====

    public void evictByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("缓存失效: pattern={}, count={}", pattern, keys.size());
        }
    }

    // ===== 工具方法 =====

    /**
     * 生成 RAG 缓存 key: question + topK + strategy 的 MD5
     */
    public static String buildRagCacheKey(String question, int topK, String strategy) {
        return md5(question + "|" + topK + "|" + (strategy != null ? strategy : "default"));
    }

    /**
     * 生成 Predict 缓存 key: 规范化 JSON 的 MD5
     */
    public static String buildPredictCacheKey(String text) {
        return md5(text.trim().replaceAll("\\s+", " "));
    }

    /**
     * 生成 OCR 缓存 key: 图片内容 hash
     */
    public static String buildOcrCacheKey(byte[] imageBytes) {
        return md5(imageBytes);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(java.util.Arrays.hashCode(input));
        }
    }
}
