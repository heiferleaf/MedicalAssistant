package com.whu.medicalbackend.common.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MedicationServiceClient {

    private final RestClient restClient;

    public MedicationServiceClient(RestClient.Builder builder,
                                   @Value("${service.medication.url:http://medication-service:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public int countTasksByDate(Long userId, String date, Integer status) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/task/count")
                .queryParam("userId", userId)
                .queryParam("date", date);
        if (status != null) {
            uri.queryParam("status", status);
        }
        Integer count = restClient.get()
                .uri(uri.build().toUri())
                .retrieve()
                .body(Integer.class);
        return count != null ? count : 0;
    }
}
