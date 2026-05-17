package com.whu.medicalbackend.common.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FlaskClientConfig {

    @Bean
    public RestClient flaskRestClient(
            @Value("${flask.base-url:http://127.0.0.1:8001}") String baseUrl,
            @Value("${ai.http.connect-timeout-ms:${flask.timeout-ms:120000}}") int connectTimeoutMs,
            @Value("${ai.http.read-timeout-ms:${flask.timeout-ms:120000}}") int readTimeoutMs,
            @Value("${ai.http.connection-request-timeout-ms:1000}") int connectionRequestTimeoutMs,
            @Value("${ai.http.max-connections:200}") int maxConnections,
            @Value("${ai.http.max-connections-per-route:50}") int maxConnectionsPerRoute
    ) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeoutMs))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
