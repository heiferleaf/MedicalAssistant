package com.whu.medicalbackend.agent.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.service.PlanService;
import com.whu.medicalbackend.dto.PlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用药计划查询工具
 */
@Component
public class PlanQueryTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanQueryTool.class);

    private final PlanService planService;

    public PlanQueryTool(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 查询用户的用药计划
     * @param userId 用户 ID
     * @return 用药计划列表
     */
    @Tool(value = "查询用户的用药计划")
    public Map<String, Object> queryPlans(@P(value = "用户 ID") String userId) {
        logger.info("查询用户用药计划，userId: {}", userId);
        
        try {
            Long uid = Long.valueOf(userId);
            List<PlanVO> plans = planService.getPlanList(uid);
            logger.info("查询到 {} 个用药计划", plans.size());
            
            // 构建结果
            Map<String, Object> result = Map.of(
                "success", true,
                "plans_count", plans.size(),
                "plans", plans.stream().map(plan -> Map.of(
                    "id", plan.getId(),
                    "medicineName", plan.getMedicineName(),
                    "dosage", plan.getDosage(),
                    "timePoints", plan.getTimePoints(),
                    "startDate", plan.getStartDate(),
                    "endDate", plan.getEndDate(),
                    "remark", plan.getRemark()
                )).collect(Collectors.toList())
            );
            
            return result;
        } catch (Exception e) {
            logger.error("查询用药计划失败", e);
            return Map.of(
                "success", false,
                "message", "查询用药计划失败: " + e.getMessage()
            );
        }
    }
}
