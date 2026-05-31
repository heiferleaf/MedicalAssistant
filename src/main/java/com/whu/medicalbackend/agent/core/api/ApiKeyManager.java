package com.whu.medicalbackend.agent.core.api;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多 API Key 管理器 — 从环境变量读取多个 Key，按轮询（Round-Robin）分配，
 * 避免所有请求集中在一个 API Key 上触发限流。
 *
 * <p>配置方式（优先级）：</p>
 * <ol>
 *   <li><b>DASHSCOPE_API_KEYS</b> — 逗号分隔的 API Key 列表，如 {@code sk-xxx1,sk-xxx2,sk-xxx3}</li>
 *   <li><b>DASHSCOPE_API_KEY</b> — 单 Key 向后兼容</li>
 * </ol>
 */
@Component
public class ApiKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyManager.class);

    private final String apiKeysCsv;
    private final String singleApiKey;

    private final List<String> keys = new ArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    public ApiKeyManager(
            @Value("${dashscope.api-keys:}") String apiKeysCsv,
            @Value("${dashscope.api-key:}") String singleApiKey) {
        this.apiKeysCsv = apiKeysCsv;
        this.singleApiKey = singleApiKey;
    }

    @PostConstruct
    public void init() {
        // 优先使用 DASHSCOPE_API_KEYS（逗号分隔）
        if (apiKeysCsv != null && !apiKeysCsv.isBlank()) {
            String[] parts = apiKeysCsv.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isBlank()) {
                    keys.add(trimmed);
                }
            }
        }

        // 回退到单 key
        if (keys.isEmpty() && singleApiKey != null && !singleApiKey.isBlank()) {
            keys.add(singleApiKey.trim());
        }

        if (keys.isEmpty()) {
            logger.warn("未配置任何 DASHSCOPE_API_KEY / DASHSCOPE_API_KEYS，LLM 功能将不可用");
        } else {
            logger.info("ApiKeyManager 初始化完成，共 {} 个 API Key：{}",
                    keys.size(), keys.size() == 1 ? "单 Key 模式" : "多 Key 轮询模式");
            if (keys.size() > 1) {
                logger.debug("API Keys: {}", String.join(", ", keys.stream()
                        .map(k -> k.substring(0, Math.min(8, k.length())) + "****")
                        .toList()));
            }
        }
    }

    /**
     * 轮询获取下一个 API Key（线程安全）
     */
    public String getNextKey() {
        if (keys.isEmpty()) {
            return null;
        }
        int index = Math.abs(counter.getAndIncrement()) % keys.size();
        return keys.get(index);
    }

    /**
     * 获取所有注册的 API Key（只读）
     */
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(keys);
    }

    /**
     * 获取注册的 Key 数量
     */
    public int getKeyCount() {
        return keys.size();
    }

    /**
     * 是否有可用的 API Key
     */
    public boolean hasKeys() {
        return !keys.isEmpty();
    }
}
