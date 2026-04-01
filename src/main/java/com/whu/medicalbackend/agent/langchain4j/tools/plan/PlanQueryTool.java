package com.whu.medicalbackend.agent.langchain4j.tools.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.medical.service.PlanService;
import com.whu.medicalbackend.medical.dto.PlanVO;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionBroadcaster;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PlanQueryTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanQueryTool.class);

    private final PlanService planService;
    private final ToolExecutionWrapper wrapper;

    public PlanQueryTool(PlanService planService, ToolExecutionBroadcaster broadcaster) {
        this.planService = planService;
        this.wrapper = new ToolExecutionWrapper(broadcaster);
    }

    @Tool(value = "Query the user's medication plans. Use this when the user asks about their medication plans, wants to see what plans they have, or requests information about their current medications.")
    public Map<String, Object> queryPlans(@P(value = "The user ID to query plans for") String userId) {
        logger.info("查询用户用药计划，userId: {}", userId);

        return wrapper.execute("queryPlans", 
            Map.of("userId", userId),
            "查询用户用药计划",
            () -> {
                try {
                    Long uid = Long.valueOf(userId);
                    List<PlanVO> plans = planService.getPlanList(uid);
                    logger.info("查询到 {} 个用药计划", plans.size());

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", true);
                    result.put("plans_count", plans.size());
                    result.put("plans", plans.stream().map(plan -> {
                        Map<String, Object> planMap = new LinkedHashMap<>();
                        planMap.put("id", plan.getId());
                        planMap.put("medicineName", plan.getMedicineName());
                        planMap.put("dosage", plan.getDosage());
                        planMap.put("timePoints", plan.getTimePoints());
                        planMap.put("startDate", plan.getStartDate());
                        planMap.put("endDate", plan.getEndDate());
                        planMap.put("remark", plan.getRemark());
                        return planMap;
                    }).collect(Collectors.toList()));

                    return result;
                } catch (Exception e) {
                    logger.error("查询用药计划失败", e);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "查询用药计划失败: " + e.getMessage());
                    return result;
                }
            }
        );
    }
}
