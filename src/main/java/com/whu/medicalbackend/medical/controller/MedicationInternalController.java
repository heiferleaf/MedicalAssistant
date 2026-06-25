package com.whu.medicalbackend.medical.controller;

import com.whu.medicalbackend.common.client.dto.MedicationTaskDTO;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.medical.entity.MedicationTask;
import com.whu.medicalbackend.medical.mapper.MedicationTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服药任务内部 API 控制器
 *
 * 提供服药任务查询接口供其他微服务调用
 * 路径前缀：/internal/medication
 *
 * 注意：此接口仅供内部服务调用，不对外暴露
 *
 * @author Medical Assistant Team
 */
@RestController
@RequestMapping("/internal/medication")
public class MedicationInternalController {

    private static final Logger logger = LoggerFactory.getLogger(MedicationInternalController.class);

    @Autowired
    private MedicationTaskMapper medicationTaskMapper;

    /**
     * 根据任务 ID 查询服药任务
     *
     * @param taskId 任务 ID
     * @return 服药任务信息
     */
    @GetMapping("/task/{taskId}")
    public Result<MedicationTaskDTO> getTaskById(@PathVariable Long taskId) {
        logger.debug("内部调用: 查询服药任务, taskId={}", taskId);

        MedicationTask task = medicationTaskMapper.findById(taskId);
        if (task == null) {
            return Result.error(404, "服药任务不存在");
        }

        MedicationTaskDTO dto = convertToDTO(task);
        return Result.success(dto);
    }

    /**
     * 查询用户指定日期的服药任务列表
     *
     * @param userId 用户 ID
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 服药任务列表
     */
    @GetMapping("/task/user/{userId}/date/{date}")
    public Result<List<MedicationTaskDTO>> getTasksByUserAndDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        logger.debug("内部调用: 查询用户指定日期的服药任务, userId={}, date={}", userId, date);

        LocalDate taskDate = LocalDate.parse(date);
        List<MedicationTask> tasks = medicationTaskMapper.findByUserIdAndDate(userId, taskDate);

        List<MedicationTaskDTO> dtos = tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    /**
     * 查询用户的服药任务历史
     *
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 服药任务列表
     */
    @GetMapping("/task/user/{userId}/history")
    public Result<List<MedicationTaskDTO>> getTaskHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String medicineName,
            @RequestParam(required = false) Integer status) {
        logger.debug("内部调用: 查询用户服药任务历史, userId={}, startDate={}, endDate={}",
                userId, startDate, endDate);

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        List<MedicationTask> tasks = medicationTaskMapper.findHistory(userId, start, end, medicineName, status);

        List<MedicationTaskDTO> dtos = tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    @GetMapping("/task/user/{userId}/count")
    public Result<Integer> countTasksByDate(
            @PathVariable Long userId,
            @RequestParam String date,
            @RequestParam(required = false) Integer status) {
        LocalDate taskDate = LocalDate.parse(date);
        int count = (status != null)
                ? medicationTaskMapper.countStatusByDate(userId, taskDate, status)
                : medicationTaskMapper.countTotalByDate(userId, taskDate);
        return Result.success(count);
    }

    private MedicationTaskDTO convertToDTO(MedicationTask task) {
        MedicationTaskDTO dto = new MedicationTaskDTO();
        dto.setTaskId(task.getId());
        dto.setUserId(task.getUserId());
        dto.setPlanId(task.getPlanId());
        dto.setScheduledTime(LocalDateTime.of(task.getTaskDate(), task.getTimePoint()));
        dto.setActualTime(task.getOperateTime());
        dto.setStatus(task.getStatus());
        dto.setDosage(task.getDosage());
        return dto;
    }
}
