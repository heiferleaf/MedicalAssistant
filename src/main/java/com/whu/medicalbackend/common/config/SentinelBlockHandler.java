package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SentinelBlockHandler {
    
    public static void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":429,\"message\":\"Too Many Requests\"}");
    }
}
