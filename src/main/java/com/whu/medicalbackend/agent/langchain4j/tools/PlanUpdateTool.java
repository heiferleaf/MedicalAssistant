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
import java.util.List;
import java.util.Map;

/**
 * 用药计划更新工具
 */
@Component
public class PlanUpdateTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanUpdateTool.class);

    private final PlanService planService;

    public PlanUpdateTool(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 更新用药计划
     * @param userId 用户 ID
     * @param planId 计划 ID
     * @param medicineName 药品名称
     * @param dosage 剂量
     * @param timePoints 时间点列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param remark 备注
     * @return 更新结果
     */
    @Tool(value = "更新现有的用药计划")
    public Map<String, Object> updatePlan(
            @P(value = "用户 ID") String userId,
            @P(value = "计划 ID") Integer planId,
            @P(value = "药品名称") String medicineName,
            @P(value = "剂量") String dosage,
            @P(value = "时间点列表") List<String> timePoints,
            @P(value = "开始日期，格式：yyyy-MM-dd") String startDate,
            @P(value = "结束日期，格式：yyyy-MM-dd") String endDate,
            @P(value = "备注") String remark
    ) {
        logger.info("更新用药计划，userId: {}, planId: {}", userId, planId);
        
        try {
            // 验证参数
            if (planId == null) {
                return Map.of(
                    "success", false,
                    "message", "缺少必需参数：planId"
                );
            }
            if (medicineName == null || medicineName.isBlank()) {
                return Map.of(
                    "success", false,
                    "message", "缺少必需参数：medicineName"
                );
            }
            if (dosage == null || dosage.isBlank()) {
                dosage = "按医嘱";
            }
            if (timePoints == null || timePoints.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "缺少必需参数：timePoints"
                );
            }
            if (startDate == null || startDate.isBlank()) {
                return Map.of(
                    "success", false,
                    "message", "缺少必需参数：startDate"
                );
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
            
            // 执行更新
            Long uid = Long.valueOf(userId);
            planService.updatePlan(uid, planId.longValue(), dto);
            
            return Map.of(
                "success", true,
                "message", "用药计划更新成功",
                "plan_id", planId
            );
        } catch (Exception e) {
            logger.error("更新用药计划失败", e);
            return Map.of(
                "success", false,
                "message", "更新用药计划失败: " + e.getMessage()
            );
        }
    }
}
