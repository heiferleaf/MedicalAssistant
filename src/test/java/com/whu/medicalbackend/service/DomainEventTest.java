package com.whu.medicalbackend.service;

import com.whu.medicalbackend.common.infra.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    @Test
    void should_CreateEventWithCorrectFields() {
        DomainEvent event = DomainEvent.of("medication.alarm", "MedicationTask", "123");
        event.setUserId(1L);
        event.setGroupId(10L);
        event.getPayload().put("medicineName", "TestMedicine");

        assertEquals("medication.alarm", event.getEventType());
        assertEquals("MedicationTask", event.getAggregateType());
        assertEquals("123", event.getAggregateId());
        assertEquals(1L, event.getUserId());
        assertEquals(10L, event.getGroupId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("TestMedicine", event.getPayload().get("medicineName"));
    }

    @Test
    void should_GenerateUniqueIdForEachEvent() {
        DomainEvent event1 = DomainEvent.of("medication.alarm", "MedicationTask", "123");
        DomainEvent event2 = DomainEvent.of("medication.alarm", "MedicationTask", "123");

        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    void should_SetTimestampOnCreation() {
        DomainEvent event = DomainEvent.of("medication.alarm", "MedicationTask", "123");

        assertNotNull(event.getOccurredAt());
    }
}
