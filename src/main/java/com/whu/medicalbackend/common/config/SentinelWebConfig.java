package com.whu.medicalbackend.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

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
}
