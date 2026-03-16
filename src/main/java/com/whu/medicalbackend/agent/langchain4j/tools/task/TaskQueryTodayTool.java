package com.whu.medicalbackend.agent.langchain4j.tools.task;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.whu.medicalbackend.service.TaskService;
import com.whu.medicalbackend.dto.TaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TaskQueryTodayTool {

    private static final Logger logger = LoggerFactory.getLogger(TaskQueryTodayTool.class);

    private final TaskService taskService;

    public TaskQueryTodayTool(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(value = "Get today's medication tasks. Use this when the user asks about their tasks for today, wants to see what medications they need to take today, or requests their daily schedule.")
    public Map<String, Object> getTodayTasks(@P(value = "The user ID to get tasks for") String userId) {
        logger.info("查询用户今日任务，userId: {}", userId);

        try {
            Long uid = Long.valueOf(userId);
            List<TaskVO> tasks = taskService.getTodayTasks(uid);
            logger.info("查询到 {} 个今日任务", tasks.size());

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
            logger.error("查询今日任务失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "查询今日任务失败: " + e.getMessage());
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
