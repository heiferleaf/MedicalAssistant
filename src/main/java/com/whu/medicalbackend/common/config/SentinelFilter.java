package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Sentinel 全局 Web Filter，基于 Jakarta Servlet API + Sentinel Core API 实现。
 */
public class SentinelFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String resourceName = httpRequest.getRequestURI();
        Entry entry = null;
        try {
            entry = SphU.entry(resourceName);
            chain.doFilter(request, response);
        } catch (BlockException e) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too Many Requests\"}");
        } catch (IOException | ServletException | RuntimeException e) {
            Tracer.traceEntry(e, entry);
            throw e;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
