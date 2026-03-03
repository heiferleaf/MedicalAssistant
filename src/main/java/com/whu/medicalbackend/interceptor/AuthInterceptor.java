package com.whu.medicalbackend.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.common.ResultCode;
import com.whu.medicalbackend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if("OPTIONS".equals(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Optional<Claims> claimsOptional = JwtUtil.parseToken(token);
            if (claimsOptional.isPresent()) {
                request.setAttribute("userId", Long.valueOf(claimsOptional.get().getSubject()));
                return true;
            }

        }
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");

        Result<?> result = Result.error(ResultCode.NEED_REFRESH_ERROR);
        String resultJson = new ObjectMapper().writeValueAsString(result);

        response.getWriter().write(resultJson);
        return false;
    }
}
