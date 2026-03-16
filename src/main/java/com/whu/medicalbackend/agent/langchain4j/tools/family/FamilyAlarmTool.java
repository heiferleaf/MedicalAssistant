package com.whu.medicalbackend.agent.langchain4j.tools.family;

import com.whu.medicalbackend.dto.FamilyAlarmVO;
import com.whu.medicalbackend.dto.FamilyDetailVO;
import com.whu.medicalbackend.service.FamilyGroupService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FamilyAlarmTool {

    private static final Logger logger = LoggerFactory.getLogger(FamilyAlarmTool.class);

    private final FamilyGroupService familyGroupService;

    public FamilyAlarmTool(FamilyGroupService familyGroupService) {
        this.familyGroupService = familyGroupService;
    }

    @Tool("查询当天家庭组的健康异常告警")
    public String getFamilyAlarms(@ToolMemoryId String memoryId) {
        try {
            Long userId = parseUserId(memoryId);
            logger.info("[getFamilyAlarms] userId={}", userId);

            FamilyDetailVO myGroup = familyGroupService.getMyGroupDetails(userId);
            if (myGroup == null || myGroup.getGroup() == null) {
                return "您还没有加入任何家庭组。";
            }

            Long groupId = myGroup.getGroup().getGroupId();
            List<FamilyAlarmVO> alarms = familyGroupService.getTodayAlarms(groupId);

            if (alarms == null || alarms.isEmpty()) {
                return "今天没有家庭健康告警。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("今日家庭告警（共 ").append(alarms.size()).append(" 条）:\n\n");

            for (int i = 0; i < alarms.size(); i++) {
                FamilyAlarmVO alarm = alarms.get(i);
                sb.append((i + 1)).append(". [").append(alarm.getAlarmTime()).append("] ");
                sb.append(alarm.getMemberName()).append(" - ");
                sb.append(alarm.getMedicineName()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("[getFamilyAlarms] 执行失败", e);
            return "查询家庭告警失败：" + e.getMessage();
        }
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
