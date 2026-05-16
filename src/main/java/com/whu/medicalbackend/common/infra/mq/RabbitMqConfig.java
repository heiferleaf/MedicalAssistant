package com.whu.medicalbackend.common.infra.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange domainExchange() {
        return new TopicExchange(MqNames.EXCHANGE_DOMAIN, true, false);
    }

    @Bean
    public TopicExchange delayExchange() {
        return new TopicExchange(MqNames.EXCHANGE_DELAY, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(MqNames.EXCHANGE_DEAD_LETTER, true, false);
    }

    @Bean
    public Queue domainEventsQueue() {
        return durableQueue(MqNames.QUEUE_DOMAIN_EVENTS);
    }

    @Bean
    public Queue wsPushQueue() {
        return durableQueue(MqNames.QUEUE_WS_PUSH);
    }

    @Bean
    public Queue aiTaskQueue() {
        return durableQueue(MqNames.QUEUE_AI_TASK);
    }

    @Bean
    public Queue cacheInvalidateQueue() {
        return durableQueue(MqNames.QUEUE_CACHE_INVALIDATE);
    }

    @Bean
    public Queue delayMedicationRemindQueue() {
        return durableQueue(MqNames.QUEUE_DELAY_MEDICATION_REMIND);
    }

    @Bean
    public Queue delayMedicationMissedQueue() {
        return durableQueue(MqNames.QUEUE_DELAY_MEDICATION_MISSED);
    }

    @Bean
    public Queue delayFamilyInviteQueue() {
        return durableQueue(MqNames.QUEUE_DELAY_FAMILY_INVITE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MqNames.QUEUE_DEAD_LETTER).build();
    }

    @Bean
    public Binding medicationEventsBinding(
            @Qualifier("domainEventsQueue") Queue domainEventsQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(domainEventsQueue).to(domainExchange).with(MqNames.ROUTING_MEDICATION_EVENTS);
    }

    @Bean
    public Binding familyEventsBinding(
            @Qualifier("domainEventsQueue") Queue domainEventsQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(domainEventsQueue).to(domainExchange).with(MqNames.ROUTING_FAMILY_EVENTS);
    }

    @Bean
    public Binding healthEventsBinding(
            @Qualifier("domainEventsQueue") Queue domainEventsQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(domainEventsQueue).to(domainExchange).with(MqNames.ROUTING_HEALTH_EVENTS);
    }

    @Bean
    public Binding agentEventsBinding(
            @Qualifier("domainEventsQueue") Queue domainEventsQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(domainEventsQueue).to(domainExchange).with(MqNames.ROUTING_AGENT_EVENTS);
    }

    @Bean
    public Binding wsPushBinding(
            @Qualifier("wsPushQueue") Queue wsPushQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(wsPushQueue).to(domainExchange).with(MqNames.ROUTING_WS_PUSH);
    }

    @Bean
    public Binding aiTaskBinding(
            @Qualifier("aiTaskQueue") Queue aiTaskQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(aiTaskQueue).to(domainExchange).with(MqNames.ROUTING_AI_TASK);
    }

    @Bean
    public Binding cacheInvalidateBinding(
            @Qualifier("cacheInvalidateQueue") Queue cacheInvalidateQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(cacheInvalidateQueue).to(domainExchange).with(MqNames.ROUTING_CACHE_INVALIDATE);
    }

    @Bean
    public Binding delayMedicationRemindBinding(
            @Qualifier("delayMedicationRemindQueue") Queue delayMedicationRemindQueue,
            @Qualifier("delayExchange") TopicExchange delayExchange
    ) {
        return BindingBuilder.bind(delayMedicationRemindQueue).to(delayExchange).with(MqNames.ROUTING_DELAY_MEDICATION_REMIND);
    }

    @Bean
    public Binding delayMedicationMissedBinding(
            @Qualifier("delayMedicationMissedQueue") Queue delayMedicationMissedQueue,
            @Qualifier("delayExchange") TopicExchange delayExchange
    ) {
        return BindingBuilder.bind(delayMedicationMissedQueue).to(delayExchange).with(MqNames.ROUTING_DELAY_MEDICATION_MISSED);
    }

    @Bean
    public Binding delayFamilyInviteBinding(
            @Qualifier("delayFamilyInviteQueue") Queue delayFamilyInviteQueue,
            @Qualifier("delayExchange") TopicExchange delayExchange
    ) {
        return BindingBuilder.bind(delayFamilyInviteQueue).to(delayExchange).with(MqNames.ROUTING_DELAY_FAMILY_INVITE);
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange
    ) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(MqNames.ROUTING_DEAD_LETTER);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息未到达 Broker: correlationData={}, cause={}", correlationData, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("消息路由失败: exchange={}, routingKey={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });

        return rabbitTemplate;
    }

    private Queue durableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(MqNames.EXCHANGE_DEAD_LETTER)
                .deadLetterRoutingKey(queueName + ".failed")
                .build();
    }
}
