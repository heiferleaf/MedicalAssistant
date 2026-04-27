package com.whu.medicalbackend.common.config;

import com.whu.medicalbackend.ws.WsPubSubBroadcaster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 提供 Redis 的Pub/Sub 模式的 Redis 容器配置
 */
@Configuration
public class RedisMessageConfig{

    @Bean
    public RedisMessageListenerContainer wsListenerContainer(
            RedisConnectionFactory factory,
            WsPubSubBroadcaster wsPubSubBroadcaster
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        container.addMessageListener(wsPubSubBroadcaster, new PatternTopic("ws:group:*"));

        return container;
    }

}
