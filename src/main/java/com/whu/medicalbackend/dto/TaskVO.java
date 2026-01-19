package com.whu.medicalbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.whu.medicalbackend.entity.MedicationTask;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class TaskVO {
    private Long id;
    private Long planId;
    private String medicineName;
    private String dosage;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate taskDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime timePoint;

    private Integer status;  // 0-未服用，1-已服用，2-漏服

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime operateTime;

    private TaskVO(TaskVOBuilder builder) {
        this.id             = builder.id;
        this.planId         = builder.planId;
        this.medicineName   = builder.medicineName;
        this.dosage         = builder.dosage;
        this.taskDate       = builder.taskDate;
        this.timePoint      = builder.timePoint;
        this.status         = builder.status;
        this.operateTime    = builder.operateTime;
    }

    public static class TaskVOBuilder {
        private Long            id;
        private Long            planId;
        private String          medicineName;
        private String          dosage;
        private LocalDate       taskDate;
        private LocalTime       timePoint;
        private Integer         status;
        private LocalDateTime   operateTime;

        public TaskVOBuilder fromEntity(MedicationTask task, String medicineName) {
            this.id             = task.getId();
            this.planId         = task.getPlanId();
            this.medicineName   = medicineName;
            this.dosage         = task.getDosage();
            this.taskDate       = task.getTaskDate();
            this.timePoint      = task.getTimePoint();
            this.status         = task.getStatus();
            this.operateTime    = task.getOperateTime();
            return this;
        }

        public TaskVO build() {
            return new TaskVO(this);
        }
    }
}
