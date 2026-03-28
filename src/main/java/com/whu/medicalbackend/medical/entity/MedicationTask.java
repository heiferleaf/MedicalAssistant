package com.whu.medicalbackend.medical.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationTask implements Serializable {
    private static final long serialVersionUID = 4L;

    private Long id;
    private Long userId;
    private Long planId;
    private Long medicineId;
    private String dosage;
    private LocalDate taskDate;
    private LocalTime timePoint;  // 注意：数据库是VARCHAR，需要TypeHandler转换
    private Integer status;  // 0-未服用，1-已服用，2-漏服
    private LocalDateTime operateTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
