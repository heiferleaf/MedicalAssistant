package com.whu.medicalbackend.common.infra.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.infra.hook.DomainEventHookExecutor;
import com.whu.medicalbackend.common.infra.mq.MqNames;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "infra.domain-event-consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventMessageListener {

    private final DomainEventHookExecutor hookExecutor;
    private final ObjectMapper objectMapper;

    public DomainEventMessageListener(DomainEventHookExecutor hookExecutor, ObjectMapper objectMapper) {
        this.hookExecutor = hookExecutor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MqNames.QUEUE_DOMAIN_EVENTS)
    public void onDomainEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            DomainEvent event = objectMapper.readValue(message.getBody(), DomainEvent.class);
            if (event == null) {
                log.warn("收到空领域事件，已忽略");
                channel.basicAck(deliveryTag, false);
                return;
            }
            event.ensureMetadata();
            hookExecutor.execute(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("领域事件处理失败, deliveryTag={}", deliveryTag, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
