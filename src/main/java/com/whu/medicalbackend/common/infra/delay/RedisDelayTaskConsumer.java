package com.whu.medicalbackend.common.infra.delay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "infra.delay.redis", name = "enabled", havingValue = "true")
public class RedisDelayTaskConsumer {

    // 用 Lua 在 Redis 单线程内完成取出和删除，避免多实例 poll 时 range/remove 之间出现竞态窗口。
    private static final RedisScript<List> POP_READY_TASKS_SCRIPT = new DefaultRedisScript<>("""
            local items = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2])
            for _, item in ipairs(items) do
                redis.call('ZREM', KEYS[1], item)
            end
            return items
            """, List.class);

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
        List<String> readyTasks = popReadyTasks(now);

        if (readyTasks.isEmpty()) {
            return;
        }

        for (String raw : readyTasks) {
            handleRawTask(raw);
        }
    }

    private List<String> popReadyTasks(long now) {
        List<String> tasks = redisTemplate.execute(
                POP_READY_TASKS_SCRIPT,
                Collections.singletonList(RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY),
                String.valueOf(now),
                String.valueOf(batchSize)
        );
        return tasks == null ? List.of() : tasks;
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
