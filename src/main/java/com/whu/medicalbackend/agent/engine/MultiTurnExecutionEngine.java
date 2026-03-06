package com.whu.medicalbackend.agent.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.whu.medicalbackend.agent.llm.AgentPromptTemplate;
import com.whu.medicalbackend.agent.llm.DashScopeApiService;
import com.whu.medicalbackend.agent.llm.FunctionCallResult;
import com.whu.medicalbackend.agent.memory.AgentMemoryRepository;
import com.whu.medicalbackend.agent.tools.PlanTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮 Function Call 执行引擎（完整版）
 * 
 * 设计模式：
 * 1. 责任链模式：多轮执行形成处理链
 * 2. 策略模式：支持不同的执行策略
 * 3. 门面模式：封装复杂的 LLM 交互逻辑
 * 4. 注册表模式：工具注册中心
 * 
 * 扩展性：
 * - 新增工具只需注册到 toolHandlers，无需修改引擎核心逻辑
 * - 支持自定义执行策略（最大轮数、超时控制、重试机制等）
 * - 支持多种 LLM 提供商（通过 LLMClient 抽象）
 */
@Slf4j
@Component
public class MultiTurnExecutionEngine {
    
    private final DashScopeApiService llmService;
    private final AgentMemoryRepository memoryRepository;
    private final PlanTools planTools;
    private final ObjectMapper objectMapper;
    
    /**
     * 工具处理器函数式接口
     */
    @FunctionalInterface
    public interface ToolHandler {
        Map<String, Object> execute(
            String userId,
            String sessionId,
            Map<String, Object> params,
            Map<String, Object> context
        );
    }
    
    /**
     * 工具注册表
     */
    private final Map<String, ToolHandler> toolHandlers = new ConcurrentHashMap<>();
    
    /**
     * 默认配置
     */
    private int maxTurns = 5;
    private long timeoutMs = 30000; // 30 秒
    private boolean enableRetry = true;
    private int maxRetries = 2;
    
    public MultiTurnExecutionEngine(
            DashScopeApiService llmService,
            AgentMemoryRepository memoryRepository,
            PlanTools planTools,
            ObjectMapper objectMapper
    ) {
        this.llmService = llmService;
        this.memoryRepository = memoryRepository;
        this.planTools = planTools;
        this.objectMapper = objectMapper;
        
        // 注册默认工具处理器
        registerDefaultTools();
    }
    
    /**
     * 注册默认工具处理器
     */
    private void registerDefaultTools() {
        // Plan 相关工具
        toolHandlers.put("spring_plan_query", (userId, sessionId, params, context) -> 
            planTools.executeTool("spring_plan_query", params, userId, sessionId, 
                bool(context.get("with_trace")), bool(context.get("with_timing")), 
                (Map<String, Object>) context.get("trace"))
        );
        
        toolHandlers.put("spring_plan_create", (userId, sessionId, params, context) -> 
            planTools.executeTool("spring_plan_create", params, userId, sessionId,
                bool(context.get("with_trace")), bool(context.get("with_timing")),
                (Map<String, Object>) context.get("trace"))
        );
        
        toolHandlers.put("spring_plan_update", (userId, sessionId, params, context) -> 
            planTools.executeTool("spring_plan_update", params, userId, sessionId,
                bool(context.get("with_trace")), bool(context.get("with_timing")),
                (Map<String, Object>) context.get("trace"))
        );
        
        toolHandlers.put("spring_plan_delete", (userId, sessionId, params, context) -> 
            planTools.executeTool("spring_plan_delete", params, userId, sessionId,
                bool(context.get("with_trace")), bool(context.get("with_timing")),
                (Map<String, Object>) context.get("trace"))
        );
        
        log.info("已注册 {} 个默认工具处理器", toolHandlers.size());
    }
    
    /**
     * 注册自定义工具处理器
     * 
     * @param toolName 工具名称
     * @param handler 工具处理器
     */
    public void registerTool(String toolName, ToolHandler handler) {
        toolHandlers.put(toolName, handler);
        log.info("已注册工具：{}", toolName);
    }
    
