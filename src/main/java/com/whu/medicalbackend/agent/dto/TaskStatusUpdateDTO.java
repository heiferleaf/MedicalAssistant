package com.whu.medicalbackend.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusUpdateDTO {
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值范围：0-2")
    @Max(value = 2, message = "状态值范围：0-2")
    private Integer status; // 0-未服用， 1-已复用， 2-漏服
}
