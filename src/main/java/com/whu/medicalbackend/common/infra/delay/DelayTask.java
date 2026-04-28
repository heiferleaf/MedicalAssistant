package com.whu.medicalbackend.common.infra.delay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DelayTask {

    private String taskId;
    private String taskType;
    private String bizId;
    private Instant executeAt;
    private int retryCount;
    private int maxRetry = 3;
    private Map<String, Object> payload = new HashMap<>();

    public DelayTask() {
    }

    public static DelayTask of(String taskType, String bizId, Instant executeAt) {
        DelayTask task = new DelayTask();
        task.setTaskType(taskType);
        task.setBizId(bizId);
        task.setExecuteAt(executeAt);
        task.ensureTaskId();
        return task;
    }

    public void ensureTaskId() {
        if (taskId == null || taskId.isBlank()) {
            taskId = UUID.randomUUID().toString();
        }
        if (payload == null) {
            payload = new HashMap<>();
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public Instant getExecuteAt() {
        return executeAt;
    }

    public void setExecuteAt(Instant executeAt) {
        this.executeAt = executeAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
