package com.whu.medicalbackend.agent.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.common.response.ResultCode;
import com.whu.medicalbackend.agent.AgentOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/agent")
public class AgentProxyController {

    private static final Logger logger = LoggerFactory.getLogger(AgentProxyController.class);

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @Autowired
    @Qualifier("aiExecutor")
    private ThreadPoolTaskExecutor aiExecutor;

    @Value("${agent.multi-turn.timeout-ms:30000}")
    private long agentTimeoutMs;

    // Small dedicated pool for SSE heartbeat tasks (one thread per ~100 active streams).
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newScheduledThreadPool(4);


    @PostMapping("/chat")
    @SentinelResource(value = "/api/agent/chat", blockHandler = "handleChatBlock")
    public ResponseEntity<Result<Map<String, Object>>> chat(@RequestBody Map<String, Object> payload) {
        logger.info("收到 chat 请求，payload: {}", payload);

        Future<Map<String, Object>> future = null;
        try {
            future = aiExecutor.submit(() -> agentOrchestratorService.chat(payload));
            Map<String, Object> resp = future.get(agentTimeoutMs, TimeUnit.MILLISECONDS);
            logger.info("chat 响应：success={}", resp.get("success"));
            return ResponseEntity.ok(Result.success(resp));
        } catch (RejectedExecutionException e) {
            logger.warn("Agent chat 线程池队列已满", e);
            return ResponseEntity.status(429).body(Result.error(429, "Agent 服务繁忙，请稍后重试"));
        } catch (TimeoutException e) {
            if (future != null) future.cancel(true);
            logger.warn("Agent chat 超时，timeoutMs={}", agentTimeoutMs, e);
            return ResponseEntity.status(504).body(Result.error(504, "Agent 处理超时，请稍后重试"));
        } catch (Exception e) {
            logger.error("chat 处理失败", e);
            return ResponseEntity.status(500).body(Result.error(ResultCode.SYSTEM_ERROR, "Agent chat 处理失败：" + e.getMessage()));
        }
    }

