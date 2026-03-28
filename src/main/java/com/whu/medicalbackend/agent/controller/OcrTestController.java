package com.whu.medicalbackend.agent.controller;

import com.whu.medicalbackend.agent.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR 测试接口
 */
@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OcrTestController {

    private static final Logger logger = LoggerFactory.getLogger(OcrTestController.class);

    private final OcrService ocrService;

    public OcrTestController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * 测试 OCR 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkOcrHealth() {
        Map<String, Object> response = new HashMap<>();
        boolean available = ocrService.isOcrServiceAvailable();
        
        response.put("success", available);
        response.put("message", available ? "OCR 服务可用" : "OCR 服务不可用");
        response.put("flaskBaseUrl", "配置在环境变量中");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 测试 OCR 识别（直接调用 Service，不经过 Agent）
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testOcrRecognition(
            @RequestParam("file") MultipartFile file) {
        
        logger.info("收到 OCR 测试请求，文件名：{}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            byte[] imageBytes = file.getBytes();
            logger.info("图片大小：{} bytes", imageBytes.length);
            
            Map<String, Object> result = ocrService.recognizeDrugImage(imageBytes);
            
            response.put("success", true);
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("OCR 识别失败", e);
            response.put("success", false);
            response.put("message", "OCR 识别失败：" + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
