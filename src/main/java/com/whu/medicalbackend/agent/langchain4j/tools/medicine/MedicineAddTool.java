package com.whu.medicalbackend.agent.langchain4j.tools.medicine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MedicineAddTool {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAddTool.class);

    @Tool(value = "Add a new medicine to the user's medicine cabinet. IMPORTANT: This tool does NOT actually add the medicine. It returns a confirmation marker that the frontend will use to show a confirmation card to the user.")
    public Map<String, Object> addMedicine(
            @P(value = "The user ID to add the medicine for") String userId,
            @P(value = "The name of the medicine") String medicineName,
            @P(value = "The default dosage (e.g., '1 tablet', '500mg')") String defaultDosage,
            @P(value = "Additional remarks or notes (optional)") String remark) {
        
        logger.info("MedicineAddTool 被调用，userId: {}, medicineName: {}（返回待确认标记）", userId, medicineName);
        
        // 不真正添加药品，而是返回待确认标记
        // 前端会检测到这个标记并显示确认卡片
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("pending_confirmation", true);
        result.put("tool_name", "addMedicine");
        result.put("arguments", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("medicineName", medicineName != null ? medicineName : "未知药品");
            put("defaultDosage", defaultDosage != null ? defaultDosage : "按医嘱");
            put("remark", remark);
        }});
        result.put("message", "请确认是否添加以下药品到药箱");
        
        return result;
    }
}
