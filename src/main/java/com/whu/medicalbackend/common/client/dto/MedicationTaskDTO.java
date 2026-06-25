package com.whu.medicalbackend.common.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服药任务 DTO（用于服务间传输）
 *
 * @author Medical Assistant Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 服药计划 ID
     */
    private Long planId;

    /**
     * 药品名称
     */
    private String medicineName;

    /**
     * 计划服药时间
     */
    private LocalDateTime scheduledTime;

    /**
     * 实际服药时间
     */
    private LocalDateTime actualTime;

    /**
     * 任务状态（0-待服药，1-已服药，2-已跳过，3-已过期）
     */
    private Integer status;

    /**
     * 剂量
     */
    private String dosage;

    /**
     * 备注
     */
    private String remark;
}
