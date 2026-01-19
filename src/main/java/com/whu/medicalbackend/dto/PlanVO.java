package com.whu.medicalbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.whu.medicalbackend.entity.MedicationPlan;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
public class PlanVO {

    private Long id;
    private String medicineName;  // 从medicine表关联查询
    private String dosage;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private List<LocalTime> timePoints;

    private String remark;

    private PlanVO(PlanVO.PlanVOBuilder planVOBuilder) {
        this.id             = planVOBuilder.medicationPlan.getId();
        this.medicineName   = planVOBuilder.medicineName;
        this.dosage         = planVOBuilder.medicationPlan.getDosage();
        this.startDate      = planVOBuilder.medicationPlan.getStartDate();
        this.endDate        = planVOBuilder.medicationPlan.getEndDate();
    }

    public static class PlanVOBuilder {
        private MedicationPlan medicationPlan;
        private String medicineName;

        public PlanVOBuilder setMedicationPlan(MedicationPlan medicationPlan) {
            this.medicationPlan = medicationPlan;
            return this;
        }

        public PlanVOBuilder setMedicineName(String medicineName) {
            this.medicineName = medicineName;
            return this;
        }

        public PlanVO build() {
            return new PlanVO(this);
        }
    }
}
