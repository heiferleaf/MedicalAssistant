package com.whu.medicalbackend.agent.core.task;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 异步任务模型
 * 支持 L1-L4 请求分级，用于异步任务调度和状态追踪
 */
public class AgentTask {

    public enum Grade {
        L1_QUICK_CACHE,   // 缓存命中，毫秒级返回
        L2_SIMPLE_LLM,    // 简单 LLM 调用，秒级返回
        L3_AGENT_MULTI,   // 多轮 Agent 调用，含工具执行
        L4_LONG_RUNNING   // 耗时操作（图片处理、复杂推理）
    }

    public enum Status {
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }

    private final String taskId;
    private final String sessionId;
    private final String userId;
    private final Grade grade;
    private final int timeoutMs;
    private final Map<String, Object> payload;
    private volatile Status status;
    private volatile Map<String, Object> result;
    private volatile String errorMessage;
    private final LocalDateTime createdAt;
    private volatile LocalDateTime completedAt;
    private final CompletableFuture<Map<String, Object>> future;

    public AgentTask(String taskId, String sessionId, String userId, Grade grade,
                     int timeoutMs, Map<String, Object> payload,
                     CompletableFuture<Map<String, Object>> future) {
        this.taskId = taskId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.grade = grade;
        this.timeoutMs = timeoutMs;
        this.payload = payload;
        this.status = Status.QUEUED;
        this.createdAt = LocalDateTime.now();
        this.future = future;
    }

    public String getTaskId() { return taskId; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public Grade getGrade() { return grade; }
    public int getTimeoutMs() { return timeoutMs; }
    public Map<String, Object> getPayload() { return payload; }
    public Status getStatus() { return status; }
    public Map<String, Object> getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public CompletableFuture<Map<String, Object>> getFuture() { return future; }

    public void setStatus(Status status) { this.status = status; }
    public void setResult(Map<String, Object> result) { this.result = result; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
