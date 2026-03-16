package com.whu.medicalbackend.printpdf;

import java.util.List;

public class PrintPdfRequest {

    private String generatedTime;
    private String department;
    private String patient;
    private String visitDate;
    private List<MedicationItem> medications;
    private List<HealthDataItem> healthData;
    private List<String> questions;
    private String otherInfo;

    public static class MedicationItem {
        private Long id;
        private String name;
        private String schedule;
        private Integer takenDays;
        private Integer missedCount;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSchedule() {
            return schedule;
        }

        public void setSchedule(String schedule) {
            this.schedule = schedule;
        }

        public Integer getTakenDays() {
            return takenDays;
        }

        public void setTakenDays(Integer takenDays) {
            this.takenDays = takenDays;
        }

        public Integer getMissedCount() {
            return missedCount;
        }

        public void setMissedCount(Integer missedCount) {
            this.missedCount = missedCount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class HealthDataItem {
        private Long id;
        private String date;
        private String indicator;
        private String value;
        private String unit;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getIndicator() {
            return indicator;
        }

        public void setIndicator(String indicator) {
            this.indicator = indicator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public String getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(String generatedTime) {
        this.generatedTime = generatedTime;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public List<MedicationItem> getMedications() {
        return medications;
    }

    public void setMedications(List<MedicationItem> medications) {
        this.medications = medications;
    }

    public List<HealthDataItem> getHealthData() {
        return healthData;
    }

    public void setHealthData(List<HealthDataItem> healthData) {
        this.healthData = healthData;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }
}