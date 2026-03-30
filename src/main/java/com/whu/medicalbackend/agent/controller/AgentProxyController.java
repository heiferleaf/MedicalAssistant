package com.whu.medicalbackend.agent.controller;

import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.common.response.ResultCode;
import com.whu.medicalbackend.agent.AgentOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/agent")
public class AgentProxyController {

    private static final Logger logger = LoggerFactory.getLogger(AgentProxyController.class);

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        logger.info("收到 chat 请求，payload: {}", payload);
        try {
            // 使用 String.valueOf() 处理可能的 Integer 类型
            String userId = String.valueOf(payload.get("user_id"));
            String sessionId = String.valueOf(payload.get("session_id"));
            String message = String.valueOf(payload.get("message"));
            logger.info("chat 请求详情：userId={}, sessionId={}, message 长度={}", userId, sessionId, message != null ? message.length() : 0);
            
            Map<String, Object> resp = agentOrchestratorService.chat(payload);
            logger.info("chat 响应：success={}", resp.get("success"));
            return Result.success(resp);
        } catch (Exception e) {
            logger.error("chat 处理失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "Agent chat 处理失败：" + e.getMessage());
        }
    }

    @GetMapping({ "/health", "/health/" })
    public Result<Map<String, Object>> health() {
        try {
            Map<String, Object> resp = agentOrchestratorService.health();
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "Agent health 获取失败：" + e.getMessage());
        }
    }

    /**
     * SSE 流式聊天接口
     * 注意：由于 EventSource 只支持 GET 请求，所以这里使用 GET
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(
            @RequestParam("user_id") String userId,
            @RequestParam("session_id") String sessionId,
            @RequestParam("message") String message) {
        logger.info("====== 收到 chatStream 请求 ======");
        logger.info("userId={}, sessionId={}, message 长度={}", userId, sessionId, message != null ? message.length() : 0);
        logger.info("===================================");
        
        // 创建 SSE Emitter，设置超时时间为 5 分钟
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        
        // 异步处理请求
        CompletableFuture.runAsync(() -> {
            try {
                // 调用服务层处理，通过回调返回流式数据
                agentOrchestratorService.chatStream(userId, sessionId, message, emitter);
                
                // 完成时发送结束标记
                emitter.send(SseEmitter.event()
                    .name("end")
                    .data("[DONE]"));
                emitter.complete();
                logger.info("chatStream 完成");
                
            } catch (Exception e) {
                logger.error("chatStream 处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                }
            }
        });
        
        // 设置完成回调，关闭连接
        emitter.onCompletion(() -> {
            logger.info("SSE 连接正常关闭");
        });
        
        emitter.onTimeout(() -> {
            logger.warn("SSE 连接超时");
            emitter.completeWithError(new RuntimeException("SSE timeout"));
        });
        
        emitter.onError(throwable -> {
            logger.error("SSE 连接异常", throwable);
        });
        
        return emitter;
    }
}
