package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import com.whu.medicalbackend.common.infra.push.WsPushCommand;
import com.whu.medicalbackend.common.infra.push.WsPushPublisher;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.family.service.FamilyCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@Order(100)
public class WsPushHook implements DomainEventHook {

    private final WsPushPublisher wsPushPublisher;
    private final ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider;
    private final ObjectProvider<FamilyCacheService> familyCacheServiceProvider;

    public WsPushHook(WsPushPublisher wsPushPublisher,
                      ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider,
                      ObjectProvider<FamilyCacheService> familyCacheServiceProvider) {
        this.wsPushPublisher = wsPushPublisher;
        this.familyMemberMapperProvider = familyMemberMapperProvider;
        this.familyCacheServiceProvider = familyCacheServiceProvider;
    }

    @Override
    public String hookName() {
        return "wsPush";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return true;
    }

    @Override
    public void handle(DomainEvent event) {
        Long groupId = event.getGroupId();
        Long userId = event.getUserId();

        if (groupId != null && groupId > 0) {
            handleGroupPush(event, groupId);
        } else if (userId != null && userId > 0) {
            handleUserPush(event, userId);
        } else {
            log.warn("WsPushHook: 跳过推送，groupId 和 userId 均为空, eventType={}", event.getEventType());
        }
    }

    private void handleGroupPush(DomainEvent event, Long groupId) {
        List<Long> userIds = resolveTargetUserIds(groupId);
        if (userIds.isEmpty()) {
            log.warn("WsPushHook: 家庭组无活跃成员, groupId={}, eventType={}", groupId, event.getEventType());
            return;
        }

        Map<String, Object> payload = buildPayload(event);
        for (Long userId : userIds) {
            wsPushPublisher.pushToUser(WsPushCommand.user(userId, groupId, payload));
        }
    }

    private void handleUserPush(DomainEvent event, Long userId) {
        Map<String, Object> payload = buildPayload(event);
        wsPushPublisher.pushToUser(WsPushCommand.user(userId, 0L, payload));
    }

    private List<Long> resolveTargetUserIds(Long groupId) {
        FamilyCacheService familyCacheService = familyCacheServiceProvider.getIfAvailable();
        if (familyCacheService != null) {
            Set<Object> cachedMemberIds = familyCacheService.getGroupMemberIds(groupId);
            if (cachedMemberIds != null && !cachedMemberIds.isEmpty()) {
                return cachedMemberIds.stream()
                        .map(String::valueOf)
                        .map(Long::valueOf)
                        .toList();
            }
        }

        FamilyMemberMapper memberMapper = familyMemberMapperProvider.getIfAvailable();
        if (memberMapper == null) {
            return List.of();
        }
        return memberMapper.findActiveUserIdsByGroupId(groupId);
    }

    private Map<String, Object> buildPayload(DomainEvent event) {
        Map<String, Object> payload = new HashMap<>();
        if (event.getPayload() != null) {
            payload.putAll(event.getPayload());
        }
        payload.putIfAbsent("eventType", event.getEventType());
        payload.putIfAbsent("eventId", event.getEventId());
        return payload;
    }
}
