package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.whu.medicalbackend.common.response.Result;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

@Configuration
public class SentinelWebConfig {

    @Bean
    public FilterRegistrationBean<SentinelFilter> sentinelFilterRegistration() {
        FilterRegistrationBean<SentinelFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SentinelFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    public static class SentinelFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String resource = httpRequest.getRequestURI();

            Entry entry = null;
            try {
                entry = SphU.entry(resource);
                chain.doFilter(request, response);
            } catch (BlockException e) {
                handleBlockException(httpResponse, e);
            } finally {
                if (entry != null) {
                    entry.exit();
                }
            }
        }

        private void handleBlockException(HttpServletResponse response, BlockException ex) throws IOException {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);

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

            Result<?> result = Result.error(429, message);
            response.getWriter().write(toJson(result));
        }

        private String toJson(Result<?> result) {
            return String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    result.getCode(), result.getMessage());
        }
    }
}
