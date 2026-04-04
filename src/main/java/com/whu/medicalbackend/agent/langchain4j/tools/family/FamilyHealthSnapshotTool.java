package com.whu.medicalbackend.agent.langchain4j.tools.family;

import com.whu.medicalbackend.family.dto.FamilyDetailVO;
import com.whu.medicalbackend.family.dto.FamilyHealthSnapshotVO;
import com.whu.medicalbackend.family.service.serviceImpl.FamilyGroupService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FamilyHealthSnapshotTool {

    private static final Logger logger = LoggerFactory.getLogger(FamilyHealthSnapshotTool.class);

    private final FamilyGroupService familyGroupService;

    public FamilyHealthSnapshotTool(FamilyGroupService familyGroupService) {
        this.familyGroupService = familyGroupService;
    }

    @Tool("查询家庭组成员的健康与用药数据（今日快照）")
    public String getFamilyHealthSnapshot(@ToolMemoryId String memoryId) {
        try {
            Long userId = parseUserId(memoryId);
            logger.info("[getFamilyHealthSnapshot] userId={}", userId);

            FamilyDetailVO myGroup = familyGroupService.getMyGroupDetails(userId);
            if (myGroup == null || myGroup.getGroup() == null) {
                return "您还没有加入任何家庭组。";
            }

            Long groupId = myGroup.getGroup().getGroupId();
            FamilyHealthSnapshotVO snapshot = familyGroupService.getFamilyHealthSnapshot(groupId);

            if (snapshot == null || snapshot.getMembers() == null || snapshot.getMembers().isEmpty()) {
                return "暂无家庭健康数据。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("家庭健康快照（").append(snapshot.getUpdateTime()).append("）\n\n");
            sb.append("成员今日用药情况:\n");

            for (int i = 0; i < snapshot.getMembers().size(); i++) {
                FamilyHealthSnapshotVO.MemberHealthDetail member = snapshot.getMembers().get(i);
                sb.append((i + 1)).append(". ").append(member.getNickname()).append("\n");
                sb.append("   今日任务: ").append(member.getCompletedTasks()).append("/").append(member.getTotalTasks()).append(" 已完成\n");
                if (member.getHeartRate() != null) {
                    sb.append("   最近心率: ").append(member.getHeartRate()).append(" 次/分\n");
                }
                if (member.getBloodOxygen() != null) {
                    sb.append("   最近血氧: ").append(member.getBloodOxygen()).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("[getFamilyHealthSnapshot] 执行失败", e);
            return "查询家庭健康快照失败：" + e.getMessage();
        }
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
