package com.whu.medicalbackend.ws.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.entity.FamilyGroup;
import com.whu.medicalbackend.entity.User;
import com.whu.medicalbackend.mapper.FamilyGroupMapper;
import com.whu.medicalbackend.mapper.UserMapper;
import com.whu.medicalbackend.service.serviceImpl.FamilyCacheService;
import com.whu.medicalbackend.service.serviceImpl.RedisService;
import com.whu.medicalbackend.ws.WebSocketSessionManager;
import com.whu.medicalbackend.ws.event.FamilyPushEvent;
import com.whu.medicalbackend.ws.event.UserTaskMedicineRemindEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class WsAlarmBroadcastListener{

    private static final Logger logger = LoggerFactory.getLogger(WsAlarmBroadcastListener.class);

    @Autowired
    private RedisService redisService;
    @Autowired
    private WebSocketSessionManager sessionManager;
    @Autowired
    private FamilyCacheService familyCacheService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FamilyGroupMapper groupMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 使用 @Async 确保广播不阻塞主业务流程（如审批事务）
     */
    @Async
    @EventListener
    public void handleFamilyPushEvent(FamilyPushEvent event) throws JsonProcessingException {
        Long groupId = event.getGroupId();

/*        if(event instanceof  FamilyMedicineAlarmEvent) {
            // 处理发送短信的服务
            SmsHandle(groupId);
        }*/

        Set<Object>memberIds = familyCacheService.getGroupMemberIds(groupId);

        if(memberIds == null || memberIds.isEmpty()) {
            logger.warn("【WS广播】Redis中未找到小组 {} 的成员缓存，请检查缓存同步逻辑", groupId);
            return;
        }

        String jsonPayload = objectMapper.writeValueAsString(event.getData());

        memberIds.forEach(memberId -> {
            logger.info("向家庭组{ } 的用户{ } 广播消息，{}", memberId, groupId, jsonPayload);
            String userIdStr = memberId.toString();
            Long userId = Long.valueOf(userIdStr);
            // 发送给在线用户
            Boolean isOnline = redisService.hasMember(userIdStr);
            if(Boolean.FALSE.equals(isOnline)) return;

            WebSocketSession session = sessionManager.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonPayload));
                    logger.debug("【WS广播】成功推送到在线用户: {}", userId);
                } catch (IOException e) {
                    // 忽略发送异常
                    logger.error("【WS广播】物理推送失败: {}", userId);
                }
            }
        });
    }

    @Async
    @EventListener
    public void handleUserTaskMedicineRemindEvent(UserTaskMedicineRemindEvent event) throws JsonProcessingException {
        Long userId = event.getUserId();
        Boolean isOnline = (redisService.hasMember(userId.toString()));

        if(Boolean.FALSE.equals(isOnline)) return;

        // 在线的用户广播消息提醒
        WebSocketSession session = sessionManager.get(userId);
        if (session != null && session.isOpen()) {
            String jsonPayload = objectMapper.writeValueAsString(event.getData());
            try {
                session.sendMessage(new TextMessage(jsonPayload));
                logger.debug("【WS广播】成功推送到在线用户: {}", userId);
            } catch (IOException e) {
                // 忽略发送异常
                logger.error("【WS广播】物理推送失败: {}", userId);
            }
        }
    }

    private void SmsHandle(Long groupId) {
        Optional<String> phone =  Optional.ofNullable(groupMapper.selectById(groupId))
                .map(FamilyGroup::getOwnerUserId)
                .map(userId -> userMapper.findByUserId(userId))
                .map(User::getPhoneNumber);

        if(phone.isPresent()) {
            logger.info("family group leader phone {}", phone.get());
        }
    }
}