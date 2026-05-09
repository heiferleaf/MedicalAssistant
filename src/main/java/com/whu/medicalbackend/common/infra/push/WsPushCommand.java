package com.whu.medicalbackend.common.infra.push;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WsPushCommand {

    private String commandId;
    private Long userId;
    private Long groupId;
    private Map<String, Object> payload = new HashMap<>();
    private Instant createdAt;

    public WsPushCommand() {
    }

    public static WsPushCommand user(Long userId, Long groupId, Map<String, Object> payload) {
        WsPushCommand command = new WsPushCommand();
        command.setUserId(userId);
        command.setGroupId(groupId);
        command.setPayload(payload);
        command.ensureMetadata();
        return command;
    }

    public void ensureMetadata() {
        if (commandId == null || commandId.isBlank()) {
            commandId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (payload == null) {
            payload = new HashMap<>();
        }
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
