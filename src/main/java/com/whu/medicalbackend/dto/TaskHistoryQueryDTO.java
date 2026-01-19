package com.whu.medicalbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 历史任务查询DTO
 * 使用场景：
 * GET /api/task/history - 查询历史任务
 *
 * 注意：这个DTO用于接收查询参数，不是RequestBody
 */
@Data
public class TaskHistoryQueryDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;  // 可选，默认查所有

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;  // 可选，默认查所有

    private String medicineName;  // 可选，按药品名筛选

    private Integer status;  // 可选，按状态筛选（0/1/2）
}