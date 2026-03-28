package com.whu.medicalbackend.family.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭组成员实体类
 * 对应表：family_member
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember implements Serializable {
    private static final long serialVersionUID = 5L;

    /**
     * 唯一主键
     */
    private Long id;

    /**
     * 属于哪个家庭组 (对应 family_group.id)
     */
    private Long groupId;

    /**
     * 用户ID (对应 t_user.id)
     */
    private Long userId;

    /**
     * 成员角色：'leader' 表示组长，'normal' 表示普通组员
     */
    private String role;

    /**
     * 成员加入的时间
     */
    private LocalDateTime joinTime;

    /**
     * 当前状态：'active' 表示在组，'quit' 表示已退出
     */
    private String status;

    /**
     * 如果已退出，记录退出的时间；未退出则为 null
     */
    private LocalDateTime exitTime;

    /**
     * 逻辑删除标识：0=正常，1=删除
     */
    private Integer isDeleted;
}