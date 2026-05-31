package com.whu.medicalbackend.agent.core.api;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支持多 API Key 轮询的 StreamingChatModel 包装器。
 * 在多个底层 StreamingChatModel 实例之间按 Round-Robin 分发请求，分散 API Key 压力。
 */
public class RotatingStreamingChatModel implements StreamingChatModel {

    private final List<StreamingChatModel> delegates;
    private final AtomicInteger counter = new AtomicInteger(0);

    public RotatingStreamingChatModel(List<StreamingChatModel> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("delegates list must not be empty");
        }
        this.delegates = delegates;
    }

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        StreamingChatModel delegate = delegates.get(
                Math.abs(counter.getAndIncrement()) % delegates.size());
        delegate.chat(request, handler);
    }
}
