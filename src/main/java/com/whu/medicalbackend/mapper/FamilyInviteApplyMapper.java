package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.dto.FamilyInviteApplyVO;
import com.whu.medicalbackend.entity.FamilyInviteApply;
import com.whu.medicalbackend.enumField.InviteType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FamilyInviteApplyMapper {

    int insert(FamilyInviteApply record);

    FamilyInviteApply selectById(@Param("id") Long id);

    /**
     * 查询是否存在有效的申请记录
     * 注意：type 参数现在建议传枚举类型
     */
    boolean hasPendingRecord(@Param("groupId") Long groupId,
                             @Param("inviteeId") Long inviteeId,
                             @Param("type") InviteType type);

    int updateStatus(FamilyInviteApply record);

    List<FamilyInviteApply> findMyApplies(@Param("userId") Long userId);

    List<FamilyInviteApplyVO> findUserRecords(@Param("userId") Long userId);

    List<FamilyInviteApplyVO> findPendingAppliesByGroupId(@Param("groupId") Long groupId);
}