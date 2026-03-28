package com.whu.medicalbackend.agent.predict;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PredictService {

    // 如果未配置全局的 RestTemplate Bean，此处理论也可先直接 new 一个
    private final RestTemplate restTemplate = new RestTemplate();
    private final String FLASK_URL = "http://localhost:8001/api/predict/analyze";

    public PredictResponse analyzeText(PredictRequest request) {
        return restTemplate.postForObject(FLASK_URL, request, PredictResponse.class);
    }
}