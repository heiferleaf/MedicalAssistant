package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import com.whu.medicalbackend.family.entity.FamilyGroup;
import com.whu.medicalbackend.family.mapper.FamilyGroupMapper;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(200)
public class MedicationAlarmHook implements DomainEventHook {

    private final ObjectProvider<FamilyGroupMapper> familyGroupMapperProvider;
    private final ObjectProvider<UserMapper> userMapperProvider;

    public MedicationAlarmHook(ObjectProvider<FamilyGroupMapper> familyGroupMapperProvider,
                               ObjectProvider<UserMapper> userMapperProvider) {
        this.familyGroupMapperProvider = familyGroupMapperProvider;
        this.userMapperProvider = userMapperProvider;
    }

    @Override
    public String hookName() {
        return "medicationAlarm";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return "medication.alarm".equals(event.getEventType());
    }

    @Override
    public void handle(DomainEvent event) {
        FamilyGroupMapper groupMapper = familyGroupMapperProvider.getIfAvailable();
        UserMapper userMapper = userMapperProvider.getIfAvailable();
        if (groupMapper == null || userMapper == null) {
            log.debug("MedicationAlarmHook: Mapper 不可用，跳过, groupId={}", event.getGroupId());
            return;
        }

        Optional<String> phone = Optional.ofNullable(groupMapper.selectById(event.getGroupId()))
                .map(FamilyGroup::getOwnerUserId)
                .map(userMapper::findByUserId)
                .map(User::getPhoneNumber);

        phone.ifPresent(value -> log.info("MedicationAlarmHook: 家庭组服药告警短信待发送, groupId={}, ownerPhone={}",
                event.getGroupId(), value));
    }
}
