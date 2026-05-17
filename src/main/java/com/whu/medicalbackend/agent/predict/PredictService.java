package com.whu.medicalbackend.agent.predict;

import com.whu.medicalbackend.common.infra.http.AiHttpClient;
import org.springframework.stereotype.Service;

@Service
public class PredictService {

    private final AiHttpClient aiHttpClient;

    public PredictService(AiHttpClient aiHttpClient) {
        this.aiHttpClient = aiHttpClient;
    }

    public PredictResponse analyzeText(PredictRequest request) {
        return aiHttpClient.postJson("/api/predict/analyze", request, PredictResponse.class);
    }
}
