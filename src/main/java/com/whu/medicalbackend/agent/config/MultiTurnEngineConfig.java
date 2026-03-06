package com.whu.medicalbackend.agent.config;

import com.whu.medicalbackend.agent.engine.MultiTurnExecutionEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 多轮执行引擎配置
 */
@Configuration
public class MultiTurnEngineConfig {
    
    @Value("${agent.multi-turn.max-turns:5}")
    private int maxTurns;
    
    @Value("${agent.multi-turn.timeout-ms:30000}")
    private long timeoutMs;
    
    @Value("${agent.multi-turn.retry-enabled:true}")
    private boolean retryEnabled;
    
    @Value("${agent.multi-turn.max-retries:2}")
    private int maxRetries;
    
    private final MultiTurnExecutionEngine engine;
    
    public MultiTurnEngineConfig(MultiTurnExecutionEngine engine) {
        this.engine = engine;
    }
    
    /**
     * 配置多轮执行引擎
     * 注意：引擎的其他依赖（DashScopeApiService 等）会通过构造函数自动注入
     */
    @PostConstruct
    public void configure() {
        engine.setMaxTurns(maxTurns);
        engine.setTimeoutMs(timeoutMs);
        engine.setEnableRetry(retryEnabled);
        engine.setMaxRetries(maxRetries);
        
        // 可以在这里添加更多自定义配置
        // 例如：注册自定义工具处理器
        // engine.registerTool("custom_tool", customHandler);
    }
}
