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
    @Tool(value = "创建新的用药计划")
    public Map<String, Object> createPlan(
            @P(value = "用户 ID") String userId,
            @P(value = "药品名称") String medicineName,
            @P(value = "剂量") String dosage,
            @P(value = "时间点列表") List<String> timePoints,
            @P(value = "开始日期，格式：yyyy-MM-dd") String startDate,
            @P(value = "结束日期，格式：yyyy-MM-dd") String endDate,
            @P(value = "备注") String remark
    ) {
        logger.info("创建用药计划，userId: {}, medicineName: {}", userId, medicineName);
        
        try {
            // 验证参数
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
            
            // 执行创建
            Long uid = Long.valueOf(userId);
            com.whu.medicalbackend.dto.PlanVO planVO = planService.createPlan(uid, dto);
            
            return Map.of(
                "success", true,
                "message", "用药计划创建成功",
                "plan_id", planVO.getId()
            );
        } catch (Exception e) {
            logger.error("创建用药计划失败", e);
            return Map.of(
                "success", false,
                "message", "创建用药计划失败: " + e.getMessage()
            );
        }
    }
}
