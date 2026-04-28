package com.whu.medicalbackend.common.infra.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MessageIdempotencyService {

    private static final String IN_PROGRESS_PREFIX = "infra:idempotency:running:";
    private static final String DONE_PREFIX = "infra:idempotency:done:";

    private final StringRedisTemplate redisTemplate;

    public MessageIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isDone(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(DONE_PREFIX + key));
    }

    public boolean tryStart(String key, Duration ttl) {
        if (isDone(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(IN_PROGRESS_PREFIX + key, "1", ttl));
    }

    public void markDone(String key, Duration ttl) {
        redisTemplate.opsForValue().set(DONE_PREFIX + key, "1", ttl);
        clearRunning(key);
    }

    public void clearRunning(String key) {
        redisTemplate.delete(IN_PROGRESS_PREFIX + key);
    }
}
