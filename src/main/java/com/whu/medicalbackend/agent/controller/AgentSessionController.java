package com.whu.medicalbackend.agent.controller;

import com.whu.medicalbackend.agent.core.memory.AgentMemoryRepository;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.common.response.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/agent/sessions")
public class AgentSessionController {

    @Autowired
    private AgentMemoryRepository memoryRepository;

    /**
     * 获取用户的所有会话列表
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getSessions(@RequestParam String userId) {
        try {
            List<Map<String, Object>> sessions = memoryRepository.getUserSessions(userId);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> session : sessions) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sessionId", session.get("session_id"));
                item.put("userId", session.get("user_id"));
                item.put("createdAt", session.get("created_at"));
                item.put("updatedAt", session.get("updated_at"));
                item.put("summary", session.get("summary_text"));
                
                // 获取最近的消息预览
                List<Map<String, Object>> recentMessages = memoryRepository.getRecentMessages(
                    (String) session.get("session_id"), 1
                );
                if (!recentMessages.isEmpty()) {
                    item.put("lastMessage", recentMessages.get(0).get("content"));
                } else {
                    item.put("lastMessage", "新会话");
                }
                
                result.add(item);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "获取会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            String userId = userIdObj != null ? String.valueOf(userIdObj) : null;
            String sessionId = UUID.randomUUID().toString().replace("-", "");
            
            if (userId == null || userId.isBlank()) {
                return Result.error(ResultCode.BUSINESS_ERROR, "userId 不能为空");
            }
            
            memoryRepository.touchSession(sessionId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("userId", userId);
            result.put("createdAt", new Date());
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        try {
            memoryRepository.deleteSession(sessionId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "删除会话失败：" + e.getMessage());
        }
    }

    /**
     * 更新会话信息（PUT 方法）
     */
    @PutMapping("/{sessionId}")
    public Result<Map<String, Object>> updateSessionPut(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> payload) {
        return updateSessionInternal(sessionId, payload);
    }

    /**
     * 更新会话信息（POST 方法）
     */
    @PostMapping("/{sessionId}")
    public Result<Map<String, Object>> updateSessionPost(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> payload) {
        return updateSessionInternal(sessionId, payload);
    }

    /**
     * 更新会话信息内部方法
     */
    private Result<Map<String, Object>> updateSessionInternal(
            String sessionId,
            Map<String, Object> payload) {
        try {
            String summary = (String) payload.get("summary");
            
            if (summary != null && !summary.isBlank()) {
                memoryRepository.updateSessionSummary(sessionId, summary);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("summary", summary);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "更新会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话的所有消息
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<Map<String, Object>>> getMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Map<String, Object>> messages = memoryRepository.getRecentMessages(sessionId, limit);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", msg.get("role"));
                item.put("content", msg.get("content"));
                item.put("actionType", msg.get("action_type"));
                item.put("actionData", msg.get("action_data"));
                item.put("createdAt", msg.get("created_at"));
                result.add(item);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "获取消息失败：" + e.getMessage());
        }
    }

    /**
     * 清空会话消息（保留会话）
     */
    @DeleteMapping("/{sessionId}/messages")
    public Result<Void> clearMessages(@PathVariable String sessionId) {
        try {
            memoryRepository.deleteSessionMessages(sessionId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "清空消息失败：" + e.getMessage());
        }
    }
}
