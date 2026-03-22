package com.whu.medicalbackend.ws;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
public class WsPubSubBroadcaster{
    private static final String CHANNLE_PREFIX = "ws:group:";

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.instance-id}")
    private String instance;

    public void pushToUser(Long userId, String jsonPayload, Long groupId) {
        // 现在本容器中推送
        WebSocketSession session = sessionManager.get(userId);

        if(session != null && session.isOpen()) {
            pushLocalSession(userId, jsonPayload, session);
        } else {
            try {
                BroadcastMessage msg = new BroadcastMessage(userId, jsonPayload, instance);
                String json = objectMapper.writeValueAsString(msg);
                redisTemplate.convertAndSend(CHANNLE_PREFIX + groupId, json);
            } catch (JsonProcessingException e) {
                log.error("【Pub/Sub】发布消息失败, userId={}", userId, e);
            }
        }
    }

    public void onRedisMessage(String messageBody, String channel) {
        try {
            BroadcastMessage msg = objectMapper.readValue(messageBody, BroadcastMessage.class);

            if(!msg.getSourceInstanceId().equals(instance)) {
                return;
            }

            WebSocketSession session = sessionManager.get(msg.getUserId());
            if(session != null && session.isOpen()) {
                pushLocalSession(msg.getUserId(), msg.getJsonPayload(), session);
            }

        } catch (JsonProcessingException e) {
            log.error("【Pub/Sub】处理订阅消息失败", e);
        }
    }

    private void pushLocalSession(Long userId, String jsonPayload, WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonPayload));
                log.debug("【Pub/Sub】推送成功, userId={}", userId);
            } catch (IOException e) {
                log.error("【Pub/Sub】推送失败, userId={}", userId, e);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BroadcastMessage {
        private Long userId;
        private String jsonPayload;
        private String sourceInstanceId;
    }
}