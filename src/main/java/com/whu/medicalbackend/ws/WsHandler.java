package com.whu.medicalbackend.ws;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil; // 引入工具类
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WsHandler extends TextWebSocketHandler {

    @Autowired
    private RedisService redisService;

    @Resource(name = "wsSessionManager")
    private WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");

        if(userId != null) {
            sessionManager.add(userId, session);

            // 使用工具类构建在线状态的 Hash Key: family:online:members
            String onlineKey = RedisKeyBuilderUtil.getOnlineMemberKey();
            redisService.putWithHash(onlineKey, userId.toString(), "1");

            System.out.println("用户 " + userId + " 已登记，WebSocket 连接已建立");
        }
    }

    /**
     * 处理客户端发送过来的消息（处理心跳包）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 1. 判断是否是心跳包
        if ("ping".equalsIgnoreCase(payload)) {
            // 回复一个 pong 给前端，告知服务器在线
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        // 2. 如果未来有其他业务逻辑（比如客户端主动发消息），在这里处理
        // 目前你的业务主要是后端广播，所以这里通常只需要处理 ping 即可
        System.out.println("收到客户端其他消息: " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if(userId != null) {
            sessionManager.remove(userId);

            // 使用工具类构建在线状态的 Hash Key 并移除
            String onlineKey = RedisKeyBuilderUtil.getOnlineMemberKey();
            redisService.deleteWithHash(onlineKey, userId.toString());

            System.out.println("用户 " + userId + " 已下线，清理 Redis 在线状态");
        }
    }
}