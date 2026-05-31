package com.whu.medicalbackend.agent.core.api;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支持多 API Key 轮询的 ChatModel 包装器。
 * 在多个底层 ChatModel 实例之间按 Round-Robin 分发请求，分散 API Key 压力。
 */
public class RotatingChatModel implements ChatModel {

    private final List<ChatModel> delegates;
    private final AtomicInteger counter = new AtomicInteger(0);

    public RotatingChatModel(List<ChatModel> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("delegates list must not be empty");
        }
        this.delegates = delegates;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ChatModel delegate = delegates.get(
                Math.abs(counter.getAndIncrement()) % delegates.size());
        return delegate.chat(request);
    }
}
