package com.whu.medicalbackend.agent.langchain4j.core.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 工具执行包装器
 * 用于包装工具的实际执行逻辑，自动调用监听器
 */
public class ToolExecutionWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionWrapper.class);
    
    private final ToolExecutionBroadcaster broadcaster;
    
    public ToolExecutionWrapper(ToolExecutionBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }
    
    /**
     * 执行工具并通知监听器
     * @param toolName 工具名称
     * @param args 工具参数
     * @param description 工具描述
     * @param execution 工具执行逻辑
     * @return 执行结果
     */
    public <T> T execute(String toolName, Map<String, Object> args, String description, Supplier<T> execution) {
        try {
            // 通知开始
            broadcaster.onToolStart(toolName, args, description);
            
            // 执行工具
            T result = execution.get();
            
            // 通知成功
            broadcaster.onToolSuccess(toolName, result);
            
            return result;
            
        } catch (Exception e) {
            // 通知失败
            broadcaster.onToolError(toolName, e);
            throw e;
        }
    }
}
