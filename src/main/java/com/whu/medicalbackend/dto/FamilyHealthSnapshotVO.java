package com.whu.medicalbackend.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
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
        private Double lastHeartRate;
        private Double lastBloodPressure;
        private LocalDateTime healthUpdateTime;
        // 今日服药统计
        private Integer completedTasks;
        private Integer totalTasks;
    }
}
