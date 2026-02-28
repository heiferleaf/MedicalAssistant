package com.whu.medicalbackend.mapper;

import com. whu.medicalbackend. entity.MedicationTask;
import org.apache.ibatis. annotations.Mapper;
import org.apache.ibatis.annotations. Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java. util.List;

/**
 * 用药任务Mapper接口
 */
@Mapper
public interface MedicationTaskMapper {

    /**
     * 根据ID查询任务
     */
    MedicationTask findById(@Param("id") Long id);

    /**
     * 查询用户指定日期的任务列表
     */
    List<MedicationTask> findByUserIdAndDate(@Param("userId") Long userId,
                                             @Param("taskDate") LocalDate taskDate);

    /**
     * 历史任务查询（支持多条件筛选）
     * MyBatis知识点：动态SQL（<if>、<where>标签）
     */
    List<MedicationTask> findHistory(@Param("userId") Long userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("medicineName") String medicineName,
                                     @Param("status") Integer status);

    /**
     * 批量插入任务
     * MyBatis知识点：动态SQL的foreach标签
     */
    int batchInsert(@Param("tasks") List<MedicationTask> tasks);

    /**
     * 更新任务状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("operateTime") LocalDateTime operateTime);

    /**
     * 删除计划的未来任务（用于编辑/删除计划时）
     * 删除条件：plan_id = ?  AND task_date >= ?
     */
    int deleteFutureTasks(@Param("planId") Long planId,
                          @Param("fromDate") LocalDate fromDate);

    /**
     * 查询需要标记为漏服的任务
     * 条件：status=0（未服用）AND task_date=今天 AND time_point < 当前时间-30分钟
     */
    List<MedicationTask> findTasksToMarkAsMissed(@Param("currentDate") LocalDate currentDate,
                                                 @Param("currentTime") LocalDateTime currentTime);

    /**
     * 批量更新任务状态为漏服
     */
    int batchUpdateToMissed(@Param("ids") List<Long> ids);

    /**
     * 查询指定日期所有未完成的任务（用于动态调度）
     */
    List<MedicationTask> findUncompletedTasksByDate(@Param("taskDate") LocalDate taskDate);

    /**
     * 统计当日特定状态的任务数
     */
    int countStatusByDate(@Param("userId") Long userId,
                          @Param("date") LocalDate date,
                          @Param("status") Integer status);

    /**
     * 统计当日总任务数
     */
    int countTotalByDate(@Param("userId") Long userId,
                         @Param("date") LocalDate date);
}