package com.whu.medicalbackend.bootstrap;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.whu.medicalbackend.common"
})
@MapperScan({
        "com.whu.medicalbackend.medical.mapper",
        "com.whu.medicalbackend.family.mapper"
})
@Import(RedisService.class)
public class SchedulerServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SchedulerServiceApplication.class);
        app.setAdditionalProfiles("scheduler");
        app.run(args);
    }
}
