package com.whu.medicalbackend.health.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.exception.BusinessException;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.family.mapper.FamilyGroupMapper;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.health.entity.HealthData;
import com.whu.medicalbackend.health.mapper.HealthDataMapper;
import com.whu.medicalbackend.health.service.HealthDataService;
import com.whu.medicalbackend.ws.event.FamilyHealthDataUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.whu.medicalbackend.common.schedule.DynamicTaskScheduler.formatter;

@Service
public class HealthDataServiceImpl implements HealthDataService {

    @Autowired
    private HealthDataMapper healthDataMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Override
    public void saveOrUpdateDailyHealthData(HealthData inputData) {
        if (inputData.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // Find existing record for today
        HealthData existingData = healthDataMapper.findByUserIdAndToday(inputData.getUserId());

        if (existingData == null) {
            // No record for today, fully insert
            if (inputData.getMeasureTime() == null) {
                inputData.setMeasureTime(new Date()); // Ensure measure_time is set
            }
            healthDataMapper.insert(inputData);
        } else {
            // A record exists for today, we update non-null fields
            // Set the ID to let mapper update the correct row
            inputData.setId(existingData.getId());
            if (inputData.getMeasureTime() == null) {
                inputData.setMeasureTime(new Date()); // Always update measure_time
            }
            healthDataMapper.updateSelective(inputData);
        }

        // 如果用户在家庭组中，发布一个事件通知家庭组成员健康数据更新了
        Long groupId = familyMemberMapper.getGroupIdByUserId(inputData.getUserId());
        if(groupId != null) {
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", "health_data_update");
            pushData.put("groupId", groupId);
            pushData.put("memberId", inputData.getUserId());

            putIfNotNull(pushData, "heartRate", inputData.getHeartRate());
            putIfNotNull(pushData, "stepCount", inputData.getStepCount());
            putIfNotNull(pushData, "sleepDuration", inputData.getSleepDuration());
            putIfNotNull(pushData, "sleepScope", inputData.getSleepScope());
            putIfNotNull(pushData, "bloodOxygen", inputData.getBloodOxygen());
            putIfNotNull(pushData, "relaxType", inputData.getRelaxType());
            putIfNotNull(pushData, "relaxSubType", inputData.getRelaxSubType());
            putIfNotNull(pushData, "relaxDuration", inputData.getRelaxDuration());
            putIfNotNull(pushData, "pressureMaxScore", inputData.getPressureMaxScore());
            putIfNotNull(pushData, "pressureMinScore", inputData.getPressureMinScore());
            putIfNotNull(pushData, "pressureAvgScore", inputData.getPressureAvgScore());
            putIfNotNull(pushData, "measureTime", inputData.getMeasureTime());

            pushData.put("alarmTime", LocalDateTime.now().format(formatter));
            publisher.publishEvent(new FamilyHealthDataUpdateEvent(this, groupId, pushData));
        }
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    @Override
    public Optional<HealthData> getLatestHealthData(Long userId) {
        if(userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        String cacheKey = RedisKeyBuilderUtil.getHealthDataCachePrefix(userId);
        String dataJson = redisService.get(cacheKey);

        try {
            if (dataJson != null) {
                if (dataJson.equals(redisService.nullValue)) {
                    return Optional.empty();
                }
                HealthData cacheData = objectMapper.readValue(dataJson, HealthData.class);
                return Optional.ofNullable(cacheData);
            }

            String lockKey = RedisKeyBuilderUtil.getHealthDataLockKey(userId);
            boolean isLocked = false;
            final int maxAttempts = 5;
            int curAttempt = 0;
            while(true) {
                try {
                    if (redisService.tryLock(lockKey, 5, 10)) {
                        isLocked = true;
                        dataJson = redisService.get(cacheKey);

                        if (dataJson != null) {
                            if (dataJson.equals(redisService.nullValue)) {
                                return Optional.empty();
                            }
                            HealthData cacheData = objectMapper.readValue(dataJson, HealthData.class);
                            return Optional.ofNullable(cacheData);
                        }

                        HealthData dbData = healthDataMapper.findByUserIdAndToday(userId);
                        if (dbData != null) {
                            // 设置随机过期时间，防止缓存雪崩
                            redisService.setWithExpire(cacheKey, objectMapper.writeValueAsString(dbData), 60 * 30 + new Random().nextInt(60), TimeUnit.SECONDS);
                            return Optional.of(dbData);
                        }
                        // 数据库中没有，缓存一个空值，防止缓存穿透
                        redisService.setWithExpire(cacheKey, redisService.nullValue, 60 * 5, TimeUnit.SECONDS);
                        return Optional.empty();
                    }
                    else {
                        if(curAttempt >= maxAttempts) {
                            redisService.setWithExpire(cacheKey, redisService.nullValue, 60 * 5, TimeUnit.SECONDS);
                            return Optional.empty();
                        }
                        curAttempt++;
                        // 获取锁失败，说明有其他线程正在加载数据，等待一段时间后重试
                        Thread.sleep(100);
                    }
                } finally {
                    // 释放锁
                    if (isLocked)
                        redisService.unlock(lockKey);
                }
            }
        } catch (JsonProcessingException | InterruptedException e) {
            throw new BusinessException("缓存数据赌读取失败", e);
        }
    }
}
