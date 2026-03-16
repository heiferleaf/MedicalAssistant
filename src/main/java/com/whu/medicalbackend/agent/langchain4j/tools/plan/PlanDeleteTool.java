package com.whu.medicalbackend.agent.langchain4j.tools.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.service.PlanService;
import com.whu.medicalbackend.dto.PlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlanDeleteTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanDeleteTool.class);

    private final PlanService planService;

    public PlanDeleteTool(PlanService planService) {
        this.planService = planService;
    }

    @Tool(value = "Delete a medication plan. Use this when the user wants to remove, cancel, or delete a medication plan. You can delete by plan ID, by medicine name, or delete all plans.")
    public Map<String, Object> deletePlan(
            @P(value = "The user ID who owns the plan") String userId,
            @P(value = "The ID of the plan to delete (optional)") Integer planId,
            @P(value = "The name of medicine to delete (optional)") String medicineName,
            @P(value = "Set to true to delete all plans (optional)") Boolean deleteAll
    ) {
        logger.info("删除用药计划，userId: {}, planId: {}, medicineName: {}, deleteAll: {}", 
                userId, planId, medicineName, deleteAll);
        
        try {
            Long uid = Long.valueOf(userId);
            int deletedCount = 0;
            
            if (deleteAll != null && deleteAll) {
                List<PlanVO> plans = planService.getPlanList(uid);
                if (plans.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", true);
                    result.put("message", "你目前没有任何用药计划，无需删除");
                    result.put("deleted_count", 0);
                    return result;
                }
                
                for (PlanVO plan : plans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "已成功删除你所有的 " + deletedCount + " 个用药计划，未来的用药提醒已全部取消");
                result.put("deleted_count", deletedCount);
                return result;
            } else if (planId != null) {
                planService.deletePlan(uid, planId.longValue());
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "已成功删除该用药计划，未来的用药提醒已取消");
                result.put("deleted_count", 1);
                return result;
            } else if (medicineName != null && !medicineName.isBlank()) {
                List<PlanVO> plans = planService.getPlanList(uid);
                List<PlanVO> matchedPlans = plans.stream()
                    .filter(p -> p.getMedicineName().contains(medicineName) || 
                                medicineName.contains(p.getMedicineName()))
                    .collect(java.util.stream.Collectors.toList());
                
                if (matchedPlans.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "未找到名为\"" + medicineName + "\"的用药计划");
                    return result;
                }
                
                for (PlanVO plan : matchedPlans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "已成功删除 " + deletedCount + " 个与\"" + medicineName + "\"相关的用药计划，未来的用药提醒已取消");
                result.put("deleted_count", deletedCount);
                return result;
            } else {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：planId、medicineName 或 deleteAll");
                return result;
            }
        } catch (Exception e) {
            logger.error("删除用药计划失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "删除用药计划失败: " + e.getMessage());
            return result;
        }
    }
}
