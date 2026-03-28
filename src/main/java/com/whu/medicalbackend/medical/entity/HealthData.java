package com.whu.medicalbackend.medical.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HealthData implements Serializable{
    private static final long serialVersionUID = 8L;

    private Long id;
    private Long userId;
    private Long groupId;
    private Double heartRate;      // 平均心率
    private Double bloodPressure;   // 平均血压
    private LocalDateTime measureTime; // 测量时间
    private LocalDateTime createdAt;
    private Integer isDeleted;
}