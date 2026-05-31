package com.whu.medicalbackend.common.infra.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class AiHttpClient {

    private final RestClient restClient;

    public AiHttpClient(@Qualifier("flaskRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> T postJson(String path, Object payload, Class<T> responseType) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(responseType);
    }

    public <T> T get(String path, Class<T> responseType) {
        return restClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    public <T> T postMultipart(String path, MultiValueMap<String, Object> body, Class<T> responseType) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(responseType);
    }
}
