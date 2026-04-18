package com.whu.medicalbackend.ws;

import com.whu.medicalbackend.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        String token = servletRequest.getServletRequest().getParameter("token");

        Optional<Claims> claims;
        if(token != null && (claims = JwtUtil.parseToken(token)).isPresent()) {
            Long userId = claims.map(Claims::getSubject).map(Long::valueOf).orElse(Long.valueOf(-1));
            if(userId == Long.valueOf(-1)) return false;
            attributes.put("userId", userId);
            return true;
        }
        response.getBody().write("{\"msg\":\"token invalid\"}".getBytes());
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
