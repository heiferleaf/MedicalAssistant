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

    public void put(String cacheKey, RagResponse response) {
        if (!properties.isCacheEnabled() || response == null) {
            return;
        }
        try {
            boolean emptyAnswer = response.getAnswer() == null || response.getAnswer().trim().isEmpty();
            long ttlSeconds = emptyAnswer ? properties.getNullCacheTtlSeconds() : ttlWithJitter();
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

    private long ttlWithJitter() {
        long jitter = properties.getCacheTtlJitterSeconds() <= 0
                ? 0
                : ThreadLocalRandom.current().nextLong(properties.getCacheTtlJitterSeconds() + 1);
        return properties.getCacheTtlSeconds() + jitter;
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
