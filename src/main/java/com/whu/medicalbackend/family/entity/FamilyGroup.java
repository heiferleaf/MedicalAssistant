package com.whu.medicalbackend.family.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭组主表实体
 * 对应表：family_group
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyGroup implements Serializable {
    private static final long serialVersionUID = 6L;

    private Long id;
    private String groupName;
    private Long ownerUserId;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}