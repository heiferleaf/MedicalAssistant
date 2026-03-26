package com.whu.medicalbackend.agent.langchain4j.core.listener;

import java.util.Map;

/**
 * 工具执行监听器接口
 * 用于监听工具执行的各个阶段，实现实时状态推送
 */
public interface ToolExecutionListener {
    
    /**
     * 工具开始执行时调用
     * @param toolName 工具名称
     * @param args 工具参数
     * @param description 工具描述（用于前端显示）
     */
    void onToolStart(String toolName, Map<String, Object> args, String description);
    
    /**
     * 工具执行成功时调用
     * @param toolName 工具名称
     * @param result 执行结果
     */
    void onToolSuccess(String toolName, Object result);
    
    /**
     * 工具执行失败时调用
     * @param toolName 工具名称
     * @param error 错误信息
     */
    void onToolError(String toolName, Exception error);
}
