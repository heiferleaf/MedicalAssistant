package com.whu.medicalbackend.agent.langchain4j.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${dashscope.model:qwen-plus}")
    private String dashscopeModel;

    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModel)
                .build();
    }
}
