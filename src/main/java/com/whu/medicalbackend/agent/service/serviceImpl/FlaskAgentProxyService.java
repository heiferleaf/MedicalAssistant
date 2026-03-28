package com.whu.medicalbackend.agent.service.serviceImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class FlaskAgentProxyService {

    private final RestClient flaskRestClient;
    private final ObjectMapper objectMapper;
    private final String flaskBaseUrl;

    @Autowired
    public FlaskAgentProxyService(
            RestClient flaskRestClient,
            ObjectMapper objectMapper,
            @Value("${flask.base-url}") String flaskBaseUrl
    ) {
        this.flaskRestClient = flaskRestClient;
        this.objectMapper = objectMapper;
        this.flaskBaseUrl = flaskBaseUrl;
    }

    public Map<String, Object> chat(Map<String, Object> payload) {
        Object resp = flaskRestClient.post()
                .uri("/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> confirm(Map<String, Object> payload) {
        Object resp = flaskRestClient.post()
                .uri("/agent/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> health() {
        Object resp = flaskRestClient.get()
                .uri("/agent/health")
                .retrieve()
                .body(Object.class);
        Map<String, Object> map = objectMapper.convertValue(resp, new TypeReference<>() {});
        map.put("_proxy_target", flaskBaseUrl);
        return map;
    }
}
