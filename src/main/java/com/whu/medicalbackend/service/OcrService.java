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
                    
                    // 确保返回的 Map 中的字符串是标准 JSON 格式
                    Map<String, Object> cleanResponse = new java.util.LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : responseBody.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            String strValue = (String) value;
                            // 将 Python 字典字符串转换为标准 JSON 字符串
                            if (strValue.startsWith("{") || strValue.startsWith("{'")) {
                                // 替换单引号为双引号
                                strValue = strValue.replace("'", "\"");
                                value = strValue;
                            }
                        }
                        cleanResponse.put(entry.getKey(), value);
                    }
                    
                    return cleanResponse;
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
            // 直接检查 /ocr/predict 接口是否可访问（使用 GET 方法）
            // 即使返回 405 Method Not Allowed，也说明服务是活的
            restTemplate.getForObject(
                flaskBaseUrl + "/ocr/predict",
                String.class
            );
            // 如果 GET 请求成功（虽然不太可能），返回 true
            return true;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 4xx/5xx 错误（如 405 Method Not Allowed）说明服务存在，只是方法不对
            logger.info("OCR 服务可访问（返回 HTTP {}），认为服务可用", e.getStatusCode());
            return true;
        } catch (Exception e) {
            logger.error("检查 OCR 服务状态失败", e);
            return false;
        }
    }
}
