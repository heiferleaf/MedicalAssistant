package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.dto.TaskStatusUpdateDTO;
import com.whu.medicalbackend.dto.TaskVO;
import com.whu.medicalbackend.service.TaskService;
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
     *
     * URL: PUT /api/task/123/status?userId=1
     * Body: {"status": 1}
     *
     * SpringMVC知识点：
     * - 路径中的{taskId}和方法路径/status组合
     * - 完整路径：/api/task/{taskId}/status
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
     *
     * URL: GET /api/task/history?userId=1&startDate=2026-01-01&endDate=2026-01-31&medicineName=xxx&status=1
     *
     * SpringMVC知识点：
     * - @RequestParam(required = false)：可选参数
     * - @DateTimeFormat：日期格式化
     *   - 将字符串"2026-01-01"自动转为LocalDate对象
     *   - pattern指定格式
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
}