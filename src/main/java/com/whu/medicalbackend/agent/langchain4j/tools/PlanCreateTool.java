package com.whu.medicalbackend.agent.langchain4j.tools;

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

/**
 * 用药计划创建工具
 */
@Component
public class PlanCreateTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanCreateTool.class);

    private final PlanService planService;

    public PlanCreateTool(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 创建用药计划
     * @param userId 用户 ID
     * @param medicineName 药品名称
     * @param dosage 剂量
     * @param timePoints 时间点列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param remark 备注
     * @return 创建结果
     */
    @Tool(value = "Create a new medication plan for the user. Use this when the user wants to add a new medication to their schedule, set up reminders, or create a new plan for taking medications.")
    public Map<String, Object> createPlan(
            @P(value = "The user ID to create the plan for") String userId,
            @P(value = "The name of the medicine") String medicineName,
            @P(value = "The dosage information (e.g., '1 tablet', '500mg')") String dosage,
            @P(value = "List of time points (e.g., ['08:00', '12:00', '18:00'])") List<String> timePoints,
            @P(value = "Start date in yyyy-MM-dd format") String startDate,
            @P(value = "End date in yyyy-MM-dd format (optional)") String endDate,
            @P(value = "Additional notes or remarks (optional)") String remark
    ) {
        logger.info("创建用药计划，userId: {}, medicineName: {}", userId, medicineName);
        
        try {
            // 验证参数
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
            
            // 解析日期和时间
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;
            List<LocalTime> times = timePoints.stream()
                    .map(LocalTime::parse)
                    .collect(java.util.stream.Collectors.toList());
            
            // 创建 DTO
            PlanCreateDTO dto = new PlanCreateDTO();
            dto.setMedicineName(medicineName);
            dto.setDosage(dosage);
            dto.setTimePoints(times);
            dto.setStartDate(start);
            dto.setEndDate(end);
            dto.setRemark(remark);
            
            // 执行创建
            Long uid = Long.valueOf(userId);
            com.whu.medicalbackend.dto.PlanVO planVO = planService.createPlan(uid, dto);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "用药计划创建成功");
            result.put("plan_id", planVO.getId());
            return result;
        } catch (Exception e) {
            logger.error("创建用药计划失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "创建用药计划失败: " + e.getMessage());
            return result;
        }
    }
}
