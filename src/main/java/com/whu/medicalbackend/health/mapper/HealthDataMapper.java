package com.whu.medicalbackend.health.mapper;

import com.whu.medicalbackend.health.entity.HealthData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthDataMapper {
    /**
     * Finds the active health data record for a user created today.
     */
    HealthData findByUserIdAndToday(Long userId);

    /**
     * Inserts full data.
     */
    int insert(HealthData healthData);

    /**
     * Conditionally updates fields if they are not null, except measure_time which is always updated.
     */
    int updateSelective(HealthData healthData);
}
