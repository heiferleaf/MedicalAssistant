package com.whu.medicalbackend.common.infra.http;

<<<<<<< HEAD
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Component
public class AiHttpClient {
    
    public <T> T postJson(String url, Map<String, Object> body, Class<T> responseType) {
        return null;
    }
    
    public <T> T postJson(String url, Object body, Class<T> responseType) {
        return null;
    }
    
    public <T> T get(String url, Class<T> responseType) {
        return null;
    }
    
    public <T> T postMultipart(String url, MultiValueMap<String, Object> body, Class<T> responseType) {
        return null;
=======
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class AiHttpClient {

    private final RestClient flaskRestClient;

    public AiHttpClient(RestClient flaskRestClient) {
        this.flaskRestClient = flaskRestClient;
    }

    public <T> T postJson(String uri, Object body, Class<T> responseType) {
        return flaskRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public <T> T postJson(String uri, Object body, ParameterizedTypeReference<T> responseType) {
        return flaskRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public <T> T postMultipart(String uri, MultiValueMap<String, Object> body, Class<T> responseType) {
        return flaskRestClient.post()
                .uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public <T> T get(String uri, Class<T> responseType) {
        return flaskRestClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }

    public <T> T get(String uri, ParameterizedTypeReference<T> responseType) {
        return flaskRestClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
>>>>>>> fc315ac1946580259e7f744c73e93a43411327db
    }
}
