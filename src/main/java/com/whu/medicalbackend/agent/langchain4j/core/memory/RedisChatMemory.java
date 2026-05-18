package com.whu.medicalbackend.agent.langchain4j.core.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式 ChatMemory
 * 替代 LangChain4j 默认的 MessageWindowChatMemory（内存存储）
 * 实现跨实例的对话历史共享
 */
public class RedisChatMemory implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemory.class);

    private static final String MEMORY_PREFIX = "agent:chat:memory:";
    private static final long DEFAULT_TTL_HOURS = 24;

    private final Object memoryId;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;

    public RedisChatMemory(Object memoryId, StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper, int maxMessages) {
        this.memoryId = memoryId;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxMessages = maxMessages;
    }

    @Override
    public String id() {
        return String.valueOf(memoryId);
    }

    @Override
    public void add(ChatMessage message) {
        String key = redisKey();
        try {
            Map<String, Object> serialized = serializeMessage(message);
            String json = objectMapper.writeValueAsString(serialized);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            trimIfNeeded(key);
        } catch (Exception e) {
            logger.error("Failed to add message to RedisChatMemory: {}", id(), e);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        String key = redisKey();
        try {
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return List.of();
            }
            List<ChatMessage> result = new ArrayList<>();
            for (String json : jsonList) {
                ChatMessage msg = deserializeMessage(json);
                if (msg != null) {
                    result.add(msg);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to get messages from RedisChatMemory: {}", id(), e);
            return List.of();
        }
    }

    @Override
    public void clear() {
        redisTemplate.delete(redisKey());
    }

    private String redisKey() {
        return MEMORY_PREFIX + id();
    }

    private void trimIfNeeded(String key) {
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > maxMessages) {
            redisTemplate.opsForList().trim(key, size - maxMessages, -1);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> serializeMessage(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (message instanceof SystemMessage sm) {
            map.put("type", "system");
            map.put("text", sm.text());
        } else if (message instanceof UserMessage um) {
            map.put("type", "user");
            map.put("text", um.singleText());
        } else if (message instanceof AiMessage am) {
            map.put("type", "ai");
            map.put("text", am.text());
            if (am.toolExecutionRequests() != null && !am.toolExecutionRequests().isEmpty()) {
                map.put("toolExecutionRequests", am.toolExecutionRequests().toString());
            }
        } else if (message instanceof ToolExecutionResultMessage trm) {
            map.put("type", "tool_result");
            map.put("id", trm.id());
            map.put("toolName", trm.toolName());
            map.put("text", trm.text());
        } else {
            map.put("type", "unknown");
            map.put("text", message.toString());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private ChatMessage deserializeMessage(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, LinkedHashMap.class);
            String type = (String) map.get("type");
            String text = (String) map.get("text");
            if (text == null) text = "";

            return switch (type) {
                case "system" -> new SystemMessage(text);
                case "user" -> UserMessage.from(text);
                case "ai" -> AiMessage.from(text);
                case "tool_result" -> {
                    String id = (String) map.get("id");
                    String toolName = (String) map.get("toolName");
                    yield new ToolExecutionResultMessage(id, toolName, text);
                }
                default -> UserMessage.from(text);
            };
        } catch (Exception e) {
            logger.warn("Failed to deserialize chat message: {}", json, e);
            return UserMessage.from(json);
        }
    }
}
