package com.whu.medicalbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    public OcrService(@Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.flaskBaseUrl = flaskBaseUrl;
        logger.info("OcrService initialized, Flask base URL: {}", flaskBaseUrl);
    }

    /**
     * 调用 Flask OCR 接口识别药物图片
     * @param imageBytes 图片字节数组
     * @return OCR 识别结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> recognizeDrugImage(byte[] imageBytes) {
        logger.info("调用 Flask OCR 接口识别药物图片");

        try {
            // 创建 ByteArrayResource
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "drug_image.jpg";
                }
            };

            // 构建 multipart 请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 构建完整请求
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                flaskBaseUrl + "/ocr/predict",
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                
                if ("success".equals(status)) {
                    logger.info("OCR 识别成功");
                    return responseBody;
                } else {
                    String message = (String) responseBody.get("message");
                    logger.error("OCR 识别失败：{}", message);
                    throw new RuntimeException("OCR 识别失败：" + message);
                }
            } else {
                throw new RuntimeException("OCR 请求失败，状态码：" + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("OCR 识别失败", e);
            throw new RuntimeException("OCR 识别失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检查 Flask OCR 服务是否可用
     */
    public boolean isOcrServiceAvailable() {
        try {
            String response = restTemplate.getForObject(
                flaskBaseUrl + "/ocr/health",
                String.class
            );
            
            return response != null && response.contains("\"status\": \"ok\"");
        } catch (Exception e) {
            logger.error("检查 OCR 服务状态失败", e);
            return false;
        }
    }
}
