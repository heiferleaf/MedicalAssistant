package com.whu.medicalbackend.ws.event;

import com.whu.medicalbackend.enumField.FamilyEventEnum;

import java.util.Map;

public class FamilyMedicineUpdateEvent extends FamilyPushEvent{
    public FamilyMedicineUpdateEvent(Object source, Long groupId, Map<String, Object> data) {
        super(source, FamilyEventEnum.MEDICINE_UPDATE, groupId, data);
    }
}
