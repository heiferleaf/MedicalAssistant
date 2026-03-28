package com.whu.medicalbackend.medical.service;

import com.whu.medicalbackend.medical.dto.PlanCreateDTO;
import com.whu.medicalbackend.medical.dto.PlanVO;

import java.util.List;

/**
 * 用药计划服务接口
 */
public interface PlanService {

    /**
     * 获取用户的所有计划
     */
    List<PlanVO> getPlanList(Long userId);

    /**
     * 创建用药计划
     * 业务逻辑：
     * 1. 查找或创建药品
     * 2. 创建计划
     * 3. 如果开始日期是今天，生成今天的任务
     */
    PlanVO createPlan(Long userId, PlanCreateDTO dto);

    /**
     * 编辑用药计划
     * 业务逻辑：
     * 1. 查询计划是否存在
     * 2. 验证权限
     * 3. 删除未来任务（从明天开始）
     * 4. 更新计划
     * 5. 重新生成未来任务
     */
    void updatePlan(Long userId, Long planId, PlanCreateDTO dto);

    /**
     * 删除用药计划
     * 业务逻辑：
     * 1. 软删除计划
     * 2. 删除未来任务（从今天开始）
     */
    void deletePlan(Long userId, Long planId);
}