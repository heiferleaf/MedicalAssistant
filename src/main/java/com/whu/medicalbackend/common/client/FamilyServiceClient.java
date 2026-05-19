package com.whu.medicalbackend.common.client;

import org.springframework.stereotype.Service;

@Service
public class FamilyServiceClient {
    
    public Long getGroupIdByUserId(Long userId) {
        return 1L;
    }
    
    public void insertEventLog(Long groupId, Long userId, String eventType, String medicineName) {
        // mock implementation
    }
}
