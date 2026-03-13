package com.whu.medicalbackend.service;

import com.whu.medicalbackend.entity.Medicine;

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

    /**
     * 查询用户的所有药品
     *
     * @param userId 用户ID
     * @return 药品列表
     */
    List<Medicine> getMedicinesByUserId(Long userId);
}