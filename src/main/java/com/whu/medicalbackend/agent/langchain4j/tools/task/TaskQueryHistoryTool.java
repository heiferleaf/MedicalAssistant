package com.whu.medicalbackend.agent.langchain4j.tools.task;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.medical.service.TaskService;
import com.whu.medicalbackend.medical.dto.TaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TaskQueryHistoryTool {

    private static final Logger logger = LoggerFactory.getLogger(TaskQueryHistoryTool.class);

    private final TaskService taskService;

    public TaskQueryHistoryTool(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(value = "Query historical medication tasks. Use this when the user wants to see their past medication history, check what they've taken before, or view records from specific dates.")
    public Map<String, Object> getHistoryTasks(
            @P(value = "The user ID to get history for") String userId,
            @P(value = "Start date in yyyy-MM-dd format (optional)") String startDate,
            @P(value = "End date in yyyy-MM-dd format (optional)") String endDate,
            @P(value = "Filter by medicine name (optional)") String medicineName,
            @P(value = "Filter by status: 0=not taken, 1=taken, 2=missed (optional)") Integer status) {
        logger.info("查询用户历史任务，userId: {}, startDate: {}, endDate: {}, medicineName: {}, status: {}",
                userId, startDate, endDate, medicineName, status);

        try {
            Long uid = Long.valueOf(userId);

            LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;

            List<TaskVO> tasks = taskService.getHistoryTasks(uid, start, end, medicineName, status);
            logger.info("查询到 {} 个历史任务", tasks.size());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("tasks_count", tasks.size());
            result.put("tasks", tasks.stream().map(task -> {
                Map<String, Object> taskMap = new LinkedHashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("planId", task.getPlanId());
                taskMap.put("medicineName", task.getMedicineName());
                taskMap.put("dosage", task.getDosage());
                taskMap.put("taskDate", task.getTaskDate().toString());
                taskMap.put("timePoint", task.getTimePoint().toString());
                taskMap.put("status", task.getStatus());
                taskMap.put("statusText", getStatusText(task.getStatus()));
                if (task.getOperateTime() != null) {
                    taskMap.put("operateTime", task.getOperateTime().toString());
                }
                return taskMap;
            }).collect(Collectors.toList()));

            return result;
        } catch (Exception e) {
            logger.error("查询历史任务失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "查询历史任务失败: " + e.getMessage());
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
