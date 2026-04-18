package com.whu.medicalbackend.agent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RagService {
    
    @Value("${rag.service.url}")
    private String ragServiceUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public RagService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public RagResponse queryRag(RagRequest request) throws Exception {
        String url = ragServiceUrl + "/rag/query";
        
        // 添加调试日志
        System.out.println("调用RAG服务URL: " + url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RagRequest> httpRequest = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, httpRequest, String.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return objectMapper.readValue(response.getBody(), RagResponse.class);
        } else {
            throw new RuntimeException("RAG服务返回异常状态: " + response.getStatusCode());
        }
    }
}