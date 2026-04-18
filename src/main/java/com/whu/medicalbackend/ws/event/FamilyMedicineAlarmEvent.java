package com.whu.medicalbackend.ws.event;

import com.whu.medicalbackend.common.enumField.FamilyEventEnum;

import java.util.Map;

public class FamilyMedicineAlarmEvent extends FamilyPushEvent {
    public FamilyMedicineAlarmEvent(Object source, Long groupId, Map<String, Object> data) {
        super(source, FamilyEventEnum.MEDICINE_ALARM, groupId, data);
    }
}
