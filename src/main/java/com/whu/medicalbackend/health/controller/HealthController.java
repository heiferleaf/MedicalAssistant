package com.whu.medicalbackend.health.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.client.dto.HealthDataDTO;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.health.entity.HealthData;
import com.whu.medicalbackend.health.service.HealthDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthDataService healthDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/sync")
    public Result syncDailyHealthData(@RequestBody HealthData healthData) {
        healthDataService.saveOrUpdateDailyHealthData(healthData);
        return Result.success();
    }

    @GetMapping("/latest")
    public Result<HealthData> getLatestHealthData(@RequestAttribute("userId") Long userId) {
        Optional<HealthData> healthDataOpt = healthDataService.getLatestHealthData(userId);
        return Result.success(healthDataOpt.get());
    }

    /**
     * 内部接口：获取用户今日健康数据（供其他微服务调用）
     */
    @GetMapping("/today/{userId}")
    public HealthDataDTO getTodayHealthData(@PathVariable Long userId) {
        Optional<HealthData> opt = healthDataService.getLatestHealthData(userId);
        if (opt.isEmpty()) return null;
        HealthData hd = opt.get();
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("heartRate", hd.getHeartRate());
            map.put("stepCount", hd.getStepCount());
            map.put("sleepDuration", hd.getSleepDuration());
            map.put("bloodOxygen", hd.getBloodOxygen());
            HealthDataDTO dto = new HealthDataDTO();
            dto.setDataValue(objectMapper.writeValueAsString(map));
            return dto;
        } catch (Exception e) {
            return null;
        }
    }
}
