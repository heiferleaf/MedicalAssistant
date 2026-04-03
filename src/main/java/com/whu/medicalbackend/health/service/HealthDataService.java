package com.whu.medicalbackend.health.service;

import com.whu.medicalbackend.health.entity.HealthData;

import java.util.Optional;

public interface HealthDataService {
    /**
     * Records or updates daily health data according to the strategy:
     * one record per user per day.
     */
    void saveOrUpdateDailyHealthData(HealthData healthData);

    /**
     * get latest health data for a user
     */
    Optional<HealthData> getLatestHealthData(Long userId);
}
