package com.whu.medicalbackend.ws;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("wsSessionManager")
@Scope("singleton")
@Lazy(false)
public class WebSocketSessionManager {
    // userId ==> Session
    private final Map<Long, WebSocketSession> SESSION_POOL = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session) {
        SESSION_POOL.put(userId, session);
    }

    public void remove(Long userId) {
        SESSION_POOL.remove(userId);
    }

    public WebSocketSession get(Long userId) {
        return SESSION_POOL.get(userId);
    }
}
