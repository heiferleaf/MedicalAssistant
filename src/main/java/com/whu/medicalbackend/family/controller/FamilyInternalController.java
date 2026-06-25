package com.whu.medicalbackend.family.controller;

import com.whu.medicalbackend.common.client.dto.FamilyMemberDTO;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.family.mapper.FamilyEventLogMapper;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭成员内部 API 控制器
 *
 * 提供家庭成员信息查询接口供其他微服务调用
 * 路径前缀：/internal/family
 *
 * 注意：此接口仅供内部服务调用，不对外暴露
 *
 * @author Medical Assistant Team
 */
@RestController
@RequestMapping("/internal/family")
public class FamilyInternalController {

    private static final Logger logger = LoggerFactory.getLogger(FamilyInternalController.class);

    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    private FamilyEventLogMapper familyEventLogMapper;

    /**
     * 根据用户 ID 查询家庭成员信息
     *
     * @param userId 用户 ID
     * @return 家庭成员信息
     */
    @GetMapping("/member/by-user/{userId}")
    public Result<FamilyMemberDTO> getMemberByUserId(@PathVariable Long userId) {
        logger.debug("内部调用: 查询家庭成员信息, userId={}", userId);

        // 先检查用户是否在家庭组中
        if (!familyMemberMapper.checkUserInGroup(userId)) {
            return Result.error(404, "该用户不是任何家庭组成员");
        }

        Long groupId = familyMemberMapper.getGroupIdByUserId(userId);
        if (groupId == null) {
            return Result.error(404, "该用户不是任何家庭组成员");
        }

        FamilyMemberDTO dto = new FamilyMemberDTO();
        dto.setUserId(userId);
        dto.setGroupId(groupId);
        dto.setRole(familyMemberMapper.getRoleInGroup(groupId, userId).equals("leader") ? 1 : 0);

        return Result.success(dto);
    }

    /**
     * 根据家庭组 ID 查询所有活跃成员的用户 ID
     *
     * @param groupId 家庭组 ID
     * @return 活跃成员用户 ID 列表
     */
    @GetMapping("/group/{groupId}/active-members")
    public Result<List<Long>> getActiveMembers(@PathVariable Long groupId) {
        logger.debug("内部调用: 查询家庭组活跃成员, groupId={}", groupId);

        List<Long> userIds = familyMemberMapper.findActiveUserIdsByGroupId(groupId);
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(List.of());
        }

        return Result.success(userIds);
    }

    /**
     * 检查用户是否在某个家庭组中
     *
     * @param userId 用户 ID
     * @return 是否在家庭组中
     */
    @GetMapping("/member/{userId}/in-group")
    public Result<Boolean> checkUserInGroup(@PathVariable Long userId) {
        logger.debug("内部调用: 检查用户是否在家庭组中, userId={}", userId);

        boolean inGroup = familyMemberMapper.checkUserInGroup(userId);
        return Result.success(inGroup);
    }

    /**
     * 获取用户所在的家庭组 ID
     *
     * @param userId 用户 ID
     * @return 家庭组 ID
     */
    @GetMapping("/member/{userId}/group-id")
    public Result<Long> getGroupIdByUserId(@PathVariable Long userId) {
        logger.debug("内部调用: 获取用户所在家庭组ID, userId={}", userId);

        Long groupId = familyMemberMapper.getGroupIdByUserId(userId);
        if (groupId == null) {
            return Result.error(404, "用户不在任何家庭组中");
        }

        return Result.success(groupId);
    }

    @PostMapping("/event-log")
    public Result<Void> insertEventLog(
            @RequestParam Long groupId,
            @RequestParam Long userId,
            @RequestParam String eventType,
            @RequestParam String eventContent) {
        familyEventLogMapper.insertLog(groupId, userId, eventType, eventContent);
        return Result.success();
    }
}
