package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.common.ResultCode;
import com.whu.medicalbackend.service.serviceImpl.FlaskAgentProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentProxyController {

    @Autowired
    private FlaskAgentProxyService flaskAgentProxyService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> resp = flaskAgentProxyService.chat(payload);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "Flask /agent/chat 调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> resp = flaskAgentProxyService.confirm(payload);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "Flask /agent/confirm 调用失败: " + e.getMessage());
        }
    }

    @GetMapping({"/health", "/health/"})
    public Result<Map<String, Object>> health() {
        try {
            Map<String, Object> resp = flaskAgentProxyService.health();
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error(ResultCode.SYSTEM_ERROR, "Flask /agent/health 调用失败: " + e.getMessage());
        }
    }
}
