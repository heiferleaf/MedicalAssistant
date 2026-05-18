package com.whu.medicalbackend.agent.flask;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.rag.RagRequest;
import com.whu.medicalbackend.agent.rag.RagResponse;
import com.whu.medicalbackend.agent.rag.RagService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FlaskRagProxyService {

    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public FlaskRagProxyService(RagService ragService, ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> query(String question, boolean withTrace, boolean withTiming) {
        RagRequest request = new RagRequest(question);
        request.setWithTrace(withTrace);
        request.setWithTiming(withTiming);

        RagResponse response = ragService.queryRag(request);
        return objectMapper.convertValue(response, new TypeReference<>() {
        });
    }
}
