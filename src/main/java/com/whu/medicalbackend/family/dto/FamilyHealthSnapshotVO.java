package com.whu.medicalbackend.family.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class FamilyHealthSnapshotVO implements Serializable {
    private Long groupId;
    private List<MemberHealthDetail> members;
    private LocalDateTime updateTime;

    @Data
    public static class MemberHealthDetail {
        private Long userId;
        private String nickname;
        // 健康数据部分
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
        // 今日服药统计
        private Integer completedTasks;
        private Integer totalTasks;
    }
}
