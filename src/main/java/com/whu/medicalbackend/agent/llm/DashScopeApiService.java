package com.whu.medicalbackend.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashScopeApiService {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeApiService.class);
    
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public DashScopeApiService(
            ObjectMapper objectMapper,
            @Value("${dashscope.api-key:}") String apiKey,
            @Value("${dashscope.model:}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, Collections.emptyList());
    }

    public String chat(String systemPrompt, String userMessage, List<Map<String, String>> conversationHistory) {
        FunctionCallResult result = chatWithFunction(systemPrompt, userMessage, conversationHistory, null);
        return result.getContent();
    }

    /**
     * 带 Function Call 的聊天（支持联网搜索）
     */
    public FunctionCallResult chatWithFunction(
            String systemPrompt, 
            String userMessage, 
            List<Map<String, String>> conversationHistory,
            ArrayNode tools
    ) {
        if (!isConfigured()) {
            throw new RuntimeException("DashScope API 未配置");
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("result_format", "message");

            ArrayNode messages = objectMapper.createArrayNode();
            
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
            
            for (Map<String, String> msg : conversationHistory) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.get("role"));
                msgNode.put("content", msg.get("content"));
                messages.add(msgNode);
            }
            
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.set("messages", messages);
            
            // 添加工具定义
            if (tools != null && tools.size() > 0) {
                requestBody.set("tools", tools);
                requestBody.put("tool_choice", "auto");
            }
            
            // 启用联网搜索（阿里云 DashScope 内置功能）
            // 注意：需要使用 qwen-max、qwen3-max 等支持搜索的模型
            requestBody.put("enable_search", true);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.info("DashScope Request: {}", jsonBody);
            
            // 使用 OpenAI 兼容 API（支持联网搜索）
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("utf-8"));
            }

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();
            java.io.InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            if (responseCode != 200) {
                throw new RuntimeException("DashScope API 返回错误 " + responseCode + ": " + response);
            }

            logger.info("DashScope Response: {}", response);

            return parseFunctionCallResponse(response.toString());
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("DashScope API 调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析 Function Call 响应
     */
    private FunctionCallResult parseFunctionCallResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (!choices.isArray() || choices.size() == 0) {
            return FunctionCallResult.withContent("无法解析响应");
        }
        
        JsonNode message = choices.get(0).path("message");
        JsonNode toolCalls = message.path("tool_calls");
        
        // 检查是否有工具调用
        if (toolCalls.isArray() && toolCalls.size() > 0) {
            JsonNode firstToolCall = toolCalls.get(0);
            String functionName = firstToolCall.path("function").path("name").asText();
            String argumentsStr = firstToolCall.path("function").path("arguments").asText();
            
            Map<String, Object> arguments = objectMapper.readValue(argumentsStr, Map.class);
            return FunctionCallResult.of(functionName, arguments);
        }
        
        // 没有工具调用，返回普通文本
        String content = message.path("content").asText();
        return FunctionCallResult.withContent(content);
    }

    public Map<String, Object> chatWithJson(String systemPrompt, String userMessage) {
        String content = chat(systemPrompt, userMessage);
        try {
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            return Map.of("content", content, "error", "JSON 解析失败");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
    }
}
