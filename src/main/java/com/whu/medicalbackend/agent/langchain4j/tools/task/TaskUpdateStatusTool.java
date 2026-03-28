package com.whu.medicalbackend.agent.langchain4j.tools.task;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.medical.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TaskUpdateStatusTool {

    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateStatusTool.class);

    private final TaskService taskService;

    public TaskUpdateStatusTool(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(value = "Update the status of a medication task. IMPORTANT: This tool does NOT actually update the task. It returns a confirmation marker that the frontend will use to show a confirmation card to the user. Use this when user reports TAKING or MISSING a medicine. Status codes: 0 = not taken, 1 = taken, 2 = missed.")
    public Map<String, Object> updateTaskStatus(
            @P(value = "The user ID who owns the task") String userId,
            @P(value = "The ID of the task to update (optional, if not provided will create a new task)") Integer taskId,
            @P(value = "The medicine name") String medicineName,
            @P(value = "The scheduled time point (e.g., '08:00')") String timePoint,
            @P(value = "The dosage information") String dosage,
            @P(value = "The new status: 0=not taken, 1=taken, 2=missed") Integer status) {
        logger.info("TaskUpdateStatusTool 被调用，userId: {}, taskId: {}, medicineName: {}, status: {}（返回待确认标记）", userId, taskId, medicineName, status);

        // 不真正更新任务，而是返回待确认标记
        // 前端会检测到这个标记并显示确认卡片
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("pending_confirmation", true);
        result.put("tool_name", "updateTaskStatus");
        result.put("arguments", new LinkedHashMap<String, Object>() {{
            put("userId", userId);
            put("taskId", taskId); // 可以为 null
            put("medicineName", medicineName != null ? medicineName : "未知药物");
            put("timePoint", timePoint != null ? timePoint : "未知时间");
            put("dosage", dosage != null ? dosage : "按医嘱");
            put("status", status);
        }});
        result.put("message", "请确认是否更新此用药任务状态");
        
        return result;
    }

    private String getStatusText(Integer status) {
        if (status == null)
            return "未知";
        switch (status) {
            case 0:
                return "未服用";
            case 1:
                return "已服用";
            case 2:
                return "漏服";
            default:
                return "未知";
        }
    }
}
