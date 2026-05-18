package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class MedicationUpdatedHook implements DomainEventHook {

    @Override
    public String hookName() {
        return "medicationUpdated";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return "medication.updated".equals(event.getEventType());
    }

    @Override
    public void handle(DomainEvent event) {
        log.info("MedicationUpdatedHook: 任务状态已更新, eventId={}, aggregateId={}, payload={}",
                event.getEventId(), event.getAggregateId(), event.getPayload());
    }
}
