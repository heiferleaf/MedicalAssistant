package com.whu.medicalbackend.common.infra.delay.handler;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.enumField.EventLogEnum;
import com.whu.medicalbackend.common.infra.delay.DelayTask;
import com.whu.medicalbackend.common.infra.delay.DelayTaskHandler;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.family.mapper.FamilyEventLogMapper;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.medical.entity.MedicationTask;
import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.medical.mapper.MedicationTaskMapper;
import com.whu.medicalbackend.medical.mapper.MedicineMapper;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.DomainEventPublisher;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.whu.medicalbackend.common.schedule.DynamicTaskScheduler.formatter;

@Component
public class MedicationMissedHandler implements DelayTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(MedicationMissedHandler.class);

    private final MedicationTaskMapper taskMapper;
    private final FamilyMemberMapper memberMapper;
    private final MedicineMapper medicineMapper;
    private final FamilyEventLogMapper eventLogMapper;
    private final UserMapper userMapper;
    private final RedisService redisService;
    private final DomainEventPublisher domainEventPublisher;

    public MedicationMissedHandler(MedicationTaskMapper taskMapper,
                                  FamilyMemberMapper memberMapper,
                                  MedicineMapper medicineMapper,
                                  FamilyEventLogMapper eventLogMapper,
                                  UserMapper userMapper,
                                  RedisService redisService,
                                  DomainEventPublisher domainEventPublisher) {
        this.taskMapper = taskMapper;
        this.memberMapper = memberMapper;
        this.medicineMapper = medicineMapper;
        this.eventLogMapper = eventLogMapper;
        this.userMapper = userMapper;
        this.redisService = redisService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public boolean supports(String taskType) {
        return "medication.missed".equals(taskType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(DelayTask task) {
        Long taskId = Long.parseLong(task.getBizId());

        MedicationTask medTask = taskMapper.findById(taskId);
        if (medTask == null || medTask.getStatus() != 0) {
            return;
        }

        taskMapper.updateStatus(taskId, 2, null);

        medTask = taskMapper.findById(taskId);
        if (medTask != null) {
            Long userId = medTask.getUserId();
            Long groupId = memberMapper.getGroupIdByUserId(userId);
            Medicine medicine = medicineMapper.findById(medTask.getMedicineId());
            if (userId != null && groupId != null && medicine != null) {
                eventLogMapper.insertLog(groupId, userId, EventLogEnum.ALARM_MISSED.name(), medicine.getName());
                String alarmKey = RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, LocalDate.now().toString());
                redisService.delete(alarmKey);

                User user = userMapper.findByUserId(userId);
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
    }
}
