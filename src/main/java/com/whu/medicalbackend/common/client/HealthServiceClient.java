package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.HealthDataDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HealthServiceClient {

    private final RestClient restClient;

    public HealthServiceClient(RestClient.Builder builder,
                               @Value("${service.health.url:http://health-service:8084}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public HealthDataDTO getTodayHealthData(Long userId) {
        return restClient.get()
                .uri("/api/health/today/{userId}", userId)
                .retrieve()
                .body(HealthDataDTO.class);
    }
}
