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
public class PlanCreateTool {

    private static final Logger logger = LoggerFactory.getLogger(PlanCreateTool.class);

    private final PlanService planService;

    public PlanCreateTool(PlanService planService) {
        this.planService = planService;
    }

    @Tool(value = "Create a new medication plan for the user. IMPORTANT: This tool does NOT actually create the plan. It returns a confirmation marker that the frontend will use to show a confirmation card to the user.")
    public String createPlan(
            @P(value = "The user ID to create the plan for") String userId,
            @P(value = "The name of the medicine") String medicineName,
            @P(value = "The dosage information (e.g., '1 tablet', '500mg')") String dosage,
            @P(value = "List of time points (e.g., ['08:00', '12:00', '18:00'])") List<String> timePoints,
            @P(value = "Start date in yyyy-MM-dd format - MUST be today or a future date!") String startDate,
            @P(value = "End date in yyyy-MM-dd format (optional)") String endDate,
            @P(value = "Additional notes or remarks (optional)") String remark) {
        
        logger.info("PlanCreateTool 被调用，userId: {}, medicineName: {}（返回 ACTION 标记）", userId, medicineName);
        
        // 返回 ACTION 标记，前端会检测到这个标记并显示确认卡片
        StringBuilder response = new StringBuilder();
        response.append("请确认是否创建以下用药计划：\n\n");
        response.append("**药品名称**: ").append(medicineName != null ? medicineName : "未知药品").append("\n");
        response.append("**剂量**: ").append(dosage != null ? dosage : "按医嘱").append("\n");
        if (timePoints != null && !timePoints.isEmpty()) {
            response.append("**服药时间**: ").append(String.join(", ", timePoints)).append("\n");
        }
        response.append("**开始日期**: ").append(startDate != null ? startDate : "今天").append("\n");
        if (endDate != null && !endDate.isEmpty()) {
            response.append("**结束日期**: ").append(endDate).append("\n");
        }
        if (remark != null && !remark.isEmpty()) {
            response.append("**备注**: ").append(remark).append("\n");
        }
        response.append("\n[ACTION:plan_confirm]");
        
        return response.toString();
    }
}
