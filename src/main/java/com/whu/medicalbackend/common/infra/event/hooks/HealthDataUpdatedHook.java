package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class HealthDataUpdatedHook implements DomainEventHook {

    @Override
    public String hookName() {
        return "healthDataUpdated";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return "health.data.updated".equals(event.getEventType());
    }

    @Override
    public void handle(DomainEvent event) {
        log.info("HealthDataUpdatedHook: 健康数据已更新, eventId={}, userId={}, groupId={}",
                event.getEventId(), event.getUserId(), event.getGroupId());
    }
}
