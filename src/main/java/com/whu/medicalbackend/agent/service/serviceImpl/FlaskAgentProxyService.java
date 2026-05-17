package com.whu.medicalbackend.agent.service.serviceImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.infra.http.AiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FlaskAgentProxyService {

    private final AiHttpClient aiHttpClient;
    private final ObjectMapper objectMapper;
    private final String flaskBaseUrl;

    @Autowired
    public FlaskAgentProxyService(
            AiHttpClient aiHttpClient,
            ObjectMapper objectMapper,
            @Value("${flask.base-url}") String flaskBaseUrl
    ) {
        this.aiHttpClient = aiHttpClient;
        this.objectMapper = objectMapper;
        this.flaskBaseUrl = flaskBaseUrl;
    }

    public Map<String, Object> chat(Map<String, Object> payload) {
        Object resp = aiHttpClient.postJson("/agent/chat", payload, Object.class);
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> confirm(Map<String, Object> payload) {
        Object resp = aiHttpClient.postJson("/agent/confirm", payload, Object.class);
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> health() {
        Object resp = aiHttpClient.get("/agent/health", Object.class);
        Map<String, Object> map = objectMapper.convertValue(resp, new TypeReference<>() {});
        map.put("_proxy_target", flaskBaseUrl);
        return map;
    }
}
