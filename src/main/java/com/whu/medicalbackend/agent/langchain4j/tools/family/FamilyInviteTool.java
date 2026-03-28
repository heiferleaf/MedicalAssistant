package com.whu.medicalbackend.agent.langchain4j.tools.family;

import com.whu.medicalbackend.family.dto.FamilyDetailVO;
import com.whu.medicalbackend.family.service.serviceImpl.FamilyGroupService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FamilyInviteTool {

    private static final Logger logger = LoggerFactory.getLogger(FamilyInviteTool.class);

    private final FamilyGroupService familyGroupService;

    public FamilyInviteTool(FamilyGroupService familyGroupService) {
        this.familyGroupService = familyGroupService;
    }

    @Tool("通过手机号邀请成员加入家庭组，需要提供手机号码")
    public String inviteFamilyMember(@ToolMemoryId String memoryId, String phoneNumber, String remark) {
        try {
            Long userId = parseUserId(memoryId);
            logger.info("[inviteFamilyMember] userId={}, phoneNumber={}", userId, phoneNumber);

            FamilyDetailVO myGroup = familyGroupService.getMyGroupDetails(userId);
            if (myGroup == null || myGroup.getGroup() == null) {
                return "您还没有加入任何家庭组，请先创建一个家庭组。";
            }

            if (remark == null || remark.isBlank()) {
                remark = "邀请你加入家庭";
            }

            familyGroupService.inviteMember(userId, phoneNumber, remark);

            return "已成功向 " + phoneNumber + " 发送家庭组邀请！";
        } catch (Exception e) {
            logger.error("[inviteFamilyMember] 执行失败", e);
            return "邀请成员失败：" + e.getMessage();
        }
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
