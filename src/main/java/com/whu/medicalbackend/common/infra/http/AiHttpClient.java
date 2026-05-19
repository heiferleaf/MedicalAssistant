package com.whu.medicalbackend.common.infra.http;

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
    }
}
