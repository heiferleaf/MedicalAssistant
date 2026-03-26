package com.whu.medicalbackend.agent;

import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.langchain4j.agents.MedicalAgent;
import com.whu.medicalbackend.agent.core.memory.AgentMemoryRepository;
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
    private final String flaskBaseUrl;
    private final boolean llmEnabled;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            ChatModel chatModel,
            MedicalAgent medicalAgent,
            @Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl,
            @Value("${agent.llm.enabled:false}") boolean llmEnabled) {
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
    private Map<String, Object> handleMedicalAgentChat(String userId, String sessionId, String message,
            boolean withTrace, boolean withTiming) {
        try {
            logger.info("使用 Medical Agent 处理请求");
            
            // 检查是否包含图片数据（Base64）
            if (message.contains("图片数据：") && message.contains("/9j/")) {
                logger.info("检测到图片消息，手动调用 OCR 工具");
                // 提取 Base64 数据
                int base64Index = message.indexOf("/9j/");
                String base64Data = message.substring(base64Index);
                
                // 直接调用 OCR 工具
                try {
                    // 通过反射或其他方式调用 OCR 工具
                    // 这里我们直接调用 Flask OCR 服务
                    Map<String, Object> ocrResult = callFlaskOcr(base64Data);
                    
                    if (ocrResult != null && (Boolean) ocrResult.get("success")) {
                        logger.info("OCR 识别成功，结果：{}", ocrResult.get("output"));
                        
                        // 构建响应
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("success", true);
                        result.put("assistant_message", "药品识别结果：\n\n" + ocrResult.get("output"));
                        result.put("need_confirm", false);
                        result.put("actions", List.of());
                        
                        // 保存 OCR 结果到数据库
                        String output = (String) ocrResult.get("output");
                        memoryRepository.appendMessage(sessionId, userId, "assistant", output);
                        
                        if (withTrace) {
                            Map<String, Object> trace = new LinkedHashMap<>();
                            trace.put("agent_version", AGENT_VERSION);
                            trace.put("control", "ocr_manual");
                            trace.put("ocr_result", ocrResult);
                            result.put("trace", trace);
                        }
                        
                        return result;
                    } else {
                        logger.warn("OCR 识别失败：{}", ocrResult.get("message"));
                    }
                } catch (Exception e) {
                    logger.error("OCR 调用失败", e);
                }
            }
            
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
                        
                        // 将完整消息按字符流式发送
                        for (int i = 0; i < assistantMessage.length(); i++) {
                            char c = assistantMessage.charAt(i);
                            emitter.send(SseEmitter.event()
                                .name("message")
                                .data(String.valueOf(c)));
                            
                            // 每 30ms 发送一个字符，模拟流式效果（给前端渲染时间）
                            try {
                                Thread.sleep(30);
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
            
            // 将完整消息按字符流式发送
            for (int i = 0; i < response.length(); i++) {
                char c = response.charAt(i);
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(String.valueOf(c)));
                
                // 每 10ms 发送一个字符，模拟流式效果
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            fullResponse.append(response);
            memoryRepository.appendMessage(sessionId, userId, "assistant", response);
            
        } catch (Exception e) {
            logger.error("简单 LLM 流式调用失败", e);
            emitter.send(SseEmitter.event()
                .name("error")
                .data(Map.of("error", "LLM 流式调用失败：" + e.getMessage())));
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
        
        // 使用 HttpURLConnection 发送 multipart/form-data 请求
        java.net.URL url = new java.net.URL(flaskBaseUrl + "/ocr/predict");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            // 写入文件部分
            String disposition = "Content-Disposition: form-data; name=\"file\"; filename=\"drug.jpg\"\r\n";
            String contentType = "Content-Type: image/jpeg\r\n\r\n";
            
            os.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(disposition.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(contentType.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(imageBytes);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.flush();
        }
        
        // 获取响应
        int statusCode = conn.getResponseCode();
        logger.info("OCR 响应状态码：{}", statusCode);
        
        java.io.InputStream inputStream;
        if (statusCode >= 400) {
            inputStream = conn.getErrorStream();
        } else {
            inputStream = conn.getInputStream();
        }
        
        String response;
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
            response = responseBuilder.toString();
        }
        
        logger.info("OCR 响应内容：{}", response);
        
        // 解析响应
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> ocrResult = mapper.readValue(response, Map.class);
        
        // 检查响应状态
        if ("success".equals(ocrResult.get("status"))) {
            logger.info("OCR 识别成功");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("ocr_result", ocrResult.get("ocr_result"));
            result.put("output", ocrResult.get("output"));
            return result;
        } else {
            logger.warn("OCR 识别失败：{}", ocrResult.get("message"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", ocrResult.get("message"));
            return result;
        }
    }
}
