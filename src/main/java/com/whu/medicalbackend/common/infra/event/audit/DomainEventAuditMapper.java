package com.whu.medicalbackend.common.infra.event.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainEventAuditMapper {

    int insert(DomainEventAuditLog log);

    int updateStatus(@Param("eventId") String eventId,
                     @Param("status") String status);

    DomainEventAuditLog findByEventId(@Param("eventId") String eventId);
}
