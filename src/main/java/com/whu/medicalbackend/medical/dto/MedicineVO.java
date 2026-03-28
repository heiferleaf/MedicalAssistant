package com.whu.medicalbackend.medical.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicineVO{

    private Long id;
    private String name;
    private String defaultDosage;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
