package com.whu.medicalbackend.common.infra.delay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RedisDelayTaskPublisher implements DelayTaskPublisher {

    public static final String DELAY_TASK_ZSET_KEY = "infra:delay:tasks";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDelayTaskPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DelayTask task) {
        Assert.notNull(task, "delay task must not be null");
        Assert.hasText(task.getTaskType(), "delay task type must not be blank");
        Assert.notNull(task.getExecuteAt(), "delay task executeAt must not be null");

        task.ensureTaskId();
        try {
            String raw = objectMapper.writeValueAsString(task);
            redisTemplate.opsForZSet().add(DELAY_TASK_ZSET_KEY, raw, task.getExecuteAt().toEpochMilli());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize delay task failed", e);
        }
    }
}
