package com.whu.medicalbackend.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.whu.medicalbackend.common",
        "com.whu.medicalbackend.agent",
        "com.whu.medicalbackend.family",
        "com.whu.medicalbackend.medical",
        "com.whu.medicalbackend.user"
})
@MapperScan({
        "com.whu.medicalbackend.agent.mapper",
        "com.whu.medicalbackend.family.mapper",
        "com.whu.medicalbackend.medical.mapper",
        "com.whu.medicalbackend.user.mapper"
})
public class AgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AgentServiceApplication.class);
        app.setAdditionalProfiles("agent");
        app.run(args);
    }
}
