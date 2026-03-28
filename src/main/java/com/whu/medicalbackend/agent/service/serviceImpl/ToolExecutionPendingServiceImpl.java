package com.whu.medicalbackend.agent.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanCreateTool;
import com.whu.medicalbackend.agent.entity.ToolExecutionPending;
import com.whu.medicalbackend.agent.mapper.ToolExecutionPendingMapper;
import com.whu.medicalbackend.agent.service.ToolExecutionPendingService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Tool 执行待确认 Service 实现
 */
@Service
public class ToolExecutionPendingServiceImpl implements ToolExecutionPendingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionPendingServiceImpl.class);
    
    @Autowired
    private ToolExecutionPendingMapper toolExecutionPendingMapper;
    
    @Autowired
    private PlanCreateTool planCreateTool;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 需要批准的 tool 名称列表
    private static final Set<String> REQUIRES_APPROVAL_TOOLS = new HashSet<>(Arrays.asList(
        "createPlan", "updatePlan", "deletePlan"
    ));
    
    @Override
    public String savePendingRequest(Long userId, String sessionId, ToolExecutionRequest request, String aiMessage) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        
        ToolExecutionPending pending = new ToolExecutionPending();
        pending.setRequestId(requestId);
        pending.setUserId(userId);
        pending.setSessionId(sessionId);
        pending.setToolName(request.name());
        pending.setToolArguments(request.arguments());
        pending.setOriginalAiMessage(aiMessage);
        pending.setStatus("PENDING");
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // 30 分钟过期
        
        toolExecutionPendingMapper.insert(pending);
        logger.info("保存待确认 Tool 请求：requestId={}, toolName={}, userId={}", 
            requestId, request.name(), userId);
        
        return requestId;
    }
    
    @Override
    public ToolExecutionPending getPendingRequest(String requestId) {
        ToolExecutionPending pending = toolExecutionPendingMapper.findByRequestId(requestId);
        
        if (pending == null) {
            logger.warn("未找到待确认请求：requestId={}", requestId);
            return null;
        }
        
        // 检查是否过期
        if ("PENDING".equals(pending.getStatus()) && 
            pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("待确认请求已过期：requestId={}", requestId);
            toolExecutionPendingMapper.updateStatus(requestId, "EXPIRED", null, null);
            return null;
        }
        
        return pending;
    }
    
    @Override
    public List<ToolExecutionPending> getUserPendingRequests(Long userId) {
        return toolExecutionPendingMapper.findPendingByUserId(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveAndExecute(String requestId, Map<String, Object> editedData) {
        ToolExecutionPending pending = getPendingRequest(requestId);
        
        if (pending == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "请求不存在或已过期");
            return error;
        }
        
        try {
            logger.info("执行待确认 Tool：requestId={}, toolName={}", requestId, pending.getToolName());
            
            // 根据 tool 名称执行对应的 tool
            Map<String, Object> result;
            
            if ("createPlan".equals(pending.getToolName())) {
                // 使用用户编辑后的数据（如果有）
                Map<String, Object> arguments = editedData != null ? editedData : 
                    parseArguments(pending.getToolArguments());
                
                result = executeCreatePlan(pending.getUserId().toString(), arguments);
            } else if ("updatePlan".equals(pending.getToolName())) {
                // TODO: 实现 updatePlan
                result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "updatePlan 尚未实现");
            } else if ("deletePlan".equals(pending.getToolName())) {
                // TODO: 实现 deletePlan
                result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "deletePlan 尚未实现");
            } else {
                result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "不支持的 tool: " + pending.getToolName());
            }
            
            // 更新状态
            if ((Boolean) result.getOrDefault("success", false)) {
                String editedDataJson = editedData != null ? objectMapper.writeValueAsString(editedData) : null;
                toolExecutionPendingMapper.updateStatus(requestId, "EXECUTED", editedDataJson, LocalDateTime.now());
                logger.info("Tool 执行成功：requestId={}", requestId);
            } else {
                toolExecutionPendingMapper.updateStatus(requestId, "REJECTED", null, null);
                logger.error("Tool 执行失败：requestId={}, message={}", requestId, result.get("message"));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("执行待确认 Tool 失败", e);
            toolExecutionPendingMapper.updateStatus(requestId, "REJECTED", null, null);
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "执行失败：" + e.getMessage());
            return error;
        }
    }
    
    @Override
    public void rejectRequest(String requestId) {
        ToolExecutionPending pending = toolExecutionPendingMapper.findByRequestId(requestId);
        if (pending != null && "PENDING".equals(pending.getStatus())) {
            toolExecutionPendingMapper.updateStatus(requestId, "REJECTED", null, null);
            logger.info("拒绝 Tool 请求：requestId={}", requestId);
        }
    }
    
    @Override
    public int cleanupExpired() {
        int count = toolExecutionPendingMapper.deleteExpired(LocalDateTime.now());
        logger.info("清理过期待确认请求：count={}", count);
        return count;
    }
    
    /**
     * 执行 createPlan tool
     */
    private Map<String, Object> executeCreatePlan(String userId, Map<String, Object> arguments) {
        try {
            String medicineName = (String) arguments.get("medicineName");
            String dosage = (String) arguments.get("dosage");
            List<String> timePoints = (List<String>) arguments.get("timePoints");
            String startDate = (String) arguments.get("startDate");
            String endDate = (String) arguments.get("endDate");
            String remark = (String) arguments.get("remark");
            
            return planCreateTool.createPlan(
                userId, medicineName, dosage, timePoints, startDate, endDate, remark
            );
        } catch (Exception e) {
            logger.error("执行 createPlan 失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "执行 createPlan 失败：" + e.getMessage());
            return error;
        }
    }
    
    /**
     * 解析 JSON 参数
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (JsonProcessingException e) {
            logger.error("解析 Tool 参数失败", e);
            return new LinkedHashMap<>();
        }
    }
    
    /**
     * 检查 tool 是否需要批准
     */
    public static boolean requiresApproval(String toolName) {
        return REQUIRES_APPROVAL_TOOLS.contains(toolName);
    }
}
