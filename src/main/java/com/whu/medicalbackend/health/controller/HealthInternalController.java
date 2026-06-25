package com.whu.medicalbackend.health.controller;

import com.whu.medicalbackend.common.client.dto.HealthDataDTO;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.health.entity.HealthData;
import com.whu.medicalbackend.health.mapper.HealthDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 健康数据内部 API 控制器
 *
 * 提供健康数据查询接口供其他微服务调用
 * 路径前缀：/internal/health
 *
 * 注意：此接口仅供内部服务调用，不对外暴露
 *
 * @author Medical Assistant Team
 */
@RestController
@RequestMapping("/internal/health")
public class HealthInternalController {

    private static final Logger logger = LoggerFactory.getLogger(HealthInternalController.class);

    @Autowired
    private HealthDataMapper healthDataMapper;

    /**
     * 查询用户今日的健康数据
     *
     * @param userId 用户 ID
     * @return 健康数据
     */
    @GetMapping("/data/user/{userId}/today")
    public Result<HealthDataDTO> getTodayHealthData(@PathVariable Long userId) {
        logger.debug("内部调用: 查询用户今日健康数据, userId={}", userId);

        HealthData healthData = healthDataMapper.findByUserIdAndToday(userId);
        if (healthData == null) {
            return Result.error(404, "今日健康数据不存在");
        }

        HealthDataDTO dto = convertToDTO(healthData);
        return Result.success(dto);
    }

    /**
     * 检查用户今日是否有健康数据
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    @GetMapping("/data/user/{userId}/today/exists")
    public Result<Boolean> checkTodayHealthDataExists(@PathVariable Long userId) {
        logger.debug("内部调用: 检查用户今日健康数据是否存在, userId={}", userId);

        HealthData healthData = healthDataMapper.findByUserIdAndToday(userId);
        return Result.success(healthData != null);
    }

    /**
     * 将 HealthData 实体转换为 HealthDataDTO
     */
    private HealthDataDTO convertToDTO(HealthData healthData) {
        HealthDataDTO dto = new HealthDataDTO();
        dto.setDataId(healthData.getId());
        dto.setUserId(healthData.getUserId());
        // 简化处理：将多个健康指标合并为一个 JSON 字符串
        dto.setDataType("综合健康数据");
        dto.setDataValue(buildDataValue(healthData));
        dto.setMeasureTime(healthData.getMeasureTime() != null ?
                LocalDateTime.ofInstant(healthData.getMeasureTime().toInstant(), ZoneId.systemDefault()) : null);
        dto.setSource(1); // 默认设备同步
        return dto;
    }

    /**
     * 构建健康数据值（JSON 格式）
     */
    private String buildDataValue(HealthData healthData) {
        StringBuilder sb = new StringBuilder("{");
        if (healthData.getHeartRate() != null) {
            sb.append("\"heartRate\":").append(healthData.getHeartRate()).append(",");
        }
        if (healthData.getStepCount() != null) {
            sb.append("\"stepCount\":").append(healthData.getStepCount()).append(",");
        }
        if (healthData.getSleepDuration() != null) {
            sb.append("\"sleepDuration\":").append(healthData.getSleepDuration()).append(",");
        }
        if (healthData.getBloodOxygen() != null) {
            sb.append("\"bloodOxygen\":").append(healthData.getBloodOxygen()).append(",");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1); // 移除最后的逗号
        }
        sb.append("}");
        return sb.toString();
    }
}
