package com.whu.medicalbackend.common.infra.delay.handler;

import com.whu.medicalbackend.common.infra.delay.DelayTask;
import com.whu.medicalbackend.common.infra.delay.DelayTaskHandler;
import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.DomainEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class MedicationRemindHandler implements DelayTaskHandler {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DomainEventPublisher domainEventPublisher;

    public MedicationRemindHandler(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public boolean supports(String taskType) {
        return "medication.remind".equals(taskType);
    }

    @Override
    public void handle(DelayTask task) {
        Long userId = toLong(task.getPayload().get("userId"));
        String medicineName = (String) task.getPayload().get("medicineName");

        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "medicine_remind");
        pushData.put("medicineName", medicineName);
        pushData.put("remindTime", LocalDateTime.now().format(formatter));

        DomainEvent remindEvent = DomainEvent.of("medication.remind", "MedicationTask", task.getBizId());
        remindEvent.setUserId(userId);
        remindEvent.setPayload(pushData);
        domainEventPublisher.publish(remindEvent);
    }

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }
}
