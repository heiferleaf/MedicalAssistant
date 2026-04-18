package com.whu.medicalbackend.common.enumField;

public enum FamilyEventEnum{
    MEDICINE_ALARM("medicine_alarm"),
    MEDICINE_UPDATE("medicine_update"),
    MEMBER_UPDATE("member_update"),
    HEALTH_DATA_UPDATE("health_data_update");

    private String eventName;
    FamilyEventEnum(String eventName){
        this.eventName = eventName;
    }
}
