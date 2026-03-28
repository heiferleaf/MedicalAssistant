package com.whu.medicalbackend.medical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PlanFromMedicineDTO {

    /** 计划剂量（不填则使用药品默认剂量） */
    private String dosage;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "每日服药时间点不能为空")
    @JsonFormat(pattern = "HH:mm")
    private List<LocalTime> timePoints;

    private String remark;
}
