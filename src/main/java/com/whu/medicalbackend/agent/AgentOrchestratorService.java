package com.whu.medicalbackend.agent;

import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.langchain4j.MedicalAgent;
import com.whu.medicalbackend.agent.memory.AgentMemoryRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestratorService.class);

    public static final String AGENT_VERSION = "schemeA_spring_memory_v1_langchain4j_2026-03-08";

    private final AgentMemoryRepository memoryRepository;
    private final FlaskRagProxyService flaskRagProxyService;
    private final ChatModel chatModel;
    private final MedicalAgent medicalAgent;
    private final String flaskBaseUrl;
    private final boolean llmEnabled;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            ChatModel chatModel,
            MedicalAgent medicalAgent,
            @Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl,
            @Value("${agent.llm.enabled:false}") boolean llmEnabled
    ) {
        this.memoryRepository = memoryRepository;
        this.flaskRagProxyService = flaskRagProxyService;
        this.chatModel = chatModel;
        this.medicalAgent = medicalAgent;
        this.flaskBaseUrl = flaskBaseUrl;
        this.llmEnabled = llmEnabled;

        logger.info("AgentOrchestratorService initialized, LLM enabled: {}", llmEnabled);
    }

    public Map<String, Object> chat(Map<String, Object> payload) {
        String userId = str(payload.get("user_id"));
        String sessionId = str(payload.get("session_id"));
        String message = str(payload.get("message"));
        boolean withTrace = bool(payload.get("with_trace"));
        boolean withTiming = bool(payload.get("with_timing"));
        
        // 默认启用 withTrace 以显示多轮调用详情
        if (payload.get("with_trace") == null) {
            withTrace = true;
        }

        if (userId.isBlank() || sessionId.isBlank()) {
            return Map.of("success", false, "error", "user_id 和 session_id 不能为空", "status", 400);
        }
        if (message.isBlank()) {
            return Map.of("success", false, "error", "message 不能为空", "status", 400);
        }

        memoryRepository.appendMessage(sessionId, userId, "user", message);

        // 优先使用 Medical Agent（如果启用）
        if (llmEnabled) {
            Map<String, Object> agentResult = handleMedicalAgentChat(userId, sessionId, message, withTrace, withTiming);
            if (agentResult != null) {
                return agentResult;
            }
        }

        // 回退到简单 LLM 调用
        return handleSimpleLlmChat(userId, sessionId, message, withTrace, withTiming);
    }

    /**
     * 使用 Medical Agent 处理请求
     */
    private Map<String, Object> handleMedicalAgentChat(String userId, String sessionId, String message, boolean withTrace, boolean withTiming) {
        try {
            logger.info("使用 Medical Agent 处理请求");
            Map<String, Object> result = medicalAgent.execute(sessionId, userId, message);
            
            if (withTrace) {
                Map<String, Object> trace = new LinkedHashMap<>();
                trace.put("agent_version", AGENT_VERSION);
                trace.put("control", "langchain4j_agent");
                result.put("trace", trace);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Medical Agent 失败，回退到简单 LLM 调用", e);
            return null;
        }
    }

    /**
     * 简单 LLM 调用（回退方案）
     */
    private Map<String, Object> handleSimpleLlmChat(String userId, String sessionId, String message, boolean withTrace, boolean withTiming) {
        try {
            logger.info("使用简单 LLM 调用处理请求");
            
            // 从数据库获取对话历史
            List<Map<String, Object>> messages = memoryRepository.getRecentMessages(sessionId, 5);
            List<Map<String, String>> history = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                history.add(Map.of(
                    "role", str(msg.get("role")),
                    "content", str(msg.get("content"))
                ));
            }
            
            // 直接调用 LLM（不使用 Function Call）
            SystemMessage systemMessage = SystemMessage.from("你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。");
            UserMessage userMessage = UserMessage.from(message);
            ChatResponse chatResponse = chatModel.chat(systemMessage, userMessage);
            AiMessage aiMessage = chatResponse.aiMessage();
            String response = aiMessage.text();
            
            memoryRepository.appendMessage(sessionId, userId, "assistant", response);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", response);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            
            if (withTrace) {
                Map<String, Object> trace = new LinkedHashMap<>();
                trace.put("agent_version", AGENT_VERSION);
                trace.put("control", "llm_simple");
                trace.put("llm_response", response);
                result.put("trace", trace);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("简单 LLM 调用失败", e);
            return Map.of("success", false, "message", "LLM 调用失败：" + e.getMessage());
        }
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("module", "agent");
        out.put("agent_version", AGENT_VERSION);
        out.put("control_mode", llmEnabled ? "llm" : "rule");
        out.put("llm_configured", true);
        out.put("memory", Map.of("backend", "spring.jdbc.mysql", "tables", List.of("agent_sessions", "agent_messages", "agent_pending_actions")));
        out.put("rag", Map.of("target", flaskBaseUrl + "/rag/query"));
        out.put("server_time", java.time.OffsetDateTime.now().toString());
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }
}
