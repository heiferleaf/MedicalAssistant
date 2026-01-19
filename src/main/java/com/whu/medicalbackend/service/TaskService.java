package com.whu.medicalbackend. service;

import com.whu.medicalbackend.dto.TaskVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药任务服务接口
 */
public interface TaskService {

    /**
     * 获取今日任务列表
     */
    List<TaskVO> getTodayTasks(Long userId);

    /**
     * 修改任务状态
     * 业务逻辑：
     * 1. 查询任务
     * 2. 验证权限
     * 3. 验证日期（只能修改今天的任务）
     * 4. 根据状态设置operate_time
     * 5. 更新状态
     */
    TaskVO updateTaskStatus(Long userId, Long taskId, Integer status);

    /**
     * 查询历史任务
     */
    List<TaskVO> getHistoryTasks(Long userId, LocalDate startDate, LocalDate endDate,
                                 String medicineName, Integer status);

    /**
     * 标记超时任务为漏服（定时任务调用）
     */
    void markMissedTasks();
}