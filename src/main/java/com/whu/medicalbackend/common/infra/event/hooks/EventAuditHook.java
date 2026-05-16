package com.whu.medicalbackend.common.infra.event.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.audit.DomainEventAuditLog;
import com.whu.medicalbackend.common.infra.event.audit.DomainEventAuditMapper;
import com.whu.medicalbackend.common.infra.hook.DomainEventHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class EventAuditHook implements DomainEventHook {

    private final ObjectProvider<DomainEventAuditMapper> auditMapperProvider;
    private final ObjectProvider<ObjectMapper> objectMapperProvider;

    public EventAuditHook(ObjectProvider<DomainEventAuditMapper> auditMapperProvider,
                          ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.auditMapperProvider = auditMapperProvider;
        this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    public String hookName() {
        return "eventAudit";
    }

    @Override
    public boolean supports(DomainEvent event) {
        return true;
    }

    @Override
    public void handle(DomainEvent event) {
        DomainEventAuditMapper auditMapper = auditMapperProvider.getIfAvailable();
        if (auditMapper == null) {
            log.debug("EventAuditHook: 审计 Mapper 不可用，跳过");
            return;
        }

        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();

        DomainEventAuditLog auditLog = new DomainEventAuditLog();
        auditLog.setEventId(event.getEventId());
        auditLog.setEventType(event.getEventType());
        auditLog.setAggregateType(event.getAggregateType());
        auditLog.setAggregateId(event.getAggregateId());
        auditLog.setUserId(event.getUserId());
        auditLog.setGroupId(event.getGroupId());
        auditLog.setStatus("PROCESSED");

        if (objectMapper != null && event.getPayload() != null) {
            try {
                auditLog.setPayload(objectMapper.writeValueAsString(event.getPayload()));
            } catch (Exception e) {
                log.warn("EventAuditHook: 序列化 payload 失败, eventId={}", event.getEventId(), e);
            }
        }

        try {
            auditMapper.insert(auditLog);
            log.debug("EventAuditHook: 事件审计记录已写入, eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("EventAuditHook: 写入审计记录失败, eventId={}", event.getEventId(), e);
        }
    }
}
