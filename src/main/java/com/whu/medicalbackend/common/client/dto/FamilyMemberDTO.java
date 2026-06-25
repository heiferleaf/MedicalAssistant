package com.whu.medicalbackend.common.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 家庭成员信息 DTO（用于服务间传输）
 *
 * @author Medical Assistant Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成员 ID
     */
    private Long memberId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 家庭组 ID
     */
    private Long groupId;

    /**
     * 角色（0-普通成员，1-管理员）
     */
    private Integer role;

    /**
     * 关系（如：父亲、母亲、子女等）
     */
    private String relation;

    /**
     * 备注名
     */
    private String remarkName;

    /**
     * 是否活跃（0-否，1-是）
     */
    private Integer isActive;
}
