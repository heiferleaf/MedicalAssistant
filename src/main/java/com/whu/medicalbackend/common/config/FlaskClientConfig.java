package com.whu.medicalbackend.common.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class FlaskClientConfig {

    @Bean
    public RestClient flaskRestClient(
            @Value("${flask.base-url:http://8.148.94.242:8001}") String baseUrl,
            @Value("${flask.client.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${flask.client.read-timeout-ms:120000}") int readTimeoutMs,
            @Value("${flask.client.max-connections:200}") int maxConnections,
            @Value("${flask.client.max-connections-per-route:50}") int maxPerRoute,
            @Value("${flask.client.connection-ttl-ms:300000}") int connectionTtlMs,
            @Value("${flask.client.keep-alive-ms:60000}") int keepAliveMs
    ) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .setTimeToLive(connectionTtlMs, TimeUnit.MILLISECONDS)
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxPerRoute)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.of(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .setConnectionRequestTimeout(Timeout.of(connectTimeoutMs, TimeUnit.MILLISECONDS))
                        .build())
                .evictIdleConnections(TimeValue.of(keepAliveMs, TimeUnit.MILLISECONDS))
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
