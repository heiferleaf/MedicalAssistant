package com.whu.medicalbackend.agent.core.task;

import com.whu.medicalbackend.agent.core.task.AgentTask.Grade;
import com.whu.medicalbackend.agent.core.task.AgentTask.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Agent 异步任务调度服务
 * 支持请求分级（L1-L4）、超时控制、任务状态追踪
 */
@Service
public class AgentTaskService {

    private static final Logger logger = LoggerFactory.getLogger(AgentTaskService.class);
    private static final int MAX_TASKS = 100;

    private final ConcurrentHashMap<String, AgentTask> tasks = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor aiExecutor;

    private final int L1timeoutMs;
    private final int L2timeoutMs;
    private final int L3timeoutMs;
    private final int L4timeoutMs;

    public AgentTaskService(
            @Qualifier("aiExecutor") ThreadPoolTaskExecutor aiExecutor,
            @Value("${agent.task.grading.L1-timeout-ms:2000}") int L1timeoutMs,
            @Value("${agent.task.grading.L2-timeout-ms:10000}") int L2timeoutMs,
            @Value("${agent.task.grading.L3-timeout-ms:30000}") int L3timeoutMs,
            @Value("${agent.task.grading.L4-timeout-ms:120000}") int L4timeoutMs) {
        this.aiExecutor = aiExecutor;
        this.L1timeoutMs = L1timeoutMs;
        this.L2timeoutMs = L2timeoutMs;
        this.L3timeoutMs = L3timeoutMs;
        this.L4timeoutMs = L4timeoutMs;
    }

    /**
     * 自动分级并提交任务
     */
    public AgentTask submit(String sessionId, String userId, Map<String, Object> payload,
                            Supplier<Map<String, Object>> taskFn) {
        Grade grade = gradeRequest(payload);
        return submitWithGrade(sessionId, userId, payload, grade, taskFn);
    }

    /**
     * 指定分级提交任务
     */
    public AgentTask submitWithGrade(String sessionId, String userId, Map<String, Object> payload,
                                     Grade grade, Supplier<Map<String, Object>> taskFn) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        int timeoutMs = getTimeoutMs(grade);

        // 超出容量时淘汰最旧任务
        if (tasks.size() >= MAX_TASKS) {
            tasks.entrySet().stream()
                    .min(Map.Entry.comparingByValue(
                            (a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())))
                    .ifPresent(entry -> {
                        tasks.remove(entry.getKey());
                        logger.info("任务队列已满，淘汰最旧任务: {}", entry.getKey());
                    });
        }

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        AgentTask task = new AgentTask(taskId, sessionId, userId, grade, timeoutMs, payload, future);
        tasks.put(taskId, task);

        // 异步执行
        CompletableFuture.runAsync(() -> {
            task.setStatus(Status.RUNNING);
            try {
                Map<String, Object> result = taskFn.get();
                task.setResult(result);
                task.setStatus(Status.SUCCESS);
                future.complete(result);
                logger.info("任务完成: taskId={}, grade={}", taskId, grade);
            } catch (Exception e) {
                logger.error("任务执行失败: taskId={}", taskId, e);
                task.setErrorMessage(e.getMessage());
                task.setStatus(Status.FAILED);
                future.completeExceptionally(e);
            } finally {
                task.setCompletedAt(LocalDateTime.now());
            }
        }, aiExecutor);

        // 超时控制
        if (timeoutMs > 0) {
            future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                   .exceptionally(ex -> {
                       if (task.getStatus() == Status.RUNNING || task.getStatus() == Status.QUEUED) {
                           task.setStatus(Status.TIMEOUT);
                           task.setErrorMessage("任务超时 (" + timeoutMs + "ms)");
                           task.setCompletedAt(LocalDateTime.now());
                           logger.warn("任务超时: taskId={}, timeoutMs={}", taskId, timeoutMs);
                       }
                       return null;
                   });
        }

        return task;
    }

    public AgentTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public Map<String, Object> getTaskResult(String taskId) {
        AgentTask task = tasks.get(taskId);
        return task != null ? task.getResult() : null;
    }

    public boolean cancelTask(String taskId) {
        AgentTask task = tasks.get(taskId);
        if (task != null && (task.getStatus() == Status.QUEUED || task.getStatus() == Status.RUNNING)) {
            task.setStatus(Status.CANCELLED);
            task.getFuture().cancel(true);
            return true;
        }
        return false;
    }

    /**
     * 根据 payload 自动分级请求
     */
    public Grade gradeRequest(Map<String, Object> payload) {
        if (payload == null) return Grade.L2_SIMPLE_LLM;

        String message = payload.getOrDefault("message", "").toString();
        boolean hasImage = message.contains("图片数据：") || message.contains("/9j/")
                || message.contains("[BASE64_IMAGE]") || message.contains("data:image/");
        boolean hasOcr = message.contains("OCR 识别结果：");

        if (hasOcr && message.length() < 200) {
            return Grade.L1_QUICK_CACHE;
        }
        if (hasImage || message.length() > 2000) {
            return Grade.L4_LONG_RUNNING;
        }
        if (message.length() > 500 || hasOcr) {
            return Grade.L3_AGENT_MULTI;
        }
        return Grade.L2_SIMPLE_LLM;
    }

    /**
     * 根据等级获取超时时间
     */
    private int getTimeoutMs(Grade grade) {
        return switch (grade) {
            case L1_QUICK_CACHE -> L1timeoutMs;
            case L2_SIMPLE_LLM -> L2timeoutMs;
            case L3_AGENT_MULTI -> L3timeoutMs;
            case L4_LONG_RUNNING -> L4timeoutMs;
        };
    }

    /**
     * 清理已完成的旧任务
     */
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - 300_000; // 5分钟前的任务
        tasks.values().removeIf(t ->
                (t.getStatus() == Status.SUCCESS || t.getStatus() == Status.FAILED
                 || t.getStatus() == Status.TIMEOUT || t.getStatus() == Status.CANCELLED)
                && t.getCompletedAt() != null
                && t.getCompletedAt().isBefore(LocalDateTime.now().minusMinutes(5)));
    }
}
