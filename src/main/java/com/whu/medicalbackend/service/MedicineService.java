package com.whu.medicalbackend.service;

import com.whu.medicalbackend.dto.MedicineCreateDTO;
import com.whu.medicalbackend.dto.MedicineVO;
import com.whu.medicalbackend.dto.PlanFromMedicineDTO;
import com.whu.medicalbackend.dto.PlanVO;
import com. whu.medicalbackend. entity.Medicine;

import java.util.List;

/**
 * 药品服务接口
 *
 * 设计模式：
 * - 接口隔离原则：只暴露必要的方法
 * - 依赖倒置原则：上层依赖接口，不依赖实现
 */
public interface MedicineService {

    /**
     * 查找或创建药品
     * （如果用户已有同名药品，返回现有；否则创建新药品）
     *
     * @param userId 用户ID
     * @param name 药品名称
     * @param defaultDosage 默认剂量（可空）
     * @return 药品对象
     */
    Medicine findOrCreate(Long userId, String name, String defaultDosage);

    /** 查询当前用户的全部药品 */
    List<MedicineVO> getMedicineList(Long userId);

    /** 新增药品 */
    MedicineVO addMedicine(Long userId, MedicineCreateDTO dto);

    /** 编辑药品 */
    MedicineVO updateMedicine(Long userId, Long medicineId, MedicineCreateDTO dto);

    /** 删除药品 */
    void deleteMedicine(Long userId, Long medicineId);

    /**
     * 从药箱快速创建计划
     * 找到对应药品后，委托 PlanService 完成计划创建
     */
    PlanVO createPlanFromMedicine(Long userId, Long medicineId, PlanFromMedicineDTO dto);
}