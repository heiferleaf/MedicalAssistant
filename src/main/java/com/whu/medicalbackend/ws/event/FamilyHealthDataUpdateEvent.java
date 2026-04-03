package com.whu.medicalbackend.ws.event;

import com.whu.medicalbackend.common.enumField.FamilyEventEnum;

import java.util.Map;

public class FamilyHealthDataUpdateEvent extends FamilyPushEvent {
    public FamilyHealthDataUpdateEvent(Object source, Long gourpId, Map<String, Object> data) {
        super(source, FamilyEventEnum.HEALTH_DATA_UPDATE, gourpId, data);
    }
}
