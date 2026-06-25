package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.core.cache.AiCacheManager;
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
    private final AiCacheManager aiCacheManager;

    public RagCacheService(StringRedisTemplate redisTemplate,
                           RedissonClient redissonClient,
                           ObjectMapper objectMapper,
                           RagProperties properties,
                           AiCacheManager aiCacheManager) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.aiCacheManager = aiCacheManager;
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
                // 尝试 AiCacheManager 二级缓存
                String cached = aiCacheManager.getCachedRagResult(cacheKey);
                if (cached != null) {
                    logger.debug("RAG cache hit in AiCacheManager, key={}", cacheKey);
                    RagResponse response = objectMapper.readValue(cached, RagResponse.class);
                    response.setCacheHit(true);
                    response.setProviderStatus("cache");
                    // 回填到主缓存
                    redisTemplate.opsForValue().set(cacheKey, cached,
                            Duration.ofSeconds(properties.getCacheTtlSeconds()));
                    return response;
                }
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
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttlSeconds));
            // 同步写入 AiCacheManager 二级缓存
            aiCacheManager.cacheRagResult(cacheKey, json);
        } catch (Exception e) {
            logger.warn("Write RAG cache failed, key={}", cacheKey, e);
        }
    }

    /**
     * 带自定义 TTL 的缓存写入（支持动态 TTL）
     */
    public void putWithTtl(String cacheKey, RagResponse response, String question, long ttlSeconds) {
        if (!properties.isCacheEnabled() || response == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            logger.warn("Write RAG cache failed, key={}", cacheKey, e);
        }
    }

    /**
     * 获取过期的缓存值（用于降级返回）
     * 注：Redis 本身不支持获取已过期的数据，此处返回 null
     * 实际降级需要在应用层维护"过期但保留"的缓存
     */
    public RagResponse getStale(String cacheKey) {
        // TODO: 可选实现：维护 TTL 更长的"stale" 版本缓存
        // 当前简化为返回 null，由调用方自行处理
        return null;
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
