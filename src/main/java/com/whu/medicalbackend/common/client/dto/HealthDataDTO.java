package com.whu.medicalbackend.common.client.dto;

import lombok.Data;

@Data
public class HealthDataDTO {
    private Long id;
    private Long userId;
    private String dataValue;
}