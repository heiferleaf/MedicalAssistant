package com.whu.medicalbackend.common.infra.event.hooks;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import com.whu.medicalbackend.family.service.FamilyCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class FamilyMemberChangedHook implements DomainEventHook {

    private final ObjectProvider<FamilyCacheService> familyCacheServiceProvider;

    public FamilyMemberChangedHook(ObjectProvider<FamilyCacheService> familyCacheServiceProvider) {
        this.familyCacheServiceProvider = familyCacheServiceProvider;
    }

    @Override
    public String hookName() {
        return "familyMemberChanged";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return "family.member.changed".equals(event.getEventType());
    }

    @Override
    public void handle(DomainEvent event) {
        Long groupId = event.getGroupId();
        if (groupId == null) {
            log.warn("FamilyMemberChangedHook: groupId 为空, eventId={}", event.getEventId());
            return;
        }

        FamilyCacheService cacheService = familyCacheServiceProvider.getIfAvailable();
        if (cacheService == null) {
            log.debug("FamilyMemberChangedHook: CacheService 不可用，跳过, groupId={}", groupId);
            return;
        }

        cacheService.refreshMemberCache(groupId);
        log.info("FamilyMemberChangedHook: 家庭成员缓存已刷新, groupId={}, eventId={}", groupId, event.getEventId());
    }
}
