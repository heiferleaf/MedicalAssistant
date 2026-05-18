package com.whu.medicalbackend.agent.controller;

import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.common.response.ResultCode;
import com.whu.medicalbackend.agent.AgentOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RestController
@RequestMapping("/api/agent")
public class AgentProxyController {

    private static final Logger logger = LoggerFactory.getLogger(AgentProxyController.class);

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @Autowired
    @Qualifier("aiExecutor")
    private ThreadPoolTaskExecutor aiExecutor;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        logger.info("收到 chat 请求，payload: {}", payload);
        try {
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
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStreamGet(
            @RequestParam("user_id") String userId,
            @RequestParam("session_id") String sessionId,
            @RequestParam("message") String message) {
        logger.info("====== 收到 chatStream GET 请求 ======");
        logger.info("userId={}, sessionId={}, message 长度={}", userId, sessionId, message != null ? message.length() : 0);
        logger.info("===================================");

        return handleChatStream(userId, sessionId, message);
    }

    /**
     * SSE 流式聊天接口 - POST 方式（处理 Base64 图片等大消息）
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8", consumes = "application/x-www-form-urlencoded")
    public SseEmitter chatStreamPost(
            @RequestParam("user_id") String userId,
            @RequestParam("session_id") String sessionId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "token", required = false) String token) {
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

        return handleChatStream(userId, sessionId, message);
    }

    /**
     * 处理流式聊天的通用方法
     * 使用 aiExecutor 线程池替代 ForkJoinPool.commonPool()
     */
    private SseEmitter handleChatStream(String userId, String sessionId, String message) {
        // 创建 SSE Emitter，超时 5 分钟
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 使用 aiExecutor 异步执行（避免占用 Tomcat 线程池和 ForkJoinPool）
        CompletableFuture.runAsync(() -> {
            try {
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
        }, aiExecutor);

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
