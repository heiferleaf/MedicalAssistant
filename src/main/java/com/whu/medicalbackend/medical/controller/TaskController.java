package com.whu.medicalbackend.medical.controller;

import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.agent.dto.TaskStatusUpdateDTO;
import com.whu.medicalbackend.medical.dto.TaskVO;
import com.whu.medicalbackend.medical.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药任务控制器
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 获取今日任务列表
     *
     * URL: GET /api/task/today? userId=1
     */
    @GetMapping("/today")
    public Result<List<TaskVO>> getTodayTasks(@RequestAttribute("userId") Long userId) {
        List<TaskVO> tasks = taskService.getTodayTasks(userId);
        return Result.success(tasks);
    }

    /**
     * 修改任务状态
     */
    @PutMapping("/{taskId}/status")
    public Result<TaskVO> updateTaskStatus(@RequestAttribute("userId") Long userId,
                                           @PathVariable Long taskId,
                                           @Valid @RequestBody TaskStatusUpdateDTO dto) {
        TaskVO task = taskService. updateTaskStatus(userId, taskId, dto.getStatus());
        return Result.success("修改成功", task);
    }

    /**
     * 查询历史任务
     */
    @GetMapping("/history")
    public Result<List<TaskVO>> getHistoryTasks(
            @RequestAttribute("userId") Long userId,
            @RequestParam(name = "start", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "end", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) Integer status) {

        List<TaskVO> tasks = taskService.getHistoryTasks(userId, startDate, endDate, medicineName, status);
        return Result.success(tasks);
    }

    /**
     * 内部接口：统计指定日期任务数（供其他微服务调用）
     */
    @GetMapping("/count")
    public int countTasksByDate(@RequestParam Long userId,
                                @RequestParam String date,
                                @RequestParam(required = false) Integer status) {
        return taskService.countTasksByDate(userId, LocalDate.parse(date), status);
    }
}