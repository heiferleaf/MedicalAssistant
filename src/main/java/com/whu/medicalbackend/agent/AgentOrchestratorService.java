package com.whu.medicalbackend.agent;

import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.langchain4j.agents.MedicalAgent;
import com.whu.medicalbackend.agent.core.memory.AgentMemoryRepository;
import com.whu.medicalbackend.agent.core.task.AgentTaskService;
import com.whu.medicalbackend.agent.core.task.AgentTask;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionBroadcaster;
import com.whu.medicalbackend.agent.service.OcrService;
import com.whu.medicalbackend.agent.service.ToolExecutionPendingService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class AgentOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestratorService.class);

    public static final String AGENT_VERSION = "schemeA_spring_memory_v1_langchain4j_2026-03-08";

    private final AgentMemoryRepository memoryRepository;
    private final FlaskRagProxyService flaskRagProxyService;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final MedicalAgent medicalAgent;
    private final ToolExecutionBroadcaster toolExecutionBroadcaster;
    private final OcrService ocrService;
    private final ToolExecutionPendingService toolExecutionPendingService;
    private final AgentTaskService agentTaskService;
    private final String flaskBaseUrl;
    private final boolean llmEnabled;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<StreamingChatModel> streamingChatModelProvider,
            ObjectProvider<MedicalAgent> medicalAgentProvider,
            ToolExecutionBroadcaster toolExecutionBroadcaster,
            OcrService ocrService,
            ToolExecutionPendingService toolExecutionPendingService,
            AgentTaskService agentTaskService,
            @Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl,
            @Value("${agent.llm.enabled:false}") boolean llmEnabled) {
        this.memoryRepository = memoryRepository;
        this.flaskRagProxyService = flaskRagProxyService;
        // DashScope key 缺失时不创建 LLM Bean，Agent 服务仍保持健康，只在聊天入口返回配置提示。
        this.chatModel = chatModelProvider.getIfAvailable();
        this.streamingChatModel = streamingChatModelProvider.getIfAvailable();
        this.medicalAgent = medicalAgentProvider.getIfAvailable();
        this.toolExecutionPendingService = toolExecutionPendingService;
        this.toolExecutionBroadcaster = toolExecutionBroadcaster;
        this.ocrService = ocrService;
        this.agentTaskService = agentTaskService;
        this.flaskBaseUrl = flaskBaseUrl;
        this.llmEnabled = llmEnabled && this.chatModel != null;

        logger.info("AgentOrchestratorService initialized, LLM enabled: {}, chatModelConfigured: {}, medicalAgentConfigured: {}",
                this.llmEnabled, this.chatModel != null, this.medicalAgent != null);
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
        if (llmEnabled && medicalAgent != null) {
            Map<String, Object> agentResult = handleMedicalAgentChat(userId, sessionId, message, withTrace, withTiming);
            if (agentResult != null) {
                return agentResult;
            }
        }

        if (chatModel == null) {
            return llmUnavailableResult(withTrace);
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

            // 从数据库获取对话历史，构建多轮上下文
            List<Map<String, Object>> recentMessages = memoryRepository.getRecentMessages(sessionId, 5);
            List<ChatMessage> chatMessages = new ArrayList<>();
            chatMessages.add(SystemMessage.from("你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。"));
            for (Map<String, Object> msg : recentMessages) {
                String role = str(msg.get("role"));
                String content = str(msg.get("content"));
                if ("user".equals(role)) {
                    chatMessages.add(UserMessage.from(content));
                } else if ("assistant".equals(role)) {
                    chatMessages.add(AiMessage.from(content));
                }
            }
            chatMessages.add(UserMessage.from(message));

            ChatResponse chatResponse = chatModel.chat(chatMessages);
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

    /**
     * 提交聊天任务（带请求分级）
     * 返回 taskId，客户端可通过 taskId 轮询结果
     */
    public AgentTask submitChatTask(Map<String, Object> payload) {
        String userId = str(payload.get("user_id"));
        String sessionId = str(payload.get("session_id"));

        return agentTaskService.submit(sessionId, userId, payload, () -> {
            Map<String, Object> result = chat(payload);
            return result;
        });
    }

    /**
     * 获取异步任务结果
     */
    public AgentTask getTaskStatus(String taskId) {
        return agentTaskService.getTask(taskId);
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("module", "agent");
        out.put("agent_version", AGENT_VERSION);
        out.put("control_mode", llmEnabled ? "llm" : "rule");
        out.put("llm_configured", chatModel != null);
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

        toolExecutionBroadcaster.registerEmitter(sessionId, emitter);

        try {
            memoryRepository.appendMessage(sessionId, userId, "user", message);

            // ── MedicalAgent 路径 ──────────────────────────────────────────────
            // Agent 通过 ToolExecutionBroadcaster 实时推送工具调用进度；
            // 文本回复拿到后直接整段发送，无需人造 sleep。
            if (llmEnabled && medicalAgent != null) {
                try {
                    logger.info("使用 Medical Agent 进行流式处理");
                    String assistantMessage = medicalAgent.chat(sessionId, userId, message);
                    logger.info("Agent 文本回复，长度={}", assistantMessage == null ? 0 : assistantMessage.length());

                    String actionType = null;
                    String actionData = null;
                    List<?> pendingRequests = toolExecutionPendingService.getUserPendingRequests(Long.parseLong(userId));
                    if (pendingRequests != null && !pendingRequests.isEmpty()) {
                        Object pending = pendingRequests.get(0);
                        if (pending instanceof Map<?, ?> pendingMap) {
                            actionType = (String) pendingMap.get("action_type");
                            actionData = (String) pendingMap.get("tool_args_json");
                        }
                    }

                    if (assistantMessage != null && !assistantMessage.isBlank()) {
                        emitter.send(SseEmitter.event().name("message").data(assistantMessage));
                        if (actionType != null && !actionType.isBlank() && actionData != null) {
                            memoryRepository.appendMessageWithAction(
                                    sessionId, userId, "assistant", assistantMessage, actionType, actionData);
                            emitter.send(SseEmitter.event().name("action")
                                    .data(Map.of("action_type", actionType, "action_data", actionData)));
                        } else {
                            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                        }
                    }
                    emitter.send(SseEmitter.event().name("end").data(""));
                    return;
                } catch (Exception e) {
                    logger.error("Medical Agent 流式处理失败，回退到简单 LLM 流式调用", e);
                }
            }

            if (streamingChatModel == null && chatModel == null) {
                emitter.send(SseEmitter.event().name("message")
                        .data("LLM 未配置，当前无法进行智能问答。请配置 DASHSCOPE_API_KEY 后重试。"));
                emitter.send(SseEmitter.event().name("end").data(""));
                return;
            }

            // ── 真正的流式 LLM 路径 ───────────────────────────────────────────
            if (streamingChatModel != null) {
                logger.info("使用 StreamingChatModel 进行流式处理");
                CountDownLatch latch = new CountDownLatch(1);
                StringBuilder fullResponse = new StringBuilder();

                streamingChatModel.chat(
                        List.of(
                                SystemMessage.from("你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。"),
                                UserMessage.from(message)),
                        new StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String token) {
                                try {
                                    fullResponse.append(token);
                                    emitter.send(SseEmitter.event().name("message").data(token));
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }

                            @Override
                            public void onCompleteResponse(ChatResponse response) {
                                latch.countDown();
                            }

                            @Override
                            public void onError(Throwable error) {
                                logger.error("StreamingChatModel 错误", error);
                                latch.countDown();
                            }
                        });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String response = fullResponse.toString();
                if (!response.isBlank()) {
                    memoryRepository.appendMessage(sessionId, userId, "assistant", response);
                }
                emitter.send(SseEmitter.event().name("end").data(""));
                logger.info("SSE 流式输出完成，userId={}, sessionId={}", userId, sessionId);
                return;
            }

            // ── 非流式兜底（StreamingChatModel 不可用时）──────────────────────
            logger.info("StreamingChatModel 不可用，回退到阻塞 ChatModel");
            try {
                ChatResponse chatResponse = chatModel.chat(
                        SystemMessage.from("你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。"),
                        UserMessage.from(message));
                String response = chatResponse.aiMessage().text();
                emitter.send(SseEmitter.event().name("message").data(response));
                memoryRepository.appendMessage(sessionId, userId, "assistant", response);
                emitter.send(SseEmitter.event().name("end").data(""));
            } catch (Exception e) {
                logger.error("阻塞 LLM 调用失败", e);
                emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("error", "LLM 调用失败：" + e.getMessage())));
            }
        } finally {
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

    private Map<String, Object> llmUnavailableResult(boolean withTrace) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", "LLM 未配置，请配置 DASHSCOPE_API_KEY 后重试");
        result.put("need_confirm", false);
        result.put("actions", List.of());
        if (withTrace) {
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("agent_version", AGENT_VERSION);
            trace.put("control", "llm_unavailable");
            result.put("trace", trace);
        }
        return result;
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
