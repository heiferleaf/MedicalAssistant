package com.whu.medicalbackend.config;

import com.whu.medicalbackend.interceptor.AuthInterceptor;
import com.whu.medicalbackend.ws.WsHandler;
import com.whu.medicalbackend.ws.WsHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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

    @Autowired
    private WsHandler wsHandler;

    @Autowired
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/register",
                    "/api/user/refresh",
                    "/api/agent/health",
                    "/api/agent/chat",
                    "/api/agent/chat/stream",  // 添加流式接口免认证（用于测试）
                    "/api/agent/confirm",
                    "/api/agent/sessions/**",
                    "/api/predict/**",
                    "/api/rag/**",
                    "/api/ocr/**",
                    "/test/**"

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
                // 入口映射
        registry.addHandler(wsHandler, "/ws")
                .addInterceptors(wsHandshakeInterceptor)
                // 配置允许跨域
                .setAllowedOrigins("*");
    }
}