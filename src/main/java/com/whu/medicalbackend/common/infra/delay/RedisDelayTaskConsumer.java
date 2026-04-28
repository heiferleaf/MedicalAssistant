package com.whu.medicalbackend.common.infra.delay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "infra.delay.redis", name = "enabled", havingValue = "true")
public class RedisDelayTaskConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final List<DelayTaskHandler> handlers;

    @Value("${infra.delay.redis.batch-size:100}")
    private int batchSize;

    public RedisDelayTaskConsumer(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            List<DelayTaskHandler> handlers
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelayString = "${infra.delay.redis.poll-interval-ms:1000}")
    public void pollReadyTasks() {
        long now = Instant.now().toEpochMilli();
        Set<String> readyTasks = redisTemplate.opsForZSet()
                .rangeByScore(RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY, 0, now, 0, batchSize);

        if (readyTasks == null || readyTasks.isEmpty()) {
            return;
        }

        for (String raw : readyTasks) {
            Long removed = redisTemplate.opsForZSet().remove(RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY, raw);
            if (removed == null || removed == 0) {
                continue;
            }
            handleRawTask(raw);
        }
    }

    private void handleRawTask(String raw) {
        DelayTask task;
        try {
            task = objectMapper.readValue(raw, DelayTask.class);
        } catch (JsonProcessingException e) {
            log.error("延迟任务反序列化失败，任务已丢弃: {}", raw, e);
            return;
        }

        Optional<DelayTaskHandler> handler = handlers.stream()
                .filter(item -> item.supports(task.getTaskType()))
                .findFirst();

        if (handler.isEmpty()) {
            log.warn("未找到延迟任务处理器: taskType={}, taskId={}", task.getTaskType(), task.getTaskId());
            return;
        }

        try {
            handler.get().handle(task);
        } catch (Exception ex) {
            retryOrDiscard(task, ex);
        }
    }

    private void retryOrDiscard(DelayTask task, Exception ex) {
        int nextRetry = task.getRetryCount() + 1;
        if (nextRetry > task.getMaxRetry()) {
            log.error("延迟任务重试耗尽: taskType={}, taskId={}, bizId={}",
                    task.getTaskType(), task.getTaskId(), task.getBizId(), ex);
            return;
        }

        task.setRetryCount(nextRetry);
        task.setExecuteAt(Instant.now().plus(backoff(nextRetry)));
        log.warn("延迟任务执行失败，准备重试: taskType={}, taskId={}, retry={}/{}",
                task.getTaskType(), task.getTaskId(), nextRetry, task.getMaxRetry(), ex);

        try {
            String raw = objectMapper.writeValueAsString(task);
            redisTemplate.opsForZSet().add(
                    RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY,
                    raw,
                    task.getExecuteAt().toEpochMilli()
            );
        } catch (JsonProcessingException jsonEx) {
            log.error("延迟任务重试序列化失败，任务已丢弃: taskId={}", task.getTaskId(), jsonEx);
        }
    }

    private Duration backoff(int retryCount) {
        long seconds = Math.min(60, (long) Math.pow(2, retryCount));
        return Duration.ofSeconds(seconds);
    }
}
