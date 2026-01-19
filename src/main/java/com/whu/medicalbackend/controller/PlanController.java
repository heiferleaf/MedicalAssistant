package com.whu.medicalbackend.controller;

import com. whu.medicalbackend. common.Result;
import com.whu. medicalbackend.dto.PlanCreateDTO;
import com. whu.medicalbackend. dto.PlanVO;
import com.whu. medicalbackend.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用药计划控制器
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    @Autowired
    private PlanService planService;

    /**
     * 获取用药计划列表
     *
     * URL: GET /api/plan?userId=1
     */
    @GetMapping
    public Result<List<PlanVO>> getPlanList(@RequestParam Long userId) {
        List<PlanVO> plans = planService.getPlanList(userId);
        return Result.success(plans);
    }

    /**
     * 新增用药计划
     *
     * URL: POST /api/plan? userId=1
     * Body: {"medicineName":"xxx", "dosage":"2粒", ... }
     *
     */
    @PostMapping
    public Result<PlanVO> createPlan(@RequestParam Long userId,
                                     @Valid @RequestBody PlanCreateDTO dto) {
        PlanVO plan = planService.createPlan(userId, dto);
        return Result.success("创建成功", plan);
    }

    /**
     * 编辑用药计划
     *
     * URL: PUT /api/plan/123?userId=1
     * Body: {"medicineName":"xxx", ... }
     *
     * SpringMVC知识点：
     * - @PutMapping：处理PUT请求
     * - @PathVariable：绑定路径变量
     *   - URL中的{planId}会自动绑定到参数planId
     */
    @PutMapping("/{planId}")
    public Result<Void> updatePlan(@RequestParam Long userId,
                                   @PathVariable Long planId,
                                   @Valid @RequestBody PlanCreateDTO dto) {
        planService.updatePlan(userId, planId, dto);
        return Result.success("编辑成功", null);
    }

    /**
     * 删除用药计划
     *
     * URL: DELETE /api/plan/123?userId=1
     *
     * SpringMVC知识点：
     * - @DeleteMapping：处理DELETE请求
     */
    @DeleteMapping("/{planId}")
    public Result<Void> deletePlan(@RequestParam Long userId,
                                   @PathVariable Long planId) {
        planService.deletePlan(userId, planId);
        return Result.success("删除成功", null);
    }
}