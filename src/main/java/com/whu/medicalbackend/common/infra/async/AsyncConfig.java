package com.whu.medicalbackend.common.infra.async;

import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Value("${infra.async.domain.core-size:8}")
    private int domainCoreSize;

    @Value("${infra.async.domain.max-size:32}")
    private int domainMaxSize;

    @Value("${infra.async.ws-push.core-size:8}")
    private int wsPushCoreSize;

    @Value("${infra.async.ws-push.max-size:32}")
    private int wsPushMaxSize;

    @Value("${infra.async.ai.core-size:8}")
    private int aiCoreSize;

    @Value("${infra.async.ai.max-size:20}")
    private int aiMaxSize;

    @Value("${infra.async.ai.queue-capacity:100}")
    private int aiQueueCapacity;

    @Value("${infra.async.pdf.core-size:2}")
    private int pdfCoreSize;

    @Value("${infra.async.pdf.max-size:8}")
    private int pdfMaxSize;

    @Value("${infra.async.queue-capacity:500}")
    private int queueCapacity;

    @Bean("domainEventExecutor")
    public ThreadPoolTaskExecutor domainEventExecutor() {
        return buildExecutor("domain-event-", domainCoreSize, domainMaxSize, queueCapacity,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("wsPushExecutor")
    public ThreadPoolTaskExecutor wsPushExecutor() {
        return buildExecutor("ws-push-", wsPushCoreSize, wsPushMaxSize, queueCapacity,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean("aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        // AI 任务耗时长，队列过大会放大延迟；满载时快速拒绝，由调用方返回忙碌提示更可控。
        return buildExecutor("ai-", aiCoreSize, aiMaxSize, aiQueueCapacity,
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean("pdfExecutor")
    public ThreadPoolTaskExecutor pdfExecutor() {
        return buildExecutor("pdf-", pdfCoreSize, pdfMaxSize, queueCapacity / 2,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public Executor getAsyncExecutor() {
        return domainEventExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                org.slf4j.LoggerFactory.getLogger(method.getDeclaringClass())
                        .error("异步方法执行失败: {}", method.getName(), ex);
    }

    private ThreadPoolTaskExecutor buildExecutor(
            String threadNamePrefix,
            int coreSize,
            int maxSize,
            int capacity,
            RejectedExecutionHandler rejectedExecutionHandler
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(capacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