    /**
     * 执行多轮 Function Call
     * 
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param userMessage 用户消息
     * @param tools 工具定义（ArrayNode）
     * @param withTrace 是否返回追踪信息
     * @param withTiming 是否返回耗时信息
     * @return 最终响应
     */
    public Map<String, Object> execute(
            String userId,
            String sessionId,
            String userMessage,
            Object tools,
            boolean withTrace,
            boolean withTiming
    ) {
        long startTime = System.currentTimeMillis();
        long deadline = startTime + timeoutMs;
        
        // 初始化执行上下文
        ExecutionContext context = new ExecutionContext(userId, sessionId, userMessage, withTrace, withTiming, startTime);
        
        log.info("=== 开始多轮执行 === 用户：{} 会话：{} 消息：{}", userId, sessionId, userMessage);
        
        try {
            // 多轮执行循环
            for (int turn = 0; turn < maxTurns; turn++) {
                log.info("=== 第 {} 轮执行 (剩余时间：{}ms) ===", turn + 1, (deadline - System.currentTimeMillis()));
                
                // 检查超时
                if (System.currentTimeMillis() > deadline) {
                    log.warn("执行超时 ({}ms)", timeoutMs);
                    return buildTimeoutResponse(context);
                }
                
                // 1. 调用 LLM
                log.info("=== 第 {} 轮执行开始 ===", turn + 1);
                FunctionCallResult llmResult = callLLM(context, tools);
                
                if (llmResult == null) {
                    log.error("LLM 返回为空");
                    return buildErrorResponse("LLM 调用失败", context);
                }
                
                log.info("LLM 响应：{}", llmResult);
                log.info("LLM 是否调用工具：{}", llmResult.isFunctionCall());
                
                if (withTrace) {
                    context.getTrace().put("llm_result_turn_" + turn, llmResult.toString());
                }
                
                // 2. 判断是否需要工具调用
                if (!llmResult.isFunctionCall()) {
                    log.info("LLM 直接回复，无需工具调用");
                    return buildDirectResponse(llmResult.getContent(), context);
                }
                
                // 3. 执行工具
                String toolName = llmResult.getName();
                Map<String, Object> toolArgs = llmResult.getArguments();
                
                log.info("调用工具：{}，参数：{}", toolName, toolArgs);
                
                Map<String, Object> toolResult = executeToolWithRetry(
                    toolName, toolArgs, userId, sessionId, context
                );
                
                if (toolResult == null) {
                    log.error("工具执行失败：{}", toolName);
                    return buildErrorResponse("工具执行失败：" + toolName, context);
                }
                
                // 4. 记录工具调用
                context.addToolCall(toolName, toolArgs, toolResult);
                
                // 5. 更新对话历史
                String assistantMessage = (String) toolResult.get("assistant_message");
                if (assistantMessage != null) {
                    memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                }
                
                // 6. 检查是否需要继续
                Boolean needContinue = (Boolean) toolResult.get("need_continue");
                
                if (needContinue == null || !needContinue) {
                    log.info("任务完成，不需要继续执行");
                    return buildFinalResponse(assistantMessage, context);
                }
                
                log.info("需要继续执行下一轮");
                
                // 7. 添加工具结果到对话历史（供下一轮 LLM 使用）
                String toolResultJson = objectMapper.writeValueAsString(toolResult);
                context.addHistoryMessage("assistant", assistantMessage);
                context.addHistoryMessage("tool", "工具 " + toolName + " 执行结果：" + toolResultJson);
                
                // 同时添加到 memoryRepository（持久化）
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                memoryRepository.appendMessage(sessionId, userId, "tool", 
                    "工具 " + toolName + " 执行结果：" + toolResultJson);
                
                log.info("工具调用结果已保存到数据库");
            }
            
            // 超过最大轮数
            log.warn("超过最大执行轮数 {}，强制结束", maxTurns);
            return buildMaxTurnsResponse(context);
            
        } catch (Exception e) {
            log.error("多轮执行失败", e);
            return buildErrorResponse("执行失败：" + e.getMessage(), context);
        }
    }
    
