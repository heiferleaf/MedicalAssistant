package com.whu.medicalbackend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicineCreateDTO {
    @NotBlank(message = "药品名不能为空")
    @Size(max = 100, message = "药物名不能超过100字符")
    private String name;

    @Size(max = 50, message = "推荐剂量不能超过50字符")
    private String defaultDosage;

    @Size(max = 150, message = "备注不能超过150字符")
    private String remark;
}
