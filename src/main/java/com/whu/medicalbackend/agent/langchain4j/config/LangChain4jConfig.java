package com.whu.medicalbackend.agent.langchain4j.config;

import com.whu.medicalbackend.agent.core.api.ApiKeyManager;
import com.whu.medicalbackend.agent.core.api.RotatingChatModel;
import com.whu.medicalbackend.agent.core.api.RotatingStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class LangChain4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${dashscope.model:qwen-plus}")
    private String dashscopeModel;

    /**
     * 创建支持多 API Key 轮询的 ChatModel。
     * 如果配置了多个 Key（DASHSCOPE_API_KEYS），则为每个 Key 创建一个 QwenChatModel
     * 并包装在 RotatingChatModel 中实现 Round-Robin 分发；
     * 如果只有一个 Key，则退化到单模型，零额外开销。
     */
    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("!'${dashscope.api-key:}'.isBlank() || !'${dashscope.api-keys:}'.isBlank()")
    public ChatModel chatModel(ApiKeyManager apiKeyManager) {
        List<String> keys = apiKeyManager.getAllKeys();
        if (keys.isEmpty()) {
            logger.warn("ApiKeyManager 无可用 Key，不创建 ChatModel");
            return null;
        }

        if (keys.size() == 1) {
            logger.info("单 Key 模式：创建 QwenChatModel");
            return QwenChatModel.builder()
                    .apiKey(keys.getFirst())
                    .modelName(dashscopeModel)
                    .enableSearch(true)
                    .build();
        }

        // 多 Key：每个 Key 创建一个模型，包装成 RotatingChatModel
        List<ChatModel> models = new ArrayList<>();
        for (String key : keys) {
            models.add(QwenChatModel.builder()
                    .apiKey(key)
                    .modelName(dashscopeModel)
                    .enableSearch(true)
                    .build());
        }
        logger.info("多 Key 轮询模式：创建 {} 个 QwenChatModel 实例", models.size());
        return new RotatingChatModel(models);
    }

    /**
     * 创建支持多 API Key 轮询的 StreamingChatModel。
     * 策略同上：多 Key 时每个 Key 创建一个 StreamingChatModel 并轮询分发。
     */
    @Bean
    @ConditionalOnMissingBean(StreamingChatModel.class)
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("!'${dashscope.api-key:}'.isBlank() || !'${dashscope.api-keys:}'.isBlank()")
    public StreamingChatModel streamingChatModel(ApiKeyManager apiKeyManager) {
        List<String> keys = apiKeyManager.getAllKeys();
        if (keys.isEmpty()) {
            logger.warn("ApiKeyManager 无可用 Key，不创建 StreamingChatModel");
            return null;
        }

        if (keys.size() == 1) {
            logger.info("单 Key 模式：创建 QwenStreamingChatModel");
            return QwenStreamingChatModel.builder()
                    .apiKey(keys.getFirst())
                    .modelName(dashscopeModel)
                    .build();
        }

        List<StreamingChatModel> models = new ArrayList<>();
        for (String key : keys) {
            models.add(QwenStreamingChatModel.builder()
                    .apiKey(key)
                    .modelName(dashscopeModel)
                    .build());
        }
        logger.info("多 Key 轮询模式：创建 {} 个 QwenStreamingChatModel 实例", models.size());
        return new RotatingStreamingChatModel(models);
    }
}
