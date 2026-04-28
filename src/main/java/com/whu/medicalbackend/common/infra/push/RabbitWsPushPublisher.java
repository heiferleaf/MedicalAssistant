package com.whu.medicalbackend.common.infra.push;

import com.whu.medicalbackend.common.infra.mq.MqNames;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RabbitWsPushPublisher implements WsPushPublisher {

    public static final String ROUTING_KEY_PUSH_USER = "ws.push.user";

    private final RabbitTemplate rabbitTemplate;

    public RabbitWsPushPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void pushToUser(WsPushCommand command) {
        Assert.notNull(command, "ws push command must not be null");
        Assert.notNull(command.getUserId(), "ws push userId must not be null");

        command.ensureMetadata();
        rabbitTemplate.convertAndSend(MqNames.EXCHANGE_DOMAIN, ROUTING_KEY_PUSH_USER, command);
    }
}
