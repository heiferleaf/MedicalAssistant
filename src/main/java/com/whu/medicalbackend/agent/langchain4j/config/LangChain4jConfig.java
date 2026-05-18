package com.whu.medicalbackend.agent.langchain4j.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class LangChain4jConfig {

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${dashscope.model:qwen-plus}")
    private String dashscopeModel;

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("!'${dashscope.api-key:}'.isBlank()")
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModel)
                .enableSearch(true)
                .build();
    }

}
