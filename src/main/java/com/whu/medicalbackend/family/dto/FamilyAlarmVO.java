package com.whu.medicalbackend.family.dto;

import lombok.Data;

@Data
public class FamilyAlarmVO {
    private Long alarmId;
    private String memberName;
    private String medicineName;
    private String alarmTime; // 对应 2026-03-20 08:36:10
}
