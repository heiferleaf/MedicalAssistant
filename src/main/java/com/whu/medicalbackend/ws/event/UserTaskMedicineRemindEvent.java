package com.whu.medicalbackend.ws.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class UserTaskMedicineRemindEvent extends ApplicationEvent {
    private Long userId;
    private Map<String, Object> data;

    public UserTaskMedicineRemindEvent(Object source, Long userId, Map<String, Object> data) {
        super(source);
        this.userId = userId;
        this.data = data;
    }
}
