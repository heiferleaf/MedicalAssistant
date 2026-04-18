package com.whu.medicalbackend.ws.event;

import com.whu.medicalbackend.common.enumField.FamilyEventEnum;

import java.util.Map;

public class FamilyMemberUpdateEvent extends FamilyPushEvent{
    public FamilyMemberUpdateEvent(Object source, Long groupId, Map<String, Object> data) {
        super(source, FamilyEventEnum.MEMBER_UPDATE, groupId, data);
    }
}
