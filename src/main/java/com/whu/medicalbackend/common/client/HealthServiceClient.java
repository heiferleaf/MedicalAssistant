package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.HealthDataDTO;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceClient {
    
    public HealthDataDTO getTodayHealthData(Long userId) {
        return new HealthDataDTO();
    }
}
