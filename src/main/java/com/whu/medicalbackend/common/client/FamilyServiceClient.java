package com.whu.medicalbackend.common.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FamilyServiceClient {

    private final RestClient restClient;

    public FamilyServiceClient(RestClient.Builder builder,
                               @Value("${service.family.url:http://family-service:8083}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Long getGroupIdByUserId(Long userId) {
        return restClient.get()
                .uri("/api/family/group/user/{userId}/groupId", userId)
                .retrieve()
                .body(Long.class);
    }

    public void insertEventLog(Long groupId, Long userId, String eventType, String medicineName) {
        restClient.post()
                .uri("/api/family/group/event-log")
                .body(new EventLogRequest(groupId, userId, eventType, medicineName))
                .retrieve()
                .toBodilessEntity();
    }

    private record EventLogRequest(Long groupId, Long userId, String eventType, String medicineName) {}
}
