package com.whu.medicalbackend.agent.langchain4j.tools.family;

import com.whu.medicalbackend.family.dto.FamilyDetailVO;
import com.whu.medicalbackend.family.service.serviceImpl.FamilyGroupService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FamilyQueryTool {

    private static final Logger logger = LoggerFactory.getLogger(FamilyQueryTool.class);

    private final FamilyGroupService familyGroupService;

    public FamilyQueryTool(FamilyGroupService familyGroupService) {
        this.familyGroupService = familyGroupService;
    }

    @Tool("查询用户所在的家庭组及其成员信息")
    public String queryMyFamilyGroup(@ToolMemoryId String memoryId) {
        try {
            Long userId = parseUserId(memoryId);
            logger.info("[queryMyFamilyGroup] userId={}", userId);

            FamilyDetailVO result = familyGroupService.getMyGroupDetails(userId);

            if (result == null || result.getGroup() == null) {
                return "您还没有加入任何家庭组。您可以创建一个家庭组。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("家庭组名称: ").append(result.getGroup().getGroupName()).append("\n");
            sb.append("家庭组描述: ")
                    .append(result.getGroup().getDescription() != null ? result.getGroup().getDescription() : "无")
                    .append("\n");
            sb.append("组长: ").append(result.getGroup().getOwnerUserNickname()).append("\n\n");
            sb.append("家庭成员:\n");

            if (result.getMembers() != null && !result.getMembers().isEmpty()) {
                for (int i = 0; i < result.getMembers().size(); i++) {
                    sb.append((i + 1)).append(". ").append(result.getMembers().get(i).getUserNickname());
                    if (result.getMembers().get(i).getRole() != null
                            && result.getMembers().get(i).getRole().equals("leader")) {
                        sb.append(" (组长)");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("暂无成员\n");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("[queryMyFamilyGroup] 执行失败", e);
            return "查询家庭组失败：" + e.getMessage();
        }
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
