package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.flask.UnifiedFlaskClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RagService {

    private final UnifiedFlaskClient unifiedFlaskClient;
    private final ObjectMapper objectMapper;

    public RagService(UnifiedFlaskClient unifiedFlaskClient, ObjectMapper objectMapper) {
        this.unifiedFlaskClient = unifiedFlaskClient;
        this.objectMapper = objectMapper;
    }

    public RagResponse queryRag(RagRequest request) throws Exception {
        Map<String, Object> result = unifiedFlaskClient.queryRag(
                request.getQuestion(),
                request.isWithTrace(),
                request.isWithTiming()
        );

        return objectMapper.convertValue(result, RagResponse.class);
    }
}
