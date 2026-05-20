package com.whu.medicalbackend.common.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 健康数据 DTO（用于服务间传输）
 *
 * @author Medical Assistant Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 健康数据 ID
     */
    private Long dataId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 数据类型（如：血压、血糖、心率等）
     */
    private String dataType;

    /**
     * 数据值（JSON 格式）
     */
    private String dataValue;

    /**
     * 测量时间
     */
    private LocalDateTime measureTime;

    /**
     * 数据来源（0-手动输入，1-设备同步）
     */
    private Integer source;

    /**
     * 备注
     */
    private String remark;
}
