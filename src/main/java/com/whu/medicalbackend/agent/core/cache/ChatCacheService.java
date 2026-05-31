package com.whu.medicalbackend.agent.core.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatCacheService {

    private static final Logger log = LoggerFactory.getLogger(ChatCacheService.class);

    private final AiCacheManager aiCacheManager;
    private final ObjectMapper objectMapper;

    public ChatCacheService(AiCacheManager aiCacheManager, ObjectMapper objectMapper) {
        this.aiCacheManager = aiCacheManager;
        this.objectMapper = objectMapper;
    }

    public static String buildChatCacheKey(String userId, String sessionId, String message) {
        return AiCacheManager.buildPredictCacheKey(userId + "|" + sessionId + "|" + message);
    }

    /**
     * 检查请求是否适合缓存（L1/L2 级别：短文本，无图片/OCR）
     */
    public static boolean isCacheable(Map<String, Object> payload) {
        if (payload == null) return false;
        String message = String.valueOf(payload.getOrDefault("message", ""));
        return message.length() < 200
                && !message.contains("图片数据：")
                && !message.contains("/9j/")
                && !message.contains("OCR 识别结果：")
                && !message.contains("data:image/");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String cacheKey) {
        String cached = aiCacheManager.getCachedChatResult(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Map.class);
            } catch (Exception e) {
                log.warn("Chat 缓存反序列化失败", e);
            }
        }
        return null;
    }

    public void put(String cacheKey, Object result) {
        try {
            aiCacheManager.cacheChatResult(cacheKey, objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Chat 缓存写入失败", e);
        }
    }
}
