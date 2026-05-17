package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.adapter.servlet.callback.UrlBlockHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.whu.medicalbackend.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SentinelBlockHandler implements UrlBlockHandler {

    @Override
    public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException ex) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429);

        Result<?> result;
        String message;

        if (ex instanceof FlowException) {
            message = "AI 服务繁忙，请稍后重试";
        } else if (ex instanceof ParamFlowException) {
            message = "您的请求过于频繁，请稍后再试";
        } else if (ex instanceof DegradeException) {
            message = "服务暂时不可用，请稍后重试";
        } else if (ex instanceof AuthorityException) {
            message = "无权访问该资源";
        } else if (ex instanceof SystemBlockException) {
            message = "系统负载过高，请稍后重试";
        } else {
            message = "请求被限流，请稍后重试";
        }

        result = Result.error(429, message);

        response.getWriter().write(toJson(result));
    }

    private String toJson(Result<?> result) {
        return String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                result.getCode(), result.getMessage());
    }
}
