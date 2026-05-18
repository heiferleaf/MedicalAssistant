package com.whu.medicalbackend.common.infra.event.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEventAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private Long userId;
    private Long groupId;
    private String payload;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
