package com.whu.medicalbackend.family.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 家庭组申请/邀请记录详情 VO
 * 用于：
 * 1. 用户查看自己的申请/邀请历史
 * 2. 组长查看待审批列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInviteApplyVO {

    /** 记录ID (对应数据库 id) */
    private Long applyId;

    /** 家庭组ID */
    private Long groupId;

    /** 家庭组名称 (从 family_group 表关联获取) */
    private String groupName;

    /**
     * 发起人昵称
     * 如果是邀请，则为组长昵称；如果是申请，该字段可能为空或根据逻辑展示
     */
    private String inviterNickname;

    /**
     * 申请/被邀请人昵称 (对应 t_user 表)
     * 在组长审批列表展示时，显示是谁想进来
     */
    private String userNickname;

    /** 申请/被邀请人真实姓名/用户名 */
    private String userName;

    /**
     * 记录类型
     * apply: 用户申请入组
     * invite: 组长邀请用户
     */
    private String type;

    /**
     * 状态
     * pending: 待处理
     * accepted: 已同意
     * rejected: 已拒绝
     * expired: 已过期
     */
    private String status;

    /** 备注信息 (用户申请时填写的理由或邀请备注) */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 处理时间 (同意或拒绝的时间) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dealTime;

    /** 过期时间 (通常为创建时间+48小时) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;
}