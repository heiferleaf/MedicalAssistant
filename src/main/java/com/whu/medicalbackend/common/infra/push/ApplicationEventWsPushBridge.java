package com.whu.medicalbackend.common.infra.push;

import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.family.service.FamilyCacheService;
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
import java.util.Set;

@Slf4j
@Component
public class ApplicationEventWsPushBridge {

    private final WsPushPublisher wsPushPublisher;
    private final ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider;
    private final ObjectProvider<FamilyCacheService> familyCacheServiceProvider;
    private final List<FamilyPushEventHook> familyPushEventHooks;

    public ApplicationEventWsPushBridge(
            WsPushPublisher wsPushPublisher,
            ObjectProvider<FamilyMemberMapper> familyMemberMapperProvider,
            ObjectProvider<FamilyCacheService> familyCacheServiceProvider,
            List<FamilyPushEventHook> familyPushEventHooks
    ) {
        this.wsPushPublisher = wsPushPublisher;
        this.familyMemberMapperProvider = familyMemberMapperProvider;
        this.familyCacheServiceProvider = familyCacheServiceProvider;
        this.familyPushEventHooks = familyPushEventHooks;
    }

    @Async("wsPushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleFamilyPushEvent(FamilyPushEvent event) {
        Long groupId = event.getGroupId();
        if (groupId == null) {
            log.warn("跳过家庭组 WebSocket 推送: groupId 为空, eventType={}", event.getEventType());
            return;
        }

        runFamilyEventHooks(event);

        List<Long> userIds = resolveTargetUserIds(groupId, event);
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

    private List<Long> resolveTargetUserIds(Long groupId, FamilyPushEvent event) {
        FamilyCacheService familyCacheService = familyCacheServiceProvider.getIfAvailable();
        if (familyCacheService != null) {
            // 家庭成员列表优先走 Redis，避免每次推送都重新查库；缓存缺失时再回退到 DB。
            Set<Object> cachedMemberIds = familyCacheService.getGroupMemberIds(groupId);
            if (cachedMemberIds != null && !cachedMemberIds.isEmpty()) {
                return cachedMemberIds.stream()
                        .map(String::valueOf)
                        .map(Long::valueOf)
                        .toList();
            }
            log.debug("家庭组成员缓存未命中，回退查询 DB: groupId={}, eventType={}", groupId, event.getEventType());
        }

        FamilyMemberMapper memberMapper = familyMemberMapperProvider.getIfAvailable();
        if (memberMapper == null) {
            log.warn("跳过家庭组 WebSocket 推送: 当前服务未加载 FamilyMemberMapper, groupId={}, eventType={}",
                    groupId, event.getEventType());
            return List.of();
        }
        return memberMapper.findActiveUserIdsByGroupId(groupId);
    }

    private void runFamilyEventHooks(FamilyPushEvent event) {
        for (FamilyPushEventHook hook : familyPushEventHooks) {
            try {
                // Hook 用来承载 WebSocket 之外的副作用，例如短信通知，不影响主推送链路。
                hook.handle(event);
            } catch (Exception ex) {
                log.error("家庭推送事件 hook 执行失败: hook={}, groupId={}, eventType={}",
                        hook.getClass().getSimpleName(), event.getGroupId(), event.getEventType(), ex);
            }
        }
    }

    private Map<String, Object> copyPayload(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }
}
