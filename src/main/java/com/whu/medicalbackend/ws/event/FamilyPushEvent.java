package com.whu.medicalbackend.ws.event;

import com.whu.medicalbackend.enumField.FamilyEventEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class FamilyPushEvent extends ApplicationEvent {
    final FamilyEventEnum eventType;
    final Long groupId;
    final Map<String, Object> data;

    public FamilyPushEvent(Object source, FamilyEventEnum eventType, Long groupId, Map<String, Object> data) {
        super(source);
        this.eventType = eventType;
        this.groupId = groupId;
        this.data = data;
    }
}
