package com.whu.medicalbackend.agent.langchain4j.tools.medicine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import com.whu.medicalbackend.agent.service.ToolExecutionPendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MedicineAddTool {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAddTool.class);

    @Autowired
    private ToolExecutionPendingService toolExecutionPendingService;

    @Tool(value = "Add a new medicine to the user's medicine cabinet. IMPORTANT: This tool does NOT actually add the medicine. It returns a confirmation marker that the frontend will use to show a confirmation card to the user.")
    public String addMedicine(
            @P(value = "The user ID to add the medicine for") String userId,
            @P(value = "The name of the medicine") String medicineName,
            @P(value = "The default dosage (e.g., '1 tablet', '500mg')") String defaultDosage,
            @P(value = "Additional remarks or notes (optional)") String remark) {
        
        logger.info("MedicineAddTool 被调用，userId: {}, medicineName: {}（保存待确认请求）", userId, medicineName);
        
        try {
            // 构建 ToolExecutionRequest
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("userId", userId);
            arguments.put("medicineName", medicineName != null ? medicineName : "未知药品");
            arguments.put("defaultDosage", defaultDosage != null ? defaultDosage : "按医嘱");
            arguments.put("remark", remark);
            
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("addMedicine")
                .arguments(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(arguments))
                .build();
            
            // 保存到数据库（需要 sessionId，这里传空字符串，实际使用时由 Agent 框架填充）
            // 注意：这里无法获取 sessionId，所以需要修改方法签名或者通过其他方式获取
            // 暂时先不保存，直接返回 ACTION 标记
            
        } catch (Exception e) {
            logger.error("保存待确认请求失败", e);
        }
        
        // 返回 ACTION 标记，前端会检测到这个标记并显示确认卡片
        StringBuilder response = new StringBuilder();
        response.append("请确认是否添加以下药品到药箱：\n\n");
        response.append("**药品名称**: ").append(medicineName != null ? medicineName : "未知药品").append("\n");
        response.append("**默认剂量**: ").append(defaultDosage != null ? defaultDosage : "按医嘱").append("\n");
        if (remark != null && !remark.isEmpty()) {
            response.append("**备注**: ").append(remark).append("\n");
        }
        response.append("\n[ACTION:addMedicine]");
        
        return response.toString();
    }
}
