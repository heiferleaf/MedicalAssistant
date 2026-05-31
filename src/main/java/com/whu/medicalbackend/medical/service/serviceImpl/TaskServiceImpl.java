package com.whu.medicalbackend.medical.service.serviceImpl;

import com.whu.medicalbackend.common.response.ResultCode;
import com.whu.medicalbackend.medical.dto.TaskVO;
import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.medical.entity.MedicationTask;
import com.whu.medicalbackend.common.client.FamilyServiceClient;
import com.whu.medicalbackend.common.client.UserServiceClient;
import com.whu.medicalbackend.common.client.dto.UserDTO;
import com.whu.medicalbackend.common.enumField.EventLogEnum;
import com.whu.medicalbackend.common.exception.BusinessException;
import com.whu.medicalbackend.common.schedule.DynamicTaskScheduler;
import com.whu.medicalbackend.medical.mapper.MedicationTaskMapper;
import com.whu.medicalbackend.medical.mapper.MedicineMapper;
import com.whu.medicalbackend.medical.service.TaskService;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.common.lock.DistributedLock;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.DomainEventPublisher;
import com.whu.medicalbackend.family.mapper.FamilyEventLogMapper;
import io.jsonwebtoken.lang.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time. LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.whu.medicalbackend.common.schedule.DynamicTaskScheduler.formatter;

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
    private ObjectProvider<DynamicTaskScheduler> dynamicTaskSchedulerProvider;

    @Autowired
    private RedisService redisService;

    @Autowired
    private FamilyServiceClient familyServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private FamilyEventLogMapper familyEventLogMapper;

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
    @DistributedLock(prefix = RedisKeyBuilderUtil.LOCK_TASK_UPDATE_PREFIX, key = "#taskId", waitTime = 0, message = "任务处理中，请勿重复操作")
    public TaskVO updateTaskStatus(Long userId, Long taskId, Integer status) {
        // 1. 查询任务
        MedicationTask task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getMessage() + "任务不存在");
        }

        Medicine medicine = medicineMapper.findById(task.getMedicineId());
        String medicineName = medicine != null ? medicine.getName() : "未知药品";

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

        // 获取旧状态，用于判断是否需要进行家庭组-健康数据页面内的更改广播
        int oldStatus = task.getStatus();

        if(oldStatus == status) {
            return new TaskVO.TaskVOBuilder().
                    fromEntity(task, medicineName)
                    .build();
        }


        // 5. 更新状态
        taskMapper.updateStatus(taskId, status, operateTime);

        // 记录事务日志
        Long groupId = familyServiceClient.getGroupIdByUserId(userId);
        if (groupId != null && status != 0) {
            // 逻辑对齐：根据状态决定 EventType
            String eventType = (status == 1) ? EventLogEnum.TASK_DONE.name() : EventLogEnum.ALARM_MISSED.name();

            // 插入日志
            familyServiceClient.insertEventLog(groupId, userId, eventType, medicineName);

            // 如果是标记漏服，需要额外清理告警缓存 (保持一致性)
            if (status == 2) {
                String alarmKey = RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, LocalDate.now().toString());
                redisService.delete(alarmKey);
            }
        }

        // 进行广播,条件是标记位已服用，或者原来的状态是已服用
        if (groupId != null && (oldStatus == 1 || status == 1)) {
            handleSnapshotAndBroadcast(groupId, userId, task, status);
        }

        // 进行广播，条件是标记为漏服用
        if (groupId != null && status == 2) {
            UserDTO user = userServiceClient.getUserById(userId);
            Assert.notNull(user, "任务所属用户Id为空");

            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", "medicine_alarm");
            pushData.put("groupId", groupId);
            pushData.put("memberName", user.getNickname());
            pushData.put("medicineName", medicine.getName());
            pushData.put("alarmTime", LocalDateTime.now().format(formatter));

            DomainEvent alarmEvent = DomainEvent.of("medication.alarm", "MedicationTask", String.valueOf(taskId));
            alarmEvent.setUserId(userId);
            alarmEvent.setGroupId(groupId);
            alarmEvent.setPayload(pushData);
            domainEventPublisher.publish(alarmEvent);
        }

        // 6. 重新查询并返回
        task = taskMapper.findById(taskId);


        // 7. 取消定时标记为漏服的任务
        DynamicTaskScheduler dynamicTaskScheduler = dynamicTaskSchedulerProvider.getIfAvailable();
        if (dynamicTaskScheduler != null) {
            dynamicTaskScheduler.cancelTaskSchedule(taskId);
        }

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

    // ========== 私有辅助方法 ==========

    private void handleSnapshotAndBroadcast(Long groupId, Long userId, MedicationTask task, Integer newStatus) {
        Map<String, Object> pushData = new HashMap<>();
        UserDTO user = userServiceClient.getUserById(userId);

        pushData.put("type", "medicine_update");
        pushData.put("memberName", user.getUsername());
        pushData.put("medicineName", user.getNickname());
        pushData.put("date", task.getTaskDate().toString());
        pushData.put("timePoint", task.getTimePoint().toString());
        pushData.put("status", newStatus);

        DomainEvent updateEvent = DomainEvent.of("medication.updated", "MedicationTask", String.valueOf(task.getId()));
        updateEvent.setUserId(userId);
        updateEvent.setGroupId(groupId);
        updateEvent.setPayload(pushData);
        domainEventPublisher.publish(updateEvent);
    }

    private String getMedicineName(Long medicineId) {
        Medicine m = medicineMapper.findById(medicineId);
        return m != null ? m.getName() : "未知药品";
    }

    /**
     * 批量查询药品（避免N+1查询）
     * 性能优化：
     * 假设有10个任务，涉及3种药品
     * - N+1查询：10次循环，每次查询1次 = 10次SQL
     * - 批量查询：1次查询所有药品ID = 3次SQL
     */
    private Map<Long, Medicine> batchQueryMedicines(List<MedicationTask> tasks) {
        List<Long> medicineIds = tasks.stream()
                .map(MedicationTask::getMedicineId)
                .distinct()
                .collect(Collectors.toList());

        return medicineMapper.findByIds(medicineIds).stream()
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

    @Override
    public int countTasksByDate(Long userId, LocalDate date, Integer status) {
        if (status != null) {
            return taskMapper.countStatusByDate(userId, date, status);
        } else {
            return taskMapper.countTotalByDate(userId, date);
        }
    }
}
