package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class RagCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RagCacheService.class);
    private static final String CACHE_PREFIX = "ai:rag:v1:";
    private static final String LOCK_PREFIX = "lock:ai:rag:";

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;

    public RagCacheService(StringRedisTemplate redisTemplate,
                           RedissonClient redissonClient,
                           ObjectMapper objectMapper,
                           RagProperties properties) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().replaceAll("\\s+", " ");
    }

    public String cacheKey(RagRequest request) {
        String raw = normalizeQuestion(request.getQuestion()).toLowerCase(Locale.ROOT)
                + "|" + request.getTopK()
                + "|" + safe(request.getStrategy())
                + "|" + safe(request.getKnowledgeBaseVersion());
        return CACHE_PREFIX + sha256(raw);
    }

    public RLock lock(String cacheKey) {
        return redissonClient.getLock(LOCK_PREFIX + cacheKey.substring(CACHE_PREFIX.length()));
    }

    public RagResponse get(String cacheKey) {
        if (!properties.isCacheEnabled()) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(cacheKey);
            if (raw == null) {
                return null;
            }
            RagResponse response = objectMapper.readValue(raw, RagResponse.class);
            response.setCacheHit(true);
            response.setProviderStatus("cache");
            return response;
        } catch (Exception e) {
            logger.warn("Read RAG cache failed, key={}", cacheKey, e);
            return null;
        }
    }

    public void put(String cacheKey, RagResponse response, String question) {
        if (!properties.isCacheEnabled() || response == null) {
            return;
        }
        try {
            boolean emptyAnswer = response.getAnswer() == null || response.getAnswer().trim().isEmpty();
            long ttlSeconds = emptyAnswer ? properties.getNullCacheTtlSeconds() : selectTtl(question);
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            logger.warn("Write RAG cache failed, key={}", cacheKey, e);
        }
    }

    public boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(properties.getCacheLockWaitSeconds(), properties.getCacheLockLeaseSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.warn("Acquire RAG cache lock failed", e);
            return false;
        }
    }

    public void unlock(RLock lock) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            logger.warn("Release RAG cache lock failed", e);
        }
    }

    public String questionHash(String question) {
        return sha256(normalizeQuestion(question)).substring(0, 12);
    }

    public boolean isCacheEnabled() {
        return properties.isCacheEnabled();
    }

    private static final long TTL_SHORT  = 600;   // 10min — 时效性强的问题
    private static final long TTL_LONG   = 86400;  // 24h  — 基础药理知识

    // 含这些词的问题时效性强，短缓存
    private static final List<String> TIME_SENSITIVE_KEYWORDS = List.of(
            "最新", "最近", "今天", "今日", "当前", "现在", "指南", "新冠", "covid",
            "疫情", "outbreak", "recall", "召回", "通知", "公告", "latest", "recent", "current"
    );

    private long ttlWithJitter() {
        long jitter = properties.getCacheTtlJitterSeconds() <= 0
                ? 0
                : ThreadLocalRandom.current().nextLong(properties.getCacheTtlJitterSeconds() + 1);
        return properties.getCacheTtlSeconds() + jitter;
    }

    private long selectTtl(String question) {
        if (question == null) return ttlWithJitter();
        String q = question.toLowerCase(Locale.ROOT);
        for (String kw : TIME_SENSITIVE_KEYWORDS) {
            if (q.contains(kw)) {
                logger.debug("RAG cache short TTL triggered by keyword '{}' in question", kw);
                return TTL_SHORT;
            }
        }
        // 纯基础药理问题给长 TTL；否则用配置默认值
        if (q.contains("副作用") || q.contains("不良反应") || q.contains("禁忌") || q.contains("药理")
                || q.contains("mechanism") || q.contains("pharmacology") || q.contains("side effect")) {
            return TTL_LONG;
        }
        return ttlWithJitter();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
