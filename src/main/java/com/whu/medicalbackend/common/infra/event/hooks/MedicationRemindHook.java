package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class MedicationRemindHook implements DomainEventHook {

    @Override
    public String hookName() {
        return "medicationRemind";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return "medication.remind".equals(event.getEventType());
    }

    @Override
    public void handle(DomainEvent event) {
        log.info("MedicationRemindHook: 用药提醒已发送, eventId={}, userId={}, payload={}",
                event.getEventId(), event.getUserId(), event.getPayload());
    }
}
