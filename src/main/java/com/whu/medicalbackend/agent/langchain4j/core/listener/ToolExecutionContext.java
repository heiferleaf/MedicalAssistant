package com.whu.medicalbackend.agent.langchain4j.core.listener;

/**
 * 工具执行上下文
 * 使用 ThreadLocal 存储当前会话信息，用于在线程内传递 sessionId
 */
public class ToolExecutionContext {
    
    // 使用 ThreadLocal 存储当前 sessionId
    private static final ThreadLocal<String> CURRENT_SESSION_ID = new ThreadLocal<>();
    
    /**
     * 获取当前 sessionId
     * @return sessionId
     */
    public static String getCurrentSessionId() {
        return CURRENT_SESSION_ID.get();
    }
    
    /**
     * 设置当前 sessionId
     * @param sessionId 会话 ID
     */
    public static void setCurrentSessionId(String sessionId) {
        CURRENT_SESSION_ID.set(sessionId);
    }
    
    /**
     * 清除当前 sessionId
     * 必须在请求结束时调用，防止内存泄漏
     */
    public static void clearCurrentSessionId() {
        CURRENT_SESSION_ID.remove();
    }
}
