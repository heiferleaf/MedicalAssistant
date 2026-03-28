package com.whu.medicalbackend.agent.langchain4j.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具执行状态广播器
 * 通过 SSE 向客户端推送工具执行状态
 */
@Component
public class ToolExecutionBroadcaster implements ToolExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionBroadcaster.class);
    
    // 存储每个 sessionId 对应的 SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // 用于发送 SSE 事件的 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 注册 SseEmitter
     * @param sessionId 会话 ID
     * @param emitter SSE 发射器
     */
    public void registerEmitter(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
        logger.debug("注册 SSE 发射器：sessionId={}", sessionId);
    }
    
    /**
     * 注销 SseEmitter
     * @param sessionId 会话 ID
     */
    public void unregisterEmitter(String sessionId) {
        emitters.remove(sessionId);
        logger.debug("注销 SSE 发射器：sessionId={}", sessionId);
    }
    
    @Override
    public void onToolStart(String toolName, Map<String, Object> args, String description) {
        logger.info("工具开始执行：toolName={}, description={}", toolName, description);
        
        // 获取 sessionId（从当前线程上下文或参数中获取）
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            logger.warn("无法获取 sessionId，跳过工具状态推送");
            return;
        }
        
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                Map<String, Object> eventData = new LinkedHashMap<>();
                eventData.put("type", "tool_start");
                eventData.put("tool_name", toolName);
                eventData.put("description", description);
                eventData.put("status", "processing");
                eventData.put("args", args);
                
                emitter.send(SseEmitter.event()
                    .name("tool_status")
                    .data(eventData));
                
                logger.debug("推送工具开始状态：sessionId={}, toolName={}", sessionId, toolName);
            } catch (IOException e) {
                logger.error("推送工具开始状态失败：sessionId={}, toolName={}", sessionId, toolName, e);
                emitters.remove(sessionId);
            }
        } else {
            logger.debug("未找到对应的 SSE 发射器：sessionId={}", sessionId);
        }
    }
    
    @Override
    public void onToolSuccess(String toolName, Object result) {
        logger.info("工具执行成功：toolName={}", toolName);
        
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            return;
        }
        
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                Map<String, Object> eventData = new LinkedHashMap<>();
                eventData.put("type", "tool_complete");
                eventData.put("tool_name", toolName);
                eventData.put("status", "success");
                eventData.put("result", result);
                
                emitter.send(SseEmitter.event()
                    .name("tool_status")
                    .data(eventData));
                
                logger.debug("推送工具成功状态：sessionId={}, toolName={}", sessionId, toolName);
            } catch (IOException e) {
                logger.error("推送工具成功状态失败：sessionId={}, toolName={}", sessionId, toolName, e);
                emitters.remove(sessionId);
            }
        }
    }
    
    @Override
    public void onToolError(String toolName, Exception error) {
        logger.error("工具执行失败：toolName={}, error={}", toolName, error.getMessage());
        
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            return;
        }
        
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                Map<String, Object> eventData = new LinkedHashMap<>();
                eventData.put("type", "tool_complete");
                eventData.put("tool_name", toolName);
                eventData.put("status", "error");
                eventData.put("error", error.getMessage());
                
                emitter.send(SseEmitter.event()
                    .name("tool_status")
                    .data(eventData));
                
                logger.debug("推送工具错误状态：sessionId={}, toolName={}", sessionId, toolName);
            } catch (IOException e) {
                logger.error("推送工具错误状态失败：sessionId={}, toolName={}", sessionId, toolName, e);
                emitters.remove(sessionId);
            }
        }
    }
    
    /**
     * 获取当前 sessionId
     * 从 ThreadLocal 中获取，需要在调用前设置
     */
    private String getCurrentSessionId() {
        return ToolExecutionContext.getCurrentSessionId();
    }
    
    /**
     * 设置当前会话上下文
     * @param sessionId 会话 ID
     */
    public void setCurrentSession(String sessionId) {
        ToolExecutionContext.setCurrentSessionId(sessionId);
    }
    
    /**
     * 清除当前会话上下文
     */
    public void clearCurrentSession() {
        ToolExecutionContext.clearCurrentSessionId();
    }
}
