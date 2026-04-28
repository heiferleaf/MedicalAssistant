package com.whu.medicalbackend.common.infra.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
