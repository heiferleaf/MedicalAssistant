package com.whu.medicalbackend.common.infra.push;

import com.whu.medicalbackend.common.enumField.FamilyEventEnum;
import com.whu.medicalbackend.family.entity.FamilyGroup;
import com.whu.medicalbackend.family.mapper.FamilyGroupMapper;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.user.mapper.UserMapper;
import com.whu.medicalbackend.ws.event.FamilyPushEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class FamilyMedicineAlarmSmsHook implements FamilyPushEventHook {

    private final ObjectProvider<FamilyGroupMapper> familyGroupMapperProvider;
    private final ObjectProvider<UserMapper> userMapperProvider;

    public FamilyMedicineAlarmSmsHook(
            ObjectProvider<FamilyGroupMapper> familyGroupMapperProvider,
            ObjectProvider<UserMapper> userMapperProvider
    ) {
        this.familyGroupMapperProvider = familyGroupMapperProvider;
        this.userMapperProvider = userMapperProvider;
    }

    @Override
    public void handle(FamilyPushEvent event) {
        if (event.getEventType() != FamilyEventEnum.MEDICINE_ALARM) {
            return;
        }

        FamilyGroupMapper groupMapper = familyGroupMapperProvider.getIfAvailable();
        UserMapper userMapper = userMapperProvider.getIfAvailable();
        if (groupMapper == null || userMapper == null) {
            log.debug("跳过短信通知 hook: 当前服务未加载家庭组或用户 Mapper, groupId={}", event.getGroupId());
            return;
        }

        // 保留旧 SmsHandle 的 owner 手机号查找路径，后续接入短信网关时只需要替换这里的日志动作。
        Optional<String> phone = Optional.ofNullable(groupMapper.selectById(event.getGroupId()))
                .map(FamilyGroup::getOwnerUserId)
                .map(userMapper::findByUserId)
                .map(User::getPhoneNumber);

        phone.ifPresent(value -> log.info("家庭组服药告警短信待发送: groupId={}, ownerPhone={}",
                event.getGroupId(), value));
    }
}
