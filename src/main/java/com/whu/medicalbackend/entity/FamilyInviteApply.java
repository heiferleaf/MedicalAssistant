package com.whu.medicalbackend.entity;

import com.whu.medicalbackend.enumField.InviteStatus;
import com.whu.medicalbackend.enumField.InviteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭申请与邀请记录实体
 * 对应表：family_invite_apply
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInviteApply implements Serializable {
    private static final long serialVersionUID = 7L;

    private Long id;
    private Long groupId;
    private Long inviterId;    // 申请时为null，邀请时为发起人ID
    private Long inviteeId;    // 申请时为发起人ID，邀请时为被邀请人ID
    private InviteType type;       // 'apply'-申请, 'invite'-邀请
    private InviteStatus status;     // 'pending', 'accepted', 'rejected', 'expired'
    private LocalDateTime createTime;
    private LocalDateTime dealTime;
    private LocalDateTime expireTime;
    private String remark;
}