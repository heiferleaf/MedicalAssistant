package com.whu.medicalbackend.medical.controller;

import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.medical.dto.MedicineCreateDTO;
import com.whu.medicalbackend.medical.dto.MedicineVO;
import com.whu.medicalbackend.medical.dto.PlanFromMedicineDTO;
import com.whu.medicalbackend.medical.dto.PlanVO;
import com.whu.medicalbackend.medical.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicine")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    /**
     * 查询药箱（当前用户的全部药品）
     * GET /api/medicine
     */
    @GetMapping
    public Result<List<MedicineVO>> getMedicineList(
            @RequestAttribute("userId") Long userId) {
        List<MedicineVO> list = medicineService.getMedicineList(userId);
        return Result.success(list);
    }

    /**
     * 新增药品
     * POST /api/medicine
     * Body: {"name":"布洛芬", "defaultDosage":"2粒", "remark":"饭后服���"}
     */
    @PostMapping
    public Result<MedicineVO> addMedicine(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody MedicineCreateDTO dto) {
        MedicineVO vo = medicineService.addMedicine(userId, dto);
        return Result.success("添加成功", vo);
    }

    /**
     * 编辑药品
     * PUT /api/medicine/{medicineId}
     * Body: {"name":"布洛芬", "defaultDosage":"1粒", "remark":""}
     */
    @PutMapping("/{medicineId}")
    public Result<MedicineVO> updateMedicine(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long medicineId,
            @Valid @RequestBody MedicineCreateDTO dto) {
        MedicineVO vo = medicineService.updateMedicine(userId, medicineId, dto);
        return Result.success("编辑成功", vo);
    }

    /**
     * 删除药品
     * DELETE /api/medicine/{medicineId}
     */
    @DeleteMapping("/{medicineId}")
    public Result<Void> deleteMedicine(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long medicineId) {
        medicineService.deleteMedicine(userId, medicineId);
        return Result.success("删除成功", null);
    }

    /**
     * 从药箱快速创建用药计划
     * POST /api/medicine/{medicineId}/plan
     * Body: {"dosage":"2粒","startDate":"2026-03-12","timePoints":["08:00","20:00"]}
     *
     * 前端流程：
     *   计划界面 → "从药箱快速添加计划" → 跳转药箱界面 →
     *   选中某药品点击"创建计划" → 调用此接口
     */
    @PostMapping("/{medicineId}/plan")
    public Result<PlanVO> createPlanFromMedicine(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long medicineId,
            @Valid @RequestBody PlanFromMedicineDTO dto) {
        PlanVO planVO = medicineService.createPlanFromMedicine(userId, medicineId, dto);
        return Result.success("计划创建成功", planVO);
    }
}
