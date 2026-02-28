package com.whu.medicalbackend.agent.flask;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class FlaskRagProxyService {

    private final RestClient flaskRestClient;
    private final ObjectMapper objectMapper;

    public FlaskRagProxyService(RestClient flaskRestClient, ObjectMapper objectMapper) {
        this.flaskRestClient = flaskRestClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> query(String question, boolean withTrace, boolean withTiming) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("question", question);
        payload.put("with_trace", withTrace);
        payload.put("with_timing", withTiming);

        Object resp = flaskRestClient.post()
                .uri("/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);

        return objectMapper.convertValue(resp, new TypeReference<>() {
        });
    }
}
