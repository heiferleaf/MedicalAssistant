package com.whu.medicalbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class FlaskAgentProxyService {

    private final RestClient flaskRestClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FlaskAgentProxyService(RestClient flaskRestClient, ObjectMapper objectMapper) {
        this.flaskRestClient = flaskRestClient;
        this.objectMapper = objectMapper;
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
}
