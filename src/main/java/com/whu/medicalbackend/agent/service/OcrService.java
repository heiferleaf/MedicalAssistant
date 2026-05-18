package com.whu.medicalbackend.agent.service;

import com.whu.medicalbackend.agent.flask.UnifiedFlaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final UnifiedFlaskClient unifiedFlaskClient;

    public OcrService(UnifiedFlaskClient unifiedFlaskClient) {
        this.unifiedFlaskClient = unifiedFlaskClient;
        logger.info("OcrService initialized with UnifiedFlaskClient");
    }

    /**
     * 调用 Flask OCR 接口识别药物图片
     */
    public Map<String, Object> recognizeDrugImage(byte[] imageBytes) {
        logger.info("OcrService.recognizeDrugImage, size: {} bytes", imageBytes.length);
        return unifiedFlaskClient.recognizeDrug(imageBytes);
    }

    /**
     * 检查 Flask OCR 服务是否可用
     */
    public boolean isOcrServiceAvailable() {
        return unifiedFlaskClient.isFlaskAvailable();
    }
}
