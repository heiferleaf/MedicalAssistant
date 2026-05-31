package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.UserDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient.Builder builder,
                             @Value("${service.user.url:http://user-service:8081}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public UserDTO getUserById(Long userId) {
        return restClient.get()
                .uri("/api/user/{id}/detail", userId)
                .retrieve()
                .body(UserDTO.class);
    }

    public UserDTO getUserByPhone(String phone) {
        return restClient.get()
                .uri("/api/user/phone/{phone}", phone)
                .retrieve()
                .body(UserDTO.class);
    }
}