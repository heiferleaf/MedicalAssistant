package com.whu.medicalbackend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Tool 执行待确认实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionPending {
    
    /**
     * Tool 执行请求 ID（唯一）
     */
    private String requestId;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * Tool 名称（如：createPlan, updatePlan, deletePlan）
     */
    private String toolName;
    
    /**
     * Tool 参数（JSON 格式）
     */
    private String toolArguments;
    
    /**
     * AI 原始回复内容
     */
    private String originalAiMessage;
    
    /**
     * 状态：PENDING, APPROVED, REJECTED, EXECUTED, EXPIRED
     */
    private String status;
    
    /**
     * 用户编辑后的数据（JSON 格式，用户修改后保存）
     */
    private String editedData;
    
    /**
     * 执行时间
     */
    private LocalDateTime executedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 过期时间（默认 30 分钟）
     */
    private LocalDateTime expiresAt;
}
