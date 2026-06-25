package com.whu.medicalbackend.agent.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intercepts sync LLM calls (ChatModel) when agent.mock.enabled=true.
 * Covers: AgentOrchestratorService.handleSimpleLlmChat() and MedicalAgent via AiServices.
 * @Primary ensures this bean wins over the real QwenChatModel bean.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "agent.mock", name = "enabled", havingValue = "true")
public class MockLlmInterceptor implements ChatModel {

    private static final Logger logger = LoggerFactory.getLogger(MockLlmInterceptor.class);

    @Value("${agent.mock.response-delay-ms:0}")
    private int responseDelayMs;

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        MockLlmResponses.simulateLatency(responseDelayMs);
        String mockResponse = MockLlmResponses.random();
        logger.debug("[MockLLM] sync chat intercepted (delay={}ms)", responseDelayMs);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(mockResponse))
                .build();
    }
}
