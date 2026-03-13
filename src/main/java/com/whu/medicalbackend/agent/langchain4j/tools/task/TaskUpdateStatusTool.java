package com.whu.medicalbackend.agent.langchain4j.tools.task;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.service.TaskService;
import com.whu.medicalbackend.dto.TaskVO;
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

    @Tool(value = "Update the status of a medication task. Use this when the user wants to mark a task as taken, missed, or update its status. Status codes: 0 = not taken, 1 = taken, 2 = missed.")
    public Map<String, Object> updateTaskStatus(
            @P(value = "The user ID who owns the task") String userId,
            @P(value = "The ID of the task to update") Integer taskId,
            @P(value = "The new status: 0=not taken, 1=taken, 2=missed") Integer status) {
        logger.info("更新任务状态，userId: {}, taskId: {}, status: {}", userId, taskId, status);

        try {
            if (taskId == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：taskId");
                return result;
            }
            if (status == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "缺少必需参数：status");
                return result;
            }
            if (status < 0 || status > 2) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "状态值无效，必须是 0（未服用）、1（已服用）或 2（漏服）");
                return result;
            }

            Long uid = Long.valueOf(userId);
            TaskVO taskVO = taskService.updateTaskStatus(uid, taskId.longValue(), status);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "任务状态更新成功");
            result.put("task_id", taskVO.getId());
            result.put("new_status", status);
            result.put("new_status_text", getStatusText(status));
            return result;
        } catch (Exception e) {
            logger.error("更新任务状态失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "更新任务状态失败: " + e.getMessage());
            return result;
        }
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
