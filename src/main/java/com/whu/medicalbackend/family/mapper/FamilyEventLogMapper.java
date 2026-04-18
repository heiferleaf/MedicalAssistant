package com.whu.medicalbackend.family.mapper;

import com.whu.medicalbackend.family.dto.FamilyAlarmVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FamilyEventLogMapper {

    /**
     * 插入事件日志
     */
    int insertLog(@Param("groupId") Long groupId,
                  @Param("userId") Long userId,
                  @Param("eventType") String eventType,
                  @Param("eventContent") String eventContent);

    List<FamilyAlarmVO> findDailyAlarms(@Param("groupId") Long groupId);
}