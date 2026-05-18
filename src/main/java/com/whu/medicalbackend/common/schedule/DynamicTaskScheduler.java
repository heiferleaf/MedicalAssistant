package com.whu.medicalbackend.common.schedule;

import com.whu.medicalbackend.common.infra.delay.DelayTask;
import com.whu.medicalbackend.common.infra.delay.DelayTaskPublisher;
import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.DomainEventPublisher;
import com.whu.medicalbackend.medical.entity.MedicationTask;
import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.family.mapper.FamilyEventLogMapper;
import com.whu.medicalbackend.family.mapper.FamilyInviteApplyMapper;
import com.whu.medicalbackend.common.client.FamilyServiceClient;
import com.whu.medicalbackend.common.client.UserServiceClient;
import com.whu.medicalbackend.common.client.dto.UserDTO;
import com.whu.medicalbackend.medical.mapper.MedicationTaskMapper;
import com.whu.medicalbackend.medical.mapper.MedicineMapper;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.common.enumField.EventLogEnum;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import io.jsonwebtoken.lang.Assert;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "infra.legacy-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@Transactional(rollbackFor = Exception.class)
public class DynamicTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DynamicTaskScheduler.class);

    @Autowired
    private MedicationTaskMapper taskMapper;

    @Autowired
    private FamilyEventLogMapper familyEventLogMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private DelayTaskPublisher delayTaskPublisher;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private FamilyServiceClient familyServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;


    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("【动态调度】应用启动，开始初始化延迟任务（Redis ZSET 持久化）");
        logger.info("========================================");
        scheduleTasksForToday();
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void refreshDailyTasks() {
        logger.info("========================================");
        logger.info("【动态调度】每日刷新：{}", LocalDate.now());
        logger.info("========================================");
        scheduleTasksForToday();
    }

    private void scheduleTasksForToday() {
        LocalDate today = LocalDate.now();

        List<MedicationTask> tasks = taskMapper.findUncompletedTasksByDate(today);

        if (tasks.isEmpty()) {
            logger.info("【动态调度】今天没有未完成的任务");
            return;
        }

        logger.info("【动态调度】今天共有 {} 个未服用任务", tasks.size());

        int scheduledCount = 0;
        int expiredCount = 0;

        for (MedicationTask task : tasks) {
            boolean scheduled = scheduleTaskTimeout(task);
            if (scheduled) {
                scheduledCount++;
            } else {
                expiredCount++;
            }
        }

        logger.info("【动态调度】发布延迟任务：{}个，已超时直接标记：{}个", scheduledCount, expiredCount);
    }

    private boolean scheduleTaskTimeout(MedicationTask task) {
        LocalDateTime taskTime = LocalDateTime.of(task.getTaskDate(), task.getTimePoint());
        LocalDateTime timeoutTime = taskTime.plusMinutes(2);

        LocalDateTime now = LocalDateTime.now();
        if (timeoutTime.isBefore(now) || timeoutTime.isEqual(now)) {
            logger.info("【动态调度】任务ID={} 已超时，直接标记漏服（计划时间：{}，超时时间：{}）",
                    task.getId(), taskTime, timeoutTime);
            markTaskAsMissed(task.getId());
            return false;
        }

        Medicine medicine = medicineMapper.findById(task.getMedicineId());
        String medName = (medicine != null) ? medicine.getName() : "未知药品";

        String alarmKey = "alarm:" + task.getId();
        if (Boolean.TRUE.equals(redisService.setIfAbsent(alarmKey, "1", 24, java.util.concurrent.TimeUnit.HOURS))) {
            // 发布漏服标记延迟任务到 Redis ZSET
            Instant missedAt = timeoutTime.atZone(ZoneId.systemDefault()).toInstant();
            DelayTask missedTask = DelayTask.of("medication.missed", String.valueOf(task.getId()), missedAt);
            missedTask.getPayload().put("taskId", task.getId());
            delayTaskPublisher.publish(missedTask);
            logger.info("【调度诊断】漏服延迟任务已发布到 Redis: taskId={}, taskType=medication.missed, executeAt={}",
                    task.getId(), timeoutTime);
        }

        String remindKey = "remind:" + task.getId();
        if (Boolean.TRUE.equals(redisService.setIfAbsent(remindKey, "1", 24, java.util.concurrent.TimeUnit.HOURS))) {
            LocalDateTime remindTime = taskTime.minusMinutes(5);
            if (remindTime.isBefore(now) || remindTime.isEqual(now)) {
                handleRemindBroadcast(task.getId(), task.getUserId(), medName);
            } else {
                Instant remindAt = remindTime.atZone(ZoneId.systemDefault()).toInstant();
                DelayTask remindTask = DelayTask.of("medication.remind", String.valueOf(task.getId()), remindAt);
                remindTask.getPayload().put("userId", task.getUserId());
                remindTask.getPayload().put("medicineName", medName);
                delayTaskPublisher.publish(remindTask);
                logger.info("【调度诊断】提醒延迟任务已发布到 Redis: taskId={}, taskType=medication.remind, executeAt={}",
                        task.getId(), remindTime);
            }
        }

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void markTaskAsMissed(Long taskId) {
        try {
            logger.info("【动态调度】⏰ 超时！标记任务ID={} 为漏服", taskId);

            taskMapper.updateStatus(taskId, 2, null);

            MedicationTask task = taskMapper.findById(taskId);
            if (task != null) {
                Long userId = task.getUserId();
                Long groupId = familyServiceClient.getGroupIdByUserId(userId);
                Medicine medicine = medicineMapper.findById(task.getMedicineId());
                if (userId != null && groupId != null && medicine != null) {
                    familyEventLogMapper.insertLog(groupId, userId, EventLogEnum.ALARM_MISSED.name(), medicine.getName());
                    String alarmKey = RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, LocalDate.now().toString());
                    redisService.delete(alarmKey);

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
            }

            redisService.delete("alarm:" + taskId);

        } catch (Exception e) {
            logger.error("【动态调度】标记任务失败，ID=" + taskId, e);
        }
    }

    public void cancelTaskSchedule(Long taskId) {
        delayTaskPublisher.cancel("medication.missed", String.valueOf(taskId));
        delayTaskPublisher.cancel("medication.remind", String.valueOf(taskId));
        redisService.delete("alarm:" + taskId);
        redisService.delete("remind:" + taskId);
        logger.debug("【动态调度】已取消任务ID={} 的延迟任务", taskId);
    }

    public void addTaskSchedule(MedicationTask task) {
        if (task.getTaskDate().equals(LocalDate.now())) {
            logger.info("【调度诊断】addTaskSchedule 被调用: taskId={}, taskDate={}, timePoint={}",
                    task.getId(), task.getTaskDate(), task.getTimePoint());
            scheduleTaskTimeout(task);
        }
    }

    public void cancelInviteExpireTask(Long applyId) {
        delayTaskPublisher.cancel("family.invite.expire", String.valueOf(applyId));
        logger.debug("【动态调度】已取消邀请 {} 的过期延迟任务", applyId);
    }

    public void addInviteExpireTask(Long applyId, LocalDateTime expireTime) {
        cancelInviteExpireTask(applyId);

        Instant expireAt = expireTime.atZone(ZoneId.systemDefault()).toInstant();
        DelayTask task = DelayTask.of("family.invite.expire", String.valueOf(applyId), expireAt);
        delayTaskPublisher.publish(task);
        logger.debug("【动态调度】邀请 {} 过期延迟任务已发布，将在 {} 执行", applyId, expireTime);
    }

    private void handleRemindBroadcast(Long taskId, Long userId, String medicineName) {
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "medicine_remind");
        pushData.put("medicineName", medicineName);
        pushData.put("remindTime", LocalDateTime.now().format(formatter));

        DomainEvent remindEvent = DomainEvent.of("medication.remind", "MedicationTask", String.valueOf(taskId));
        remindEvent.setUserId(userId);
        remindEvent.setPayload(pushData);
        domainEventPublisher.publish(remindEvent);
    }
}
