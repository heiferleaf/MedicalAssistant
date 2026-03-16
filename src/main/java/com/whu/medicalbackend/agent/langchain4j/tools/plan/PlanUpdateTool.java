package com.whu.medicalbackend.agent.langchain4j.tools.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.service.PlanService;
import com.whu.medicalbackend.dto.PlanCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlanUpdateTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanUpdateTool.class);

    private final PlanService planService;

    public PlanUpdateTool(PlanService planService) {
        this.planService = planService;
    }

    @Tool(value = "Update an existing medication plan. Use this when the user wants to modify, change, or adjust an existing medication plan, such as changing dosage, time points, or dates.")
    public Map<String, Object> updatePlan(
            @P(value = "The user ID who owns the plan") String userId,
            @P(value = "The ID of the plan to update") Integer planId,
            @P(value = "The new name of the medicine") String medicineName,
            @P(value = "The new dosage information (e.g., '1 tablet', '500mg')") String dosage,
            @P(value = "New list of time points (e.g., ['08:00', '12:00', '18:00'])") List<String> timePoints,
            @P(value = "New start date in yyyy-MM-dd format") String startDate,
            @P(value = "New end date in yyyy-MM-dd format (optional)") String endDate,
            @P(value = "New additional notes or remarks (optional)") String remark
    ) {
        logger.info("更新用药计划，userId: {}, planId: {}", userId, planId);
        
        try {
            if (planId == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：planId");
                return result;
            }
            if (medicineName == null || medicineName.isBlank()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：medicineName");
                return result;
            }
            if (dosage == null || dosage.isBlank()) {
                dosage = "按医嘱";
            }
            if (timePoints == null || timePoints.isEmpty()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：timePoints");
                return result;
            }
            if (startDate == null || startDate.isBlank()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：startDate");
                return result;
            }
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;
            List<LocalTime> times = timePoints.stream()
                    .map(LocalTime::parse)
                    .collect(java.util.stream.Collectors.toList());
            
            PlanCreateDTO dto = new PlanCreateDTO();
            dto.setMedicineName(medicineName);
            dto.setDosage(dosage);
            dto.setTimePoints(times);
            dto.setStartDate(start);
            dto.setEndDate(end);
            dto.setRemark(remark);
            
            Long uid = Long.valueOf(userId);
            planService.updatePlan(uid, planId.longValue(), dto);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "用药计划更新成功");
            result.put("plan_id", planId);
            return result;
        } catch (Exception e) {
            logger.error("更新用药计划失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "更新用药计划失败: " + e.getMessage());
            return result;
        }
    }
}
