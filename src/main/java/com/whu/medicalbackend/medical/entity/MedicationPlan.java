package com.whu.medicalbackend.medical.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationPlan implements Serializable {
    private static final long serialVersionUID = 3L;

    private Long            id;
    private Long            userId;
    private Long            medicineId;
    private String          dosage;
    private LocalDate       startDate;
    private LocalDate       endDate;
    private List<LocalTime> timePoints;  // 注意：数据库是JSON字符串，需要TypeHandler转换
    private String          remark;
    private Integer         deleted;  // 0-正常，1-已删除
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
}
