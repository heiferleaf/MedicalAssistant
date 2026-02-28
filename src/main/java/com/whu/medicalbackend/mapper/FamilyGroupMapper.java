package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.FamilyGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FamilyGroupMapper {

    /**
     * 插入新家庭组
     */
    int insert(FamilyGroup group);

    /**
     * 根据ID查询家庭组详情
     */
    FamilyGroup selectById(@Param("id") Long groupId);

    /**
     * 校验组长是否已经拥有一个激活状态的组
     */
    boolean existsByOwnerId(@Param("ownerId") Long ownerId);
}