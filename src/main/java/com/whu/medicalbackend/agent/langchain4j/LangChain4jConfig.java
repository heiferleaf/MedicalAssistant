package com.whu.medicalbackend.agent.langchain4j;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 适配 LangChain4j 1.1.0 版本
 */
@Configuration
public class LangChain4jConfig {

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${dashscope.model:qwen-plus}")
    private String dashscopeModel;

    /**
     * 配置 ChatModel
     * 使用阿里云的 DashScope 模型（千问）
     */
    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModel)
                .build();
    }
}
