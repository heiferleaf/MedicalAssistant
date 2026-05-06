package com.whu.medicalbackend.common.infra.push;

import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.ws.event.FamilyPushEvent;
import com.whu.medicalbackend.ws.event.UserTaskMedicineRemindEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ApplicationEventWsPushBridge {

    private final WsPushPublisher wsPushPublisher;
    private final ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider;

    public ApplicationEventWsPushBridge(
            WsPushPublisher wsPushPublisher,
            ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider
    ) {
        this.wsPushPublisher = wsPushPublisher;
        this.familyMemberMapperProvider = familyMemberMapperProvider;
    }

    @Async("wsPushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleFamilyPushEvent(FamilyPushEvent event) {
        Long groupId = event.getGroupId();
        if (groupId == null) {
            log.warn("跳过家庭组 WebSocket 推送: groupId 为空, eventType={}", event.getEventType());
            return;
        }

        FamilyMemberMapper memberMapper = familyMemberMapperProvider.getIfAvailable();
        if (memberMapper == null) {
            log.warn("跳过家庭组 WebSocket 推送: 当前服务未加载 FamilyMemberMapper, groupId={}, eventType={}",
                    groupId, event.getEventType());
            return;
        }

        List<Long> userIds = memberMapper.findActiveUserIdsByGroupId(groupId);
        if (userIds == null || userIds.isEmpty()) {
            log.warn("跳过家庭组 WebSocket 推送: 家庭组无活跃成员, groupId={}, eventType={}",
                    groupId, event.getEventType());
            return;
        }

        Map<String, Object> payload = copyPayload(event.getData());
        if (event.getEventType() != null) {
            payload.putIfAbsent("eventType", event.getEventType().name());
        }

        for (Long userId : userIds) {
            wsPushPublisher.pushToUser(WsPushCommand.user(userId, groupId, payload));
        }
    }

    @Async("wsPushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserTaskMedicineRemindEvent(UserTaskMedicineRemindEvent event) {
        if (event.getUserId() == null) {
            log.warn("跳过用户 WebSocket 推送: userId 为空");
            return;
        }

        wsPushPublisher.pushToUser(WsPushCommand.user(event.getUserId(), 0L, copyPayload(event.getData())));
    }

    private Map<String, Object> copyPayload(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }
}