    /**
     * 调用 LLM
     */
    private FunctionCallResult callLLM(ExecutionContext context, Object tools) {
        try {
            // 从数据库中读取最近的对话历史（包括用户消息、assistant 消息、tool 结果）
            List<Map<String, Object>> recentMessages = memoryRepository.getRecentMessages(context.getSessionId(), 20);
            
            // 构建对话历史（只保留 role 和 content）
            List<Map<String, String>> history = new ArrayList<>();
            for (Map<String, Object> msg : recentMessages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                
                // OpenAI 兼容 API 要求：role 为 "tool" 的消息必须跟在 "assistant" 且包含 "tool_calls" 的消息之后
                // 但我们的实现中，assistant 消息可能是文本，也可能是 tool 调用
                // 所以把 "tool" 角色的消息转换为 "user" 角色的消息
                if ("tool".equals(role)) {
                    role = "user";
                    content = "工具执行结果：" + content;
                }
                
                Map<String, String> historyMsg = new HashMap<>();
                historyMsg.put("role", role);
                historyMsg.put("content", content);
                history.add(historyMsg);
            }
            
            // 如果对话历史为空，添加用户消息
            if (history.isEmpty()) {
                log.info("对话历史为空，添加用户消息");
                history.add(Map.of("role", "user", "content", context.getUserMessage()));
            } else {
                log.info("对话历史包含 {} 条消息", history.size());
            }
            
            // 类型转换为 ArrayNode
            ArrayNode toolsArray = (tools instanceof ArrayNode) ? (ArrayNode) tools : null;
            
            // 调用 LLM
            log.info("调用 LLM，用户消息：{}", context.getUserMessage());
            FunctionCallResult result = llmService.chatWithFunction(
                AgentPromptTemplate.SYSTEM_PROMPT,
                context.getUserMessage(),
                history,
                toolsArray
            );
            
            log.info("LLM 调用结果：{}", result);
            
            // 保存 LLM 的 tool 调用决策到数据库
            if (result != null && result.isFunctionCall()) {
                String toolCallJson = objectMapper.writeValueAsString(Map.of(
                    "tool_name", result.getName(),
                    "tool_arguments", result.getArguments()
                ));
                memoryRepository.appendMessage(context.getSessionId(), context.getUserId(), "assistant", 
                    "LLM 决定调用工具：" + toolCallJson);
            }
            
            return result;
        } catch (Exception e) {
            log.error("调用 LLM 失败", e);
            return null;
        }
    }
    
