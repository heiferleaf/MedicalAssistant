package com.whu.medicalbackend.medical.mapper;

import com.whu.medicalbackend.medical.entity.MedicationPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MedicationPlanMapper{
    /**
     * 根据ID查询计划
     */
    MedicationPlan findById(@Param("id") Long id);

    /**
     * 查询用户的所有有效计划（未删除）
     */
    List<MedicationPlan> findByUserId(@Param("userId") Long userId);

    /**
     * 查询指定日期有效的计划
     * （用于定时任务生成当天任务）
     * 条件：deleted=0 AND start_date <= 指定日期 AND (end_date IS NULL OR end_date >= 指定日期)
     */
    List<MedicationPlan> findActivePlansByDate(@Param("userId") Long userId,
                                               @Param("date") LocalDate date);

    /**
     * 查询指定药品下未删除的计划数量
     */
    int countActivePlansByMedicineId(@Param("medicineId") Long medicineId);

    /**
     * 插入计划
     */
    int insert(MedicationPlan plan);

    /**
     * 更新计划
     */
    int update(MedicationPlan plan);

    /**
     * 软删除计划
     */
    int softDelete(@Param("id") Long id);

    /**
     * 硬删除计划
     */
    int deleteById(@Param("id") Long id);
}