    public ResponseEntity<Result<Map<String, Object>>> handleChatBlock(Map<String, Object> payload, BlockException ex) {
        logger.warn("chat 请求被 Sentinel 限流：userId={}", payload.get("user_id"));
        return ResponseEntity.status(429).body(Result.error(429, "AI 服务繁忙，请稍后重试"));
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
     * 压测统计计数器接口
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        try {
            Map<String, Object> stats = agentOrchestratorService.getStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "Agent stats 获取失败：" + e.getMessage());
        }
    }

    /**
     * SSE 流式聊天接口
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    @SentinelResource(value = "/api/agent/chat/stream", blockHandler = "handleChatStreamBlock")
    public SseEmitter chatStreamGet(
            @RequestParam("user_id") String userId,
            @RequestParam("session_id") String sessionId,
            @RequestParam("message") String message,
            jakarta.servlet.http.HttpServletResponse response) {
        logger.info("====== 收到 chatStream GET 请求 ======");
        logger.info("userId={}, sessionId={}, message 长度={}", userId, sessionId, message != null ? message.length() : 0);
        logger.info("===================================");

        setSseHeaders(response);
        return handleChatStream(userId, sessionId, message);
    }

    /**
     * SSE 流式聊天接口 - POST 方式（处理 Base64 图片等大消息）
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8", consumes = "application/x-www-form-urlencoded")
    @SentinelResource(value = "/api/agent/chat/stream", blockHandler = "handleChatStreamBlock")
    public SseEmitter chatStreamPost(
            @RequestParam("user_id") String userId,
            @RequestParam("session_id") String sessionId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "token", required = false) String token,
            jakarta.servlet.http.HttpServletResponse response) {
        logger.info("====== 收到 chatStream POST 请求 ======");
        logger.info("userId={}, sessionId={}, message 长度={}, token={}", userId, sessionId, message != null ? message.length() : 0, token != null ? "provided" : "missing");
        logger.info("===================================");

        if (message != null && !message.isEmpty()) {
            try {
                message = java.net.URLDecoder.decode(message, "UTF-8");
                logger.info("解码后 message 长度={}", message.length());
            } catch (Exception e) {
                logger.error("URL 解码失败", e);
            }
        }

        setSseHeaders(response);
        return handleChatStream(userId, sessionId, message);
    }

    /**
     * 设置 SSE 响应头（客户端缓存、Nginx 缓冲、重连间隔）
     */
    private void setSseHeaders(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
    }

    public SseEmitter handleChatStreamBlock(String userId, String sessionId, String message, BlockException ex) {
        logger.warn("chatStream 请求被限流：userId={}", userId);
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data("AI 服务繁忙，请稍后重试"));
            emitter.complete();
        } catch (IOException e) {
            logger.error("发送限流消息失败", e);
        }
        return emitter;
    }

    /**
     * 处理流式聊天的通用方法
     * 使用 aiExecutor 线程池替代 ForkJoinPool.commonPool()
     */
    private SseEmitter handleChatStream(String userId, String sessionId, String message) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // Immediately acknowledge the connection so the client knows the request is queued.
        try {
            emitter.send(SseEmitter.event().name("queued").data("processing"));
        } catch (IOException e) {
            logger.warn("Failed to send queued event", e);
        }

        // Heartbeat every 15 s keeps the connection alive through nginx's proxy_read_timeout
        // and prevents load-balancer idle-connection resets.
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (Exception ignored) {
                // emitter already completed or errored — heartbeat will be cancelled by onCompletion
            }
        }, 15, 15, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            heartbeat.cancel(false);
            logger.info("SSE 连接正常关闭，userId={}", userId);
        });
        emitter.onTimeout(() -> {
            heartbeat.cancel(false);
            logger.warn("SSE 连接超时，userId={}", userId);
            emitter.complete();
        });
        emitter.onError(throwable -> {
            heartbeat.cancel(false);
            logger.error("SSE 连接异常，userId={}", userId, throwable);
        });

        // Run the actual LLM work on the AI thread pool, not the Tomcat request thread.
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                agentOrchestratorService.chatStream(userId, sessionId, message, emitter);
                emitter.complete();
                logger.info("chatStream 完成，userId={}", userId);
            } catch (Exception e) {
                logger.error("chatStream 处理失败，userId={}", userId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                }
            }
        }, aiExecutor);

        return emitter;
    }

    /**
     * 异步聊天任务提交（带请求分级）
     * 立即返回 taskId，客户端轮询 /task/{taskId} 获取结果
     */
    @PostMapping("/chat/async")
    public Result<Map<String, Object>> submitChatTask(@RequestBody Map<String, Object> payload) {
        logger.info("收到异步 chat 请求");
        try {
            var task = agentOrchestratorService.submitChatTask(payload);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("grade", task.getGrade().name());
            result.put("status", task.getStatus().name());
            result.put("timeoutMs", task.getTimeoutMs());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("提交异步任务失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "提交失败：" + e.getMessage());
        }
    }

    /**
     * 查询异步任务状态/结果
     */
    @GetMapping("/task/{taskId}")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            var task = agentOrchestratorService.getTaskStatus(taskId);
            if (task == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "任务不存在或已过期");
            }
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus().name());
            result.put("grade", task.getGrade().name());
            result.put("createdAt", task.getCreatedAt().toString());
            if (task.getCompletedAt() != null) {
                result.put("completedAt", task.getCompletedAt().toString());
            }
            if (task.getStatus() == com.whu.medicalbackend.agent.core.task.AgentTask.Status.SUCCESS) {
                result.put("result", task.getResult());
            }
            if (task.getErrorMessage() != null) {
                result.put("error", task.getErrorMessage());
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "查询失败：" + e.getMessage());
        }
    }
}