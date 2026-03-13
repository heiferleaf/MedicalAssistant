package com.whu.medicalbackend.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {
    
    @Autowired
    private RagService ragService;
    
    @PostMapping("/query")
    public ResponseEntity<RagResponse> queryRag(@RequestBody RagRequest request) {
        try {
            RagResponse response = ragService.queryRag(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            RagResponse errorResponse = new RagResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("RAG服务调用失败: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
}