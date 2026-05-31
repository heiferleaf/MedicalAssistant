package com.whu.medicalbackend.agent.predict;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.core.cache.AiCacheManager;
import com.whu.medicalbackend.common.infra.http.AiHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PredictService {

    private static final Logger logger = LoggerFactory.getLogger(PredictService.class);

    private final AiHttpClient aiHttpClient;
    private final AiCacheManager aiCacheManager;
    private final ObjectMapper objectMapper;

    public PredictService(AiHttpClient aiHttpClient,
                          AiCacheManager aiCacheManager,
                          ObjectMapper objectMapper) {
        this.aiHttpClient = aiHttpClient;
        this.aiCacheManager = aiCacheManager;
        this.objectMapper = objectMapper;
    }

    public PredictResponse analyzeText(PredictRequest request) {
        String cacheKey = AiCacheManager.buildPredictCacheKey(request.getText());

        // 尝试从缓存获取
        String cached = aiCacheManager.getCachedPredictResult(cacheKey);
        if (cached != null) {
            logger.debug("Predict 缓存命中, key={}", cacheKey);
            try {
                return objectMapper.readValue(cached, PredictResponse.class);
            } catch (Exception e) {
                logger.warn("Predict 缓存反序列化失败, 回退到实际调用", e);
            }
        }

        PredictResponse response = aiHttpClient.postJson("/api/predict/analyze", request, PredictResponse.class);

        // 缓存结果
        if (response != null) {
            try {
                aiCacheManager.cachePredictResult(cacheKey, objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                logger.warn("Predict 缓存写入失败", e);
            }
        }

        return response;
    }
}