    /**
     * 执行工具（带重试）
     */
    private Map<String, Object> executeToolWithRetry(
            String toolName,
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            ExecutionContext context
    ) {
        int attempts = enableRetry ? maxRetries : 1;
        
        for (int i = 0; i < attempts; i++) {
            try {
                Map<String, Object> result = executeTool(toolName, toolArgs, userId, sessionId, context);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("工具 {} 执行失败 (尝试 {}/{})", toolName, i + 1, attempts, e);
                if (i == attempts - 1) {
                    log.error("工具 {} 执行失败，已达最大重试次数", toolName, e);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 执行工具
     */
    private Map<String, Object> executeTool(
            String toolName,
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            ExecutionContext context
    ) {
        // 查找工具处理器
        ToolHandler handler = toolHandlers.get(toolName);
        
        if (handler == null) {
            log.error("未知工具：{}", toolName);
            return Map.of(
                "success", false,
                "assistant_message", "未知工具：" + toolName
            );
        }
        
        try {
            // 执行工具
            return handler.execute(userId, sessionId, toolArgs, context.getContextData());
        } catch (Exception e) {
            log.error("工具 {} 执行异常", toolName, e);
            return Map.of(
                "success", false,
                "assistant_message", "工具执行失败：" + e.getMessage()
            );
        }
    }
    
    /**
     * 构建直接回复响应
     */
    private Map<String, Object> buildDirectResponse(String message, ExecutionContext context) {
        memoryRepository.appendMessage(context.getSessionId(), context.getUserId(), "assistant", message);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("assistant_message", message);
        response.put("need_confirm", false);
        response.put("actions", List.of());
        response.put("control", "direct_answer");
        
        enrichResponse(response, context);
        return response;
    }
    
    /**
     * 构建最终响应
     */
    private Map<String, Object> buildFinalResponse(String message, ExecutionContext context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("assistant_message", message != null ? message : "操作完成");
        response.put("need_confirm", false);
        response.put("actions", List.of());
        response.put("control", "multi_turn_complete");
        
        // 合并最后一个工具结果的数据
        if (!context.getToolResults().isEmpty()) {
            Map<String, Object> lastResult = context.getToolResults().get(context.getToolResults().size() - 1);
            lastResult.forEach((key, value) -> {
                if (!key.equals("assistant_message") && 
                    !key.equals("need_confirm") && 
                    !key.equals("actions") &&
                    !key.equals("need_continue")) {
                    response.put(key, value);
                }
            });
        }
        
        enrichResponse(response, context);
        return response;
    }
    
    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(String message, ExecutionContext context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("assistant_message", message);
        response.put("need_confirm", false);
        response.put("actions", List.of());
        response.put("control", "error");
        
        enrichResponse(response, context);
        return response;
    }
    
    /**
     * 构建超时响应
     */
    private Map<String, Object> buildTimeoutResponse(ExecutionContext context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("assistant_message", "抱歉，任务执行超时，请稍后重试");
        response.put("need_confirm", false);
        response.put("actions", List.of());
        response.put("control", "timeout");
        
        enrichResponse(response, context);
        return response;
    }
    
    /**
     * 构建最大轮数响应
     */
    private Map<String, Object> buildMaxTurnsResponse(ExecutionContext context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("assistant_message", "任务已部分完成，如需继续请再次询问");
        response.put("need_confirm", false);
        response.put("actions", List.of());
        response.put("control", "max_turns_reached");
        
        enrichResponse(response, context);
        return response;
    }
    
    /**
     * 丰富响应数据（trace、timing 等）
     */
    private void enrichResponse(Map<String, Object> response, ExecutionContext context) {
        // Always include tool_calls if any were called
        if (!context.getToolCalls().isEmpty()) {
            List<Map<String, Object>> detailedToolCalls = new ArrayList<>();
            for (int i = 0; i < context.getToolCalls().size(); i++) {
                String toolName = context.getToolCalls().get(i);
                Map<String, Object> toolResult = context.getToolResults().get(i);
                
                Map<String, Object> detailedCall = new LinkedHashMap<>();
                detailedCall.put("turn", i + 1);
                detailedCall.put("tool_name", toolName);
                detailedCall.put("result", toolResult);
                detailedToolCalls.add(detailedCall);
            }
            response.put("tool_calls", detailedToolCalls);
        }
        
        if (context.isWithTrace()) {
            context.getTrace().put("tools_called", context.getToolCalls());
            context.getTrace().put("execution_turns", context.getToolCalls().size());
            response.put("trace", context.getTrace());
        }
        
        if (context.isWithTiming()) {
            Map<String, Long> timings = new HashMap<>();
            timings.put("total_ms", System.currentTimeMillis() - context.getStartTime());
            response.put("timings", timings);
        }
    }
    
    /**
     * 辅助方法：转换为 boolean
     */
    private boolean bool(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        if (obj instanceof Number) return ((Number) obj).intValue() != 0;
        return false;
    }
    
    /**
     * 执行上下文封装
     */
    private static class ExecutionContext {
        private final String userId;
        private final String sessionId;
        private final String userMessage;
        private final boolean withTrace;
        private final boolean withTiming;
        private final long startTime;
        private final Map<String, Object> trace;
        private final Map<String, Object> contextData;
        private final List<String> toolCalls;
        private final List<Map<String, Object>> toolResults;
        private final List<Map<String, String>> conversationHistory;
        
        public ExecutionContext(String userId, String sessionId, String userMessage, 
                               boolean withTrace, boolean withTiming, long startTime) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.userMessage = userMessage;
            this.withTrace = withTrace;
            this.withTiming = withTiming;
            this.startTime = startTime;
            this.trace = new LinkedHashMap<>();
            this.contextData = new HashMap<>();
            this.toolCalls = new ArrayList<>();
            this.toolResults = new ArrayList<>();
            this.conversationHistory = new ArrayList<>();
            
            // 添加用户消息到对话历史
            this.conversationHistory.add(Map.of("role", "user", "content", userMessage));
            
            // 初始化 trace
            this.trace.put("agent_version", "multi_turn_v1_complete");
            this.trace.put("engine", "MultiTurnExecutionEngine");
            
            // 初始化 contextData
            this.contextData.put("with_trace", withTrace);
            this.contextData.put("with_timing", withTiming);
            this.contextData.put("trace", this.trace);
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public String getUserMessage() { return userMessage; }
        public boolean isWithTrace() { return withTrace; }
        public boolean isWithTiming() { return withTiming; }
        public long getStartTime() { return startTime; }
        public Map<String, Object> getTrace() { return trace; }
        public Map<String, Object> getContextData() { return contextData; }
        public List<String> getToolCalls() { return toolCalls; }
        public List<Map<String, Object>> getToolResults() { return toolResults; }
        public List<Map<String, String>> getConversationHistory() { return conversationHistory; }
        
        /**
         * 添加工具调用记录
         */
        public void addToolCall(String toolName, Map<String, Object> args, Map<String, Object> result) {
            toolCalls.add(toolName);
            toolResults.add(result);
        }
        
        /**
         * 添加对话到历史
         */
        public void addHistoryMessage(String role, String content) {
            Map<String, String> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            conversationHistory.add(message);
        }
    }
    
    // ========== 配置方法 ==========
    
    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }
    
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public void setEnableRetry(boolean enableRetry) {
        this.enableRetry = enableRetry;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
