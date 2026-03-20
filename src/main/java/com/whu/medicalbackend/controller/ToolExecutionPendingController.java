package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.service.ToolExecutionPendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 执行待确认 Controller
 * 提供 Human-in-the-loop 的 API 接口
 */
@RestController
@RequestMapping("/api/agent/tool-execution")
@CrossOrigin(origins = "*")
public class ToolExecutionPendingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionPendingController.class);
    
    @Autowired
    private ToolExecutionPendingService toolExecutionPendingService;
    
    /**
     * 获取用户的待确认请求列表
     * @param userId 用户 ID
     * @return 待确认请求列表
     */
    @GetMapping("/pending")
    public Map<String, Object> getPendingRequests(@RequestParam Long userId) {
        logger.info("获取待确认请求列表，userId: {}", userId);
        
        try {
            List<?> pendingRequests = toolExecutionPendingService.getUserPendingRequests(userId);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", pendingRequests);
            result.put("count", pendingRequests != null ? pendingRequests.size() : 0);
            
            return result;
        } catch (Exception e) {
            logger.error("获取待确认请求失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 批准并执行待确认的 Tool
     * @param userId 用户 ID
     * @param requestId 请求 ID
     * @param editedData 用户编辑后的数据（可选）
     * @return 执行结果
     */
    @PostMapping("/approve")
    public Map<String, Object> approveAndExecute(
            @RequestParam Long userId,
            @RequestParam String requestId,
            @RequestBody(required = false) Map<String, Object> editedData) {
        
        logger.info("批准并执行 Tool，userId: {}, requestId: {}, editedData: {}", userId, requestId, editedData);
        
        try {
            Map<String, Object> result = toolExecutionPendingService.approveAndExecute(requestId, editedData);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", (Boolean) result.getOrDefault("success", false));
            response.put("message", result.get("message"));
            response.put("data", result);
            
            return response;
        } catch (Exception e) {
            logger.error("批准执行 Tool 失败", e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "执行失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 拒绝待确认的请求
     * @param userId 用户 ID
     * @param requestId 请求 ID
     * @return 操作结果
     */
    @PostMapping("/reject")
    public Map<String, Object> rejectRequest(
            @RequestParam Long userId,
            @RequestParam String requestId) {
        
        logger.info("拒绝待确认请求，userId: {}, requestId: {}", userId, requestId);
        
        try {
            toolExecutionPendingService.rejectRequest(requestId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "已拒绝该请求");
            
            return response;
        } catch (Exception e) {
            logger.error("拒绝请求失败", e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "拒绝失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 清理过期的待确认请求
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupExpired() {
        logger.info("清理过期待确认请求");
        
        try {
            int count = toolExecutionPendingService.cleanupExpired();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "清理成功");
            response.put("count", count);
            
            return response;
        } catch (Exception e) {
            logger.error("清理过期请求失败", e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "清理失败：" + e.getMessage());
            return response;
        }
    }
}
