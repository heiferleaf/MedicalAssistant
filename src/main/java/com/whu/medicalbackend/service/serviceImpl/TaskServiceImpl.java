package com.whu.medicalbackend.service.serviceImpl;

import com. whu.medicalbackend. common.ResultCode;
import com.whu.medicalbackend.dto.TaskVO;
import com.whu. medicalbackend.entity.Medicine;
import com.whu. medicalbackend.entity.MedicationTask;
import com.whu. medicalbackend.exception.BusinessException;
import com.whu.medicalbackend.mapper. MedicationTaskMapper;
import com.whu.medicalbackend.mapper.MedicineMapper;
import com.whu.medicalbackend.schedule.DynamicTaskScheduler;
import com.whu.medicalbackend.service. TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time. LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用药任务服务实现类
 *
 * 设计模式：
 * 1. 策略模式（Strategy）：不同状态的operate_time处理策略
 * 2. 模板方法模式（Template Method）：查询任务的通用流程
 */
@Service
@Transactional(rollbackFor = Exception. class)
public class TaskServiceImpl implements TaskService {

    @Autowired
    private MedicationTaskMapper taskMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private DynamicTaskScheduler dynamicTaskScheduler;

    /**
     * 获取今日任务列表
     *
     * 性能优化：批量查询药品，避免N+1查询
     */
    @Override
    public List<TaskVO> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. 查询今日任务
        List<MedicationTask> tasks = taskMapper.findByUserIdAndDate(userId, today);

        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查询药品（性能优化）
        Map<Long, Medicine> medicineMap = batchQueryMedicines(tasks);

        // 3. 转换为VO
        return tasks.stream()
                .map(task -> {
                    Medicine medicine = medicineMap.get(task.getMedicineId());
                    String medicineName = medicine != null ?  medicine.getName() : "未知药品";
                    return new TaskVO.TaskVOBuilder()
                            .fromEntity(task, medicineName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 修改任务状态
     *
     * 设计模式：策略模式
     * 不同状态对应不同的operate_time处理策略
     */
    @Override
    public TaskVO updateTaskStatus(Long userId, Long taskId, Integer status) {
        // 1. 查询任务
        MedicationTask task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "任务不存在");
        }

        // 2. 验证权限
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "无权操作此任务");
        }

        // 3. 验证日期（只能修改今天的任务）
        LocalDate today = LocalDate.now();
        if (! task.getTaskDate().equals(today)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "只能修改今天的任务");
        }

        // 4. 根据状态设置operate_time（策略模式）
        LocalDateTime operateTime = calculateOperateTime(status);

        // 5. 更新状态
        taskMapper.updateStatus(taskId, status, operateTime);

        if(status == 1 || status == 2) {
            dynamicTaskScheduler.cancelTaskSchedule(taskId);
        }

        // 6. 重新查询并返回
        task = taskMapper.findById(taskId);
        Medicine medicine = medicineMapper.findById(task.getMedicineId());
        String medicineName = medicine != null ? medicine.getName() : "未知药品";

        return new TaskVO.TaskVOBuilder()
                .fromEntity(task, medicineName)
                .build();
    }

    /**
     * 查询历史任务
     *
     * 性能优化：批量查询药品
     */
    @Override
    public List<TaskVO> getHistoryTasks(Long userId, LocalDate startDate, LocalDate endDate,
                                        String medicineName, Integer status) {
        // 1. 查询历史任务
        List<MedicationTask> tasks = taskMapper.findHistory(
                userId, startDate, endDate, medicineName, status
        );

        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查询药品
        Map<Long, Medicine> medicineMap = batchQueryMedicines(tasks);

        // 3. 转换为VO
        return tasks.stream()
                .map(task -> {
                    Medicine medicine = medicineMap.get(task.getMedicineId());
                    String name = medicine != null ? medicine.getName() : "未知药品";
                    return new TaskVO.TaskVOBuilder()
                            .fromEntity(task, name)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 标记超时任务为漏服（定时任务调用）
     *
     * 业务逻辑：
     * 查询条件：status=0（未服用） AND task_date=今天 AND time_point < 当前时间-30分钟
     */
    @Override
    public void markMissedTasks() {
        LocalDate today = LocalDate.now();
        // 当前时间减30分钟（超过计划时间30分钟算漏服）
        LocalDateTime currentTime = LocalDateTime.now().minusMinutes(30);

        // 1. 查询需要标记为漏服的任务
        List<MedicationTask> tasks = taskMapper.findTasksToMarkAsMissed(today, currentTime);

        // 2. 批量更新为漏服
        if (!tasks.isEmpty()) {
            List<Long> ids = tasks.stream()
                    .map(MedicationTask::getId)
                    .collect(Collectors.toList());

            taskMapper.batchUpdateToMissed(ids);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 批量查询药品（避免N+1查询）
     * 性能优化：
     * 假设有10个任务，涉及3种药品
     * - N+1查询：10次循环，每次查询1次 = 10次SQL
     * - 批量查询：1次查询所有药品ID = 3次SQL
     */
    private Map<Long, Medicine> batchQueryMedicines(List<MedicationTask> tasks) {
        // 收集所有药品ID（去重）
        List<Long> medicineIds = tasks.stream()
                .map(MedicationTask::getMedicineId)
                .distinct()
                .collect(Collectors.toList());

        return medicineIds.stream()
                .map(id -> medicineMapper.findById(id))
                .filter(m -> m != null)
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));
    }

    /**
     * 根据状态计算operate_time
     *
     * 设计模式：策略模式
     * 策略：
     * - 状态0（未服用）：清空operate_time（用户取消标记）
     * - 状态1（已服用）：设置为当前时间（记录服药时间）
     * - 状态2（漏服）：设置为当前时间（记录用户主动标记的时间）
     *
     * Java知识点：switch语句
     */
    private LocalDateTime calculateOperateTime(Integer status) {
        switch (status) {
            case 0:  // 未服用
                return null;
            case 1:  // 已服用
                return LocalDateTime.now();
            case 2:  // 漏服（用户主动标记）
                return LocalDateTime.now();
            default:
                return null;
        }
    }
}