package com.whu.medicalbackend.health.entity;

import java.util.Date;
import lombok.Data;

@Data
public class HealthData {
    private Long id;                    // 主键ID
    private Long userId;                // 关联用户ID
    private Double heartRate;           // 平均心率
    private Integer stepCount;          // 步数
    private Double sleepDuration;       // 睡眠时长（小时）
    private Integer sleepScope;         // 睡眠质量评分
    private Double bloodOxygen;         // 平均血氧
    private String relaxType;           // 放松活动类型（如冥想、深呼吸等）
    private String relaxSubType;        // 放松活动子类型（如冥想中的引导冥想、正念冥想等）
    private Double relaxDuration;       // 放松活动持续时间（分钟）
    private Integer pressureMaxScore;   // 压力评分最高值
    private Integer pressureMinScore;   // 压力评分最低值
    private Integer pressureAvgScore;   // 压力评分平均值
    private Date measureTime;           // 测量时间
    private Date createdAt;             // 创建时间
    private Integer isDeleted;          // 逻辑删除标志，0表示未删除，1表示已删除
}