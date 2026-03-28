package com.whu.medicalbackend.medical.mapper;

import com.whu.medicalbackend.medical.entity.HealthData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HealthDataMapper {
    /**
     * 获取指定用户最新的一条健康数据
     * 用于家庭看板的实时状态展示
     */
    HealthData findLatestByUserId(@Param("userId") Long userId);

    /**
     * 插入新的健康测量数据
     */
    int insert(HealthData healthData);
}