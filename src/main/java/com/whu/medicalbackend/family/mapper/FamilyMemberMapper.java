package com.whu.medicalbackend.family.mapper;

import com.whu.medicalbackend.family.entity.FamilyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FamilyMemberMapper{

    /**
     * 用户是否已经存在于某一个家庭组中
     *
     * @param userId
     * @return
     */
    boolean checkUserInGroup(@Param("userId") Long userId);

    /**
     * 获取用户所在的家庭组Id
     *
     * @param userId
     * @return
     */
    Long getGroupIdByUserId(@Param("userId") Long userId);

    /**
     * 插入家庭组新成员
     *
     * @param member
     * @return
     */
    int insert(FamilyMember member);

    /**
     * 获取指定家庭组的激活成员
     *
     * @param groupId
     * @return
     */
    List<Long> findActiveUserIdsByGroupId(@Param("groupId") Long groupId);

    /**
     * 成员退出
     *
     * @param groupId
     * @param userId
     * @param status
     * @param exitTime
     * @return
     */
    int updateMemberStatus(@Param("groupId") Long groupId, @Param("userId") Long userId,
                           @Param("status") String status, @Param("exitTime") LocalDateTime exitTime);

    /**
     * 获取成员在家庭中的身份 leader / normal
     *
     * @param groupId
     * @param userId
     * @return
     */
    String getRoleInGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);

}