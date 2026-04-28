package com.whu.medicalbackend.common.infra.event;

import com.whu.medicalbackend.common.infra.mq.MqNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        Assert.notNull(event, "domain event must not be null");
        Assert.hasText(event.getEventType(), "domain event type must not be blank");

        event.ensureMetadata();
        rabbitTemplate.convertAndSend(MqNames.EXCHANGE_DOMAIN, event.getEventType(), event);
        log.debug("领域事件已发布: eventType={}, eventId={}", event.getEventType(), event.getEventId());
    }
}
