package com.whu.medicalbackend.common.infra.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.infra.hook.DomainEventHookExecutor;
import com.whu.medicalbackend.common.infra.mq.MqNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DomainEventMessageListener {

    private final DomainEventHookExecutor hookExecutor;
    private final ObjectMapper objectMapper;

    public DomainEventMessageListener(DomainEventHookExecutor hookExecutor, ObjectMapper objectMapper) {
        this.hookExecutor = hookExecutor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MqNames.QUEUE_DOMAIN_EVENTS)
    public void onDomainEvent(Message message) throws Exception {
        DomainEvent event = objectMapper.readValue(message.getBody(), DomainEvent.class);
        if (event == null) {
            log.warn("收到空领域事件，已忽略");
            return;
        }
        event.ensureMetadata();
        hookExecutor.execute(event);
    }
}
