package com.whu.medicalbackend.agent;

import com.whu.medicalbackend.agent.langchain4j.agents.MedicalAgent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LlmChatDelegate {

    private static final Logger log = LoggerFactory.getLogger(LlmChatDelegate.class);

    @Retry(name = "llmChat")
    @RateLimiter(name = "llmChat", fallbackMethod = "chatFallback")
    @CircuitBreaker(name = "llmChat", fallbackMethod = "chatFallback")
    public ChatResponse callChat(ChatModel chatModel, SystemMessage systemMessage, UserMessage userMessage) {
        return chatModel.chat(systemMessage, userMessage);
    }

    public ChatResponse chatFallback(ChatModel chatModel, SystemMessage systemMessage, UserMessage userMessage, Throwable t) {
        log.warn("LLM Chat fallback: {}", t.getMessage());
        return null;
    }

    @Retry(name = "llmChat")
    @RateLimiter(name = "llmChat", fallbackMethod = "medicalAgentFallback")
    @CircuitBreaker(name = "llmChat", fallbackMethod = "medicalAgentFallback")
    public String callMedicalAgentChat(MedicalAgent medicalAgent, String sessionId, String userId, String message) {
        return medicalAgent.chat(sessionId, userId, message);
    }

    public String medicalAgentFallback(MedicalAgent medicalAgent, String sessionId, String userId, String message, Throwable t) {
        log.warn("MedicalAgent chat fallback: {}", t.getMessage());
        return null;
    }
}
