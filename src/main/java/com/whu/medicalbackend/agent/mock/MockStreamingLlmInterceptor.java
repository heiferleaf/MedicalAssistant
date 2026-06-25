package com.whu.medicalbackend.agent.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intercepts streaming LLM calls (StreamingChatModel) when agent.mock.enabled=true.
 * Covers: AgentOrchestratorService SSE handler.
 * @Primary ensures this bean wins over the real QwenStreamingChatModel bean.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "agent.mock", name = "enabled", havingValue = "true")
public class MockStreamingLlmInterceptor implements StreamingChatModel {

    private static final Logger logger = LoggerFactory.getLogger(MockStreamingLlmInterceptor.class);

    @Value("${agent.mock.response-delay-ms:0}")
    private int responseDelayMs;

    @Override
    public void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        MockLlmResponses.simulateLatency(responseDelayMs);
        String mockResponse = MockLlmResponses.random();
        logger.debug("[MockLLM] streaming chat intercepted (delay={}ms)", responseDelayMs);
        // emit two chunks to exercise the SSE assembly path
        int mid = mockResponse.length() / 2;
        handler.onPartialResponse(mockResponse.substring(0, mid));
        handler.onPartialResponse(mockResponse.substring(mid));
        handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(mockResponse))
                .build());
    }
}
