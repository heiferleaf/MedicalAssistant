package com.whu.medicalbackend.agent;

import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.langchain4j.agents.MedicalAgent;
import com.whu.medicalbackend.agent.core.memory.AgentMemoryRepository;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionBroadcaster;
import com.whu.medicalbackend.agent.service.OcrService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Service
public class AgentOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestratorService.class);

    public static final String AGENT_VERSION = "schemeA_spring_memory_v1_langchain4j_2026-03-08";

    private final AgentMemoryRepository memoryRepository;
    private final FlaskRagProxyService flaskRagProxyService;
    private final ChatModel chatModel;
    private final MedicalAgent medicalAgent;
    private final ToolExecutionBroadcaster toolExecutionBroadcaster;
    private final OcrService ocrService;
    private final String flaskBaseUrl;
    private final boolean llmEnabled;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            ChatModel chatModel,
            MedicalAgent medicalAgent,
            ToolExecutionBroadcaster toolExecutionBroadcaster,
            OcrService ocrService,
            @Value("${flask.base-url:http://localhost:8001}") String flaskBaseUrl,
            @Value("${agent.llm.enabled:false}") boolean llmEnabled) {
        this.memoryRepository = memoryRepository;
        this.flaskRagProxyService = flaskRagProxyService;
        this.chatModel = chatModel;
        this.medicalAgent = medicalAgent;
        this.toolExecutionBroadcaster = toolExecutionBroadcaster;
        this.ocrService = ocrService;
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
    private Map<String, Object> handleMedicalAgentChat(String userId, String sessionId, String message,
                                                       boolean withTrace, boolean withTiming) {
        try {
            logger.info("使用 Medical Agent 处理请求");
            
            // 检查是否包含 OCR 结果（前端已经调用过 OCR）
            boolean hasOcrResult = message.contains("OCR 识别结果：");
            boolean hasBase64Image = message.contains("图片数据：") && message.contains("/9j/");
            boolean hasImagePath = message.contains("图片路径：") && message.contains("/images/drug_");
            
            if (hasOcrResult || hasBase64Image || hasImagePath) {
                logger.info("检测到图片消息（OCR 结果={}, Base64={}, 路径={})，直接处理不经过 LLM", hasOcrResult, hasBase64Image, hasImagePath);
                
                String ocrText = null;
                if (hasOcrResult) {
                    // 前端已经调用过 OCR，直接提取 OCR 结果
                    int ocrStart = message.indexOf("OCR 识别结果：") + 7;
                    int ocrEnd = message.indexOf("。请根据", ocrStart);
                    if (ocrEnd == -1) ocrEnd = message.length();
                    ocrText = message.substring(ocrStart, ocrEnd).trim();
                    logger.info("从消息中提取 OCR 结果：{}", ocrText.substring(0, Math.min(100, ocrText.length())));
                    
                    // 直接使用 OCR 结果回答
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", true);
                    result.put("assistant_message", "药品识别结果：\n\n" + ocrText);
                    result.put("need_confirm", false);
                    result.put("actions", List.of());
                    
                    // 保存 OCR 结果到数据库
                    memoryRepository.appendMessage(sessionId, userId, "assistant", ocrText);
                    
                    if (withTrace) {
                        Map<String, Object> trace = new LinkedHashMap<>();
                        trace.put("agent_version", AGENT_VERSION);
                        trace.put("control", "ocr_direct");
                        Map<String, Object> ocrData = new LinkedHashMap<>();
                        ocrData.put("output", ocrText);
                        trace.put("ocr_result", ocrData);
                        result.put("trace", trace);
                    }
                    
                    return result;
                } else {
                    // 如果有 Base64 或路径但没有 OCR 结果，调用 OCR 服务
                    logger.info("未检测到 OCR 结果，但有图片数据，需要调用 OCR 服务（暂未实现）");
                    // TODO: 实现 Base64 或路径的 OCR 调用
                }
            }
            
            // 没有图片，使用 Medical Agent 正常处理
            Map<String, Object> result = medicalAgent.execute(sessionId, userId, message, message);

            // 保存 AI 回复到数据库
            if (result != null && result.get("success") != null && (Boolean) result.get("success")) {
                String assistantMessage = (String) result.get("assistant_message");
                String actionType = (String) result.get("action_type");
                String actionData = (String) result.get("action_data");

                if (assistantMessage != null && !assistantMessage.isBlank()) {
                    if (actionType != null && !actionType.isBlank() && actionData != null) {
                        // 保存带 action 的消息
                        memoryRepository.appendMessageWithAction(
                                sessionId, userId, "assistant", assistantMessage, actionType, actionData
                        );
                    } else {
                        // 保存普通消息
                        memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                    }
                }
            }

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
    private Map<String, Object> handleSimpleLlmChat(String userId, String sessionId, String message, boolean withTrace,
                                                    boolean withTiming) {
        try {
            logger.info("使用简单 LLM 调用处理请求");

            // 从数据库获取对话历史
            List<Map<String, Object>> messages = memoryRepository.getRecentMessages(sessionId, 5);
            List<Map<String, String>> history = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                history.add(Map.of(
                        "role", str(msg.get("role")),
                        "content", str(msg.get("content"))));
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
        out.put("memory", Map.of("backend", "spring.jdbc.mysql", "tables",
                List.of("agent_sessions", "agent_messages", "agent_pending_actions")));
        out.put("rag", Map.of("target", flaskBaseUrl + "/rag/query"));
        out.put("server_time", java.time.OffsetDateTime.now().toString());
        return out;
    }

    /**
     * 流式聊天接口（SSE）
     */
    public void chatStream(String userId, String sessionId, String message, SseEmitter emitter) throws IOException {
        logger.info("开始流式聊天，userId={}, sessionId={}, message 长度={}", userId, sessionId, message.length());
        
        // 注册 SSE 发射器
        toolExecutionBroadcaster.registerEmitter(sessionId, emitter);
        
        try {
            // 保存用户消息
            memoryRepository.appendMessage(sessionId, userId, "user", message);
            
            StringBuilder fullResponse = new StringBuilder();
            
            // 优先使用 Medical Agent（如果启用）
            if (llmEnabled) {
                try {
                    logger.info("使用 Medical Agent 进行流式处理");
                    // 对于 Medical Agent，我们先使用非流式调用，然后模拟流式返回
                    Map<String, Object> result = medicalAgent.execute(sessionId, userId, message, message);
                    
                    if (result != null && result.get("success") != null && (Boolean) result.get("success")) {
                        String assistantMessage = (String) result.get("assistant_message");
                        String actionType = (String) result.get("action_type");
                        String actionData = (String) result.get("action_data");
                        
                        if (assistantMessage != null && !assistantMessage.isBlank()) {
                            logger.info("开始流式发送消息，长度：{}", assistantMessage.length());
                            
                            // 将完整消息按批次流式发送（每批 10 个字符）
                            int batchSize = 10;
                            for (int i = 0; i < assistantMessage.length(); i += batchSize) {
                                int end = Math.min(i + batchSize, assistantMessage.length());
                                String batch = assistantMessage.substring(i, end);
                                emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(batch));
                                
                                // 每 50ms 发送一批，模拟流式效果
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                            
                            logger.info("流式发送完成");
                            fullResponse.append(assistantMessage);
                            
                            // 保存 AI 回复到数据库
                            if (actionType != null && !actionType.isBlank() && actionData != null) {
                                memoryRepository.appendMessageWithAction(
                                    sessionId, userId, "assistant", assistantMessage, actionType, actionData
                                );
                                // 发送 action 数据
                                emitter.send(SseEmitter.event()
                                    .name("action")
                                    .data(Map.of(
                                        "action_type", actionType,
                                        "action_data", actionData
                                    )));
                            } else {
                                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                            }
                        }
                    }
                    
                    // 发送结束事件
                    emitter.send(SseEmitter.event()
                        .name("end")
                        .data(""));
                    logger.info("SSE 流式输出完成，userId={}, sessionId={}", userId, sessionId);
                    return;
                } catch (Exception e) {
                    logger.error("Medical Agent 流式处理失败，回退到简单 LLM 流式调用", e);
                }
            }
            
            // 回退到简单 LLM 流式调用
            try {
                logger.info("使用简单 LLM 进行流式处理");
                
                // 从数据库获取对话历史
                List<Map<String, Object>> messages = memoryRepository.getRecentMessages(sessionId, 5);
                List<Map<String, String>> history = new ArrayList<>();
                for (Map<String, Object> msg : messages) {
                    history.add(Map.of(
                            "role", str(msg.get("role")),
                            "content", str(msg.get("content"))));
                }
                
                // 调用 LLM
                SystemMessage systemMessage = SystemMessage.from("你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。");
                UserMessage userMessage = UserMessage.from(message);
                ChatResponse chatResponse = chatModel.chat(systemMessage, userMessage);
                AiMessage aiMessage = chatResponse.aiMessage();
                String response = aiMessage.text();
                
                // 将完整消息按批次流式发送（每批 10 个字符）
                int batchSize = 10;
                for (int i = 0; i < response.length(); i += batchSize) {
                    int end = Math.min(i + batchSize, response.length());
                    String batch = response.substring(i, end);
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(batch));
                    
                    // 每 50ms 发送一批，模拟流式效果
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                fullResponse.append(response);
                memoryRepository.appendMessage(sessionId, userId, "assistant", response);
                
                // 发送结束事件
                emitter.send(SseEmitter.event()
                    .name("end")
                    .data(""));
                logger.info("SSE 流式输出完成，userId={}, sessionId={}", userId, sessionId);
                
            } catch (Exception e) {
                logger.error("简单 LLM 流式调用失败", e);
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", "LLM 流式调用失败：" + e.getMessage())));
            }
        } finally {
            // 注销 SSE 发射器
            toolExecutionBroadcaster.unregisterEmitter(sessionId);
        }
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

    /**
     * 直接调用 Flask OCR 服务
     */
    private Map<String, Object> callFlaskOcr(String base64Data) throws Exception {
        logger.info("调用 Flask OCR 接口识别药物图片");
        
        // Base64 解码为字节数组
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
        logger.info("图片数据解码成功，大小：{} bytes", imageBytes.length);
        
        // 使用 OcrService 调用
        Map<String, Object> ocrResult = ocrService.recognizeDrugImage(imageBytes);
        
        if (ocrResult != null && "success".equals(ocrResult.get("status"))) {
            logger.info("OCR 识别成功，结果：{}", ocrResult.get("output"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("ocr_result", ocrResult.get("ocr_result"));
            result.put("output", ocrResult.get("output"));
            return result;
        } else {
            logger.warn("OCR 识别失败：{}", ocrResult != null ? ocrResult.get("message") : "null");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", ocrResult != null ? ocrResult.get("message") : "OCR 服务不可用");
            return result;
        }
    }
}

