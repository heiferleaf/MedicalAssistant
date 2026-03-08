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

/**
 * 用药计划删除工具
 */
@Component
public class PlanDeleteTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanDeleteTool.class);

    private final PlanService planService;

    public PlanDeleteTool(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 删除用药计划
     * @param userId 用户 ID
     * @param planId 计划 ID
     * @param medicineName 药品名称
     * @param deleteAll 是否删除所有
     * @return 删除结果
     */
    @Tool(value = "删除用药计划")
    public Map<String, Object> deletePlan(
            @P(value = "用户 ID") String userId,
            @P(value = "计划 ID") Integer planId,
            @P(value = "药品名称") String medicineName,
            @P(value = "是否删除所有，true 表示删除所有") Boolean deleteAll
    ) {
        logger.info("删除用药计划，userId: {}, planId: {}, medicineName: {}, deleteAll: {}", 
                userId, planId, medicineName, deleteAll);
        
        try {
            Long uid = Long.valueOf(userId);
            int deletedCount = 0;
            
            if (deleteAll != null && deleteAll) {
                // 删除所有计划
                List<PlanVO> plans = planService.getPlanList(uid);
                if (plans.isEmpty()) {
                    return Map.of(
                        "success", true,
                        "message", "你目前没有任何用药计划，无需删除",
                        "deleted_count", 0
                    );
                }
                
                for (PlanVO plan : plans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                return Map.of(
                    "success", true,
                    "message", "已成功删除你所有的 " + deletedCount + " 个用药计划，未来的用药提醒已全部取消",
                    "deleted_count", deletedCount
                );
            } else if (planId != null) {
                // 根据 planId 删除
                planService.deletePlan(uid, planId.longValue());
                return Map.of(
                    "success", true,
                    "message", "已成功删除该用药计划，未来的用药提醒已取消",
                    "deleted_count", 1
                );
            } else if (medicineName != null && !medicineName.isBlank()) {
                // 根据药品名称删除
                List<PlanVO> plans = planService.getPlanList(uid);
                List<PlanVO> matchedPlans = plans.stream()
                    .filter(p -> p.getMedicineName().contains(medicineName) || 
                                medicineName.contains(p.getMedicineName()))
                    .collect(java.util.stream.Collectors.toList());
                
                if (matchedPlans.isEmpty()) {
                    return Map.of(
                        "success", false,
                        "message", "未找到名为\"" + medicineName + "\"的用药计划"
                    );
                }
                
                for (PlanVO plan : matchedPlans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                return Map.of(
                    "success", true,
                    "message", "已成功删除 " + deletedCount + " 个与\"" + medicineName + "\"相关的用药计划，未来的用药提醒已取消",
                    "deleted_count", deletedCount
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "缺少必需参数：planId、medicineName 或 deleteAll"
                );
            }
        } catch (Exception e) {
            logger.error("删除用药计划失败", e);
            return Map.of(
                "success", false,
                "message", "删除用药计划失败: " + e.getMessage()
            );
        }
    }
}
