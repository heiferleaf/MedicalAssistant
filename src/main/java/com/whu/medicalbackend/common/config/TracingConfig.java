package com.whu.medicalbackend.common.config;

import brave.propagation.CurrentTraceContext;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public CurrentTraceContext currentTraceContext() {
        return ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator((context, scope) -> {
                    String traceId = context.traceIdString();
                    String spanId = context.spanIdString();
                    String parentId = context.parentId() != null ? context.parentIdString() : null;
                    
                    String previousTraceId = MDC.get("traceId");
                    String previousSpanId = MDC.get("spanId");
                    String previousParentId = MDC.get("parentId");
                    
                    MDC.put("traceId", traceId);
                    MDC.put("spanId", spanId);
                    if (parentId != null) {
                        MDC.put("parentId", parentId);
                    } else {
                        MDC.remove("parentId");
                    }
                    
                    return () -> {
                        if (previousTraceId == null) {
                            MDC.remove("traceId");
                        } else {
                            MDC.put("traceId", previousTraceId);
                        }
                        if (previousSpanId == null) {
                            MDC.remove("spanId");
                        } else {
                            MDC.put("spanId", previousSpanId);
                        }
                        if (previousParentId == null) {
                            MDC.remove("parentId");
                        } else {
                            MDC.put("parentId", previousParentId);
                        }
                        scope.close();
                    };
                })
                .build();
    }
}
