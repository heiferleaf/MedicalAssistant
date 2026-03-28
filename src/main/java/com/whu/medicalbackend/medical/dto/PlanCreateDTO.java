package com.whu.medicalbackend.medical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PlanCreateDTO {

    @NotBlank(message = "药品名称不能为空")
    private String medicineName;

    private String dosage;  // 可选

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;  // 可选，留空表示长期

    @NotEmpty(message = "服药时间点不能为空")
    @JsonFormat(pattern = "HH:mm")
    private List<LocalTime> timePoints;

    private String remark;  // 可选
}
