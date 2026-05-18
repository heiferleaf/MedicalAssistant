package com.whu.medicalbackend.common.config;

import com.whu.medicalbackend.common.interceptor.AuthInterceptor;
import com.whu.medicalbackend.ws.WsHandler;
import com.whu.medicalbackend.ws.WsHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer{
    @Autowired
    private AuthInterceptor authInterceptor;

    private final ObjectProvider<WsHandler> wsHandlerProvider;
    private final ObjectProvider<WsHandshakeInterceptor> wsHandshakeInterceptorProvider;
    private final boolean websocketEnabled;

    @Autowired
    public WebConfig(
            ObjectProvider<WsHandler> wsHandlerProvider,
            ObjectProvider<WsHandshakeInterceptor> wsHandshakeInterceptorProvider,
            @Value("${service.websocket.enabled:true}") boolean websocketEnabled
    ) {
        this.wsHandlerProvider = wsHandlerProvider;
        this.wsHandshakeInterceptorProvider = wsHandshakeInterceptorProvider;
        this.websocketEnabled = websocketEnabled;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/register",
                    "/api/user/refresh",
                    "/api/user/avatar/random",
                    "/api/agent/health",
                    "/api/agent/chat",
                    "/api/agent/chat/stream",  // 添加流式接口免认证（用于测试）
                    "/api/agent/confirm",
                    "/api/agent/sessions/**",
                    "/api/agent/tool-execution/**",
                    "/api/agent/chat/async",
                    "/api/agent/task/**",
                    "/api/predict/**",
                    "/api/rag/**",
                    "/api/ocr/**",
                    "/test/**",
                    "/api/medical/prepare/pdf/file/**"  // PDF 下载接口，允许匿名访问
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (!websocketEnabled) {
            return;
        }

        WsHandler wsHandler = wsHandlerProvider.getIfAvailable();
        WsHandshakeInterceptor wsHandshakeInterceptor = wsHandshakeInterceptorProvider.getIfAvailable();
        if (wsHandler == null || wsHandshakeInterceptor == null) {
            return;
        }

        registry.addHandler(wsHandler, "/ws")
                .addInterceptors(wsHandshakeInterceptor)
                // 配置允许跨域
                .setAllowedOrigins("*");
    }
}
