package com.whu.medicalbackend.family.service.serviceImpl;

import com.whu.medicalbackend.family.dto.*;

import java.util.List;

public interface FamilyGroupService {
    FamilyCreateVO createGroup(Long userId, String groupName, String description);

    FamilyDetailVO getMyGroupDetails(Long userId);

    void applyJoin(Long userId, Long groupId, String remark);

    void approveApply(Long applyId, Long userId, String opType, String remark);

    void leaveGroup(Long groupId, Long userId);

    void inviteMember(Long leaderId, String inviteePhone, String remark);

    FamilyHealthSnapshotVO getFamilyHealthSnapshot(Long groupId);

    List<FamilyAlarmVO> getTodayAlarms(Long groupId);

    List<FamilyInviteApplyVO> getMyApplyRecords(Long userId);

    List<FamilyInviteApplyVO> getPendingApplies(Long groupId, Long userId);
}
