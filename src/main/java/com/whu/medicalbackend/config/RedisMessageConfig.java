package com.whu.medicalbackend.config;

import com.whu.medicalbackend.ws.WsPubSubBroadcaster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

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

        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(wsPubSubBroadcaster, "onRedisMessage");
        container.addMessageListener(messageListenerAdapter, new PatternTopic("ws:group:*"));

        return container;
    }

}
