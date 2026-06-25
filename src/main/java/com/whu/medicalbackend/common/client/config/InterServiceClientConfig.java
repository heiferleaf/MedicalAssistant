package com.whu.medicalbackend.common.client.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 服务间通信配置
 *
 * 配置 RestTemplate 用于微服务间的 HTTP 调用
 *
 * @author Medical Assistant Team
 */
@Configuration
public class InterServiceClientConfig {

    /**
     * 创建用于服务间调用的 RestTemplate
     *
     * 配置：
     * - 连接超时：3 秒
     * - 读取超时：10 秒
     * - 错误处理器：自定义错误处理
     */
    @Bean("interServiceRestTemplate")
    public RestTemplate interServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(this::clientHttpRequestFactory)
                .errorHandler(new InterServiceErrorHandler())
                .build();
    }

    /**
     * 配置 HTTP 请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return factory;
    }
}
