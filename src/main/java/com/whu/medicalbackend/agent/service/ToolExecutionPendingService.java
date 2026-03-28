package com.whu.medicalbackend.agent.service;

import com.whu.medicalbackend.agent.entity.ToolExecutionPending;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;
import java.util.Map;

/**
 * Tool 执行待确认 Service
 */
public interface ToolExecutionPendingService {
    
    /**
     * 保存待确认的 Tool 执行请求
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param request Tool 执行请求
     * @param aiMessage AI 原始回复
     * @return requestId
     */
    String savePendingRequest(Long userId, String sessionId, ToolExecutionRequest request, String aiMessage);
    
    /**
     * 获取待确认的请求
     * @param requestId 请求 ID
     * @return 待确认请求
     */
    ToolExecutionPending getPendingRequest(String requestId);
    
    /**
     * 获取用户的所有待确认请求
     * @param userId 用户 ID
     * @return 待确认请求列表
     */
    List<ToolExecutionPending> getUserPendingRequests(Long userId);
    
    /**
     * 批准并执行 Tool
     * @param requestId 请求 ID
     * @param editedData 用户编辑后的数据（可选）
     * @return 执行结果
     */
    Map<String, Object> approveAndExecute(String requestId, Map<String, Object> editedData);
    
    /**
     * 拒绝请求
     * @param requestId 请求 ID
     */
    void rejectRequest(String requestId);
    
    /**
     * 清理过期请求
     * @return 清理的数量
     */
    int cleanupExpired();
}
