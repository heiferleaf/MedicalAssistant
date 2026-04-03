package com.whu.medicalbackend.health.controller;


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
}
