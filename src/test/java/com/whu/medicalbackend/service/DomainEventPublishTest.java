package com.whu.medicalbackend.service;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import com.whu.medicalbackend.common.infra.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("monolith")
class DomainEventPublishTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Test
    void should_PublishEventSuccessfully() {
        DomainEvent event = DomainEvent.of("medication.alarm", "MedicationTask", "123");
        event.setUserId(1L);
        event.setGroupId(10L);
        event.getPayload().put("medicineName", "TestMedicine");
        event.getPayload().put("alarmTime", "2026-05-16 18:00:00");

        assertDoesNotThrow(() -> domainEventPublisher.publish(event));
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void should_CreateEventWithCorrectMetadata() {
        DomainEvent event = DomainEvent.of("medication.updated", "MedicationTask", "456");
        event.setUserId(2L);
        event.setGroupId(20L);

        assertEquals("medication.updated", event.getEventType());
        assertEquals("MedicationTask", event.getAggregateType());
        assertEquals("456", event.getAggregateId());
        assertEquals(2L, event.getUserId());
        assertEquals(20L, event.getGroupId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }
}
