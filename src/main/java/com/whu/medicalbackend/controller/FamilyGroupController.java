package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.dto.*;
import com.whu.medicalbackend.service.FamilyGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family/group")
public class FamilyGroupController {

    @Autowired private FamilyGroupService familyGroupService;

    /**
     * 1.1 创建家庭组
     */
    @PostMapping
    public Result<FamilyCreateVO> createGroup(@RequestAttribute("userId") Long userId, @RequestBody Map<String, String> body) {
        String groupName = body.get("groupName");
        String description = body.get("description");

        FamilyCreateVO familyCreateVO = familyGroupService.createGroup(userId, groupName, description);
        return Result.success(familyCreateVO);
    }

    /**
     * 1.2 查询本人所在家庭组及成员
     */
    @GetMapping("/me")
    public Result<FamilyDetailVO> getMyGroup(@RequestAttribute("userId") Long userId) {
        FamilyDetailVO familyDetailVO = familyGroupService.getMyGroupDetails(userId);
        return Result.success(familyDetailVO);
    }

    /**
     * 1.3 申请加入家庭组
     */
    @PostMapping("/{groupId}/apply")
    public Result<Void> applyJoin(@RequestAttribute("userId") Long userId,
                                  @PathVariable Long groupId,
                                  @RequestBody(required = false) Map<String, String> params) {
        String remark = params != null ? params.getOrDefault("remark", "申请加入") : "申请加入";
        familyGroupService.applyJoin(userId, groupId, remark);
        return Result.success();
    }

    /**
     * 1.4 邀请成员
     */
    @PostMapping("/{groupId}/invite")
    public Result<Void> inviteMember(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long groupId,
                                     @RequestParam String phoneNumber,
                                     @RequestBody Map<String, String> params) {
        String remark = params.getOrDefault("remark", "邀请你加入家庭");
        familyGroupService.inviteMember(userId, phoneNumber, remark);
        return Result.success();
    }

    /**
     * 1.5 & 1.6 审批/处理（申请或邀请）
     * 说明：Service 的 approveApply 统一处理了同意和拒绝逻辑
     */
    @PostMapping("/{groupId}/approve")
    public Result<Void> approve(@PathVariable Long groupId,
                                @RequestBody Map<String, Object> params){
        Long applyId = Long.valueOf(params.get("applyId").toString());
        String opType = params.get("opType").toString(); // "accept" or "reject"
        String remark = (String) params.get("remark");

        familyGroupService.approveApply(applyId, opType, remark);
        return Result.success();
    }

    /**
     * 1.7 成员主动退出
     */
    @PostMapping("/{groupId}/quit")
    public Result<Void> quitGroup(@RequestAttribute("userId") Long userId,
                                  @PathVariable Long groupId) {
        familyGroupService.leaveGroup(groupId, userId);
        return Result.success();
    }

    /**
     * 2.1 查看本人申请/邀请历史
     * 需要 Service 补充 getMyApplyRecords 方法
     */
    @GetMapping("/my/apply-records")
    public Result<List<FamilyInviteApplyVO>> getMyApplyRecords(@RequestAttribute("userId") Long userId) {
        // List<FamilyInviteApplyVO> list = familyGroupService.getMyApplyRecords(userId);
        // return Result.success(list);
        return Result.error(500, "Service待实现");
    }

    /**
     * 2.2 查看本人收到的邀请
     * 需要 Service 补充 getMyInviteRecords 方法
     */
    @GetMapping("/my/invite-records")
    public Result<List<FamilyInviteApplyVO>> getMyInviteRecords(@RequestAttribute("userId") Long userId) {
        // List<FamilyInviteApplyVO> list = familyGroupService.getMyInviteRecords(userId);
        return Result.error(500, "Service待实现");
    }

    /**
     * 2.3 组长审批待处理列表
     * 需要 Service 补充 getPendingApplies 方法
     */
    @GetMapping("/{groupId}/pending-applies")
    public Result<List<FamilyInviteApplyVO>> getPendingApplies(@PathVariable Long groupId) {
        // List<FamilyInviteApplyVO> list = familyGroupService.getPendingApplies(groupId);
        return Result.error(500, "Service待实现");
    }

    /**
     * 3.1 查询家庭组成员健康与用药数据 (今日快照)
     */
    @GetMapping("/{groupId}/health")
    public Result<FamilyHealthSnapshotVO> getHealthSnapshot(@PathVariable Long groupId) {
        FamilyHealthSnapshotVO vo = familyGroupService.getFamilyHealthSnapshot(groupId);
        return Result.success(vo);
    }

    /**
     * 3.2 查询当天家庭组健康异常告警
     */
    @GetMapping("/{groupId}/alarms")
    public Result<List<FamilyAlarmVO>> getTodayAlarms(@PathVariable Long groupId) {
        List<FamilyAlarmVO> list = familyGroupService.getTodayAlarms(groupId);
        return Result.success(list);
    }
}
