package com.whu.medicalbackend.bootstrap;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.family.service.FamilyCacheService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.whu.medicalbackend.common",
        "com.whu.medicalbackend.ws"
})
@MapperScan({})
@Import({RedisService.class, FamilyCacheService.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NotificationServiceApplication.class);
        app.setAdditionalProfiles("notification");
        app.run(args);
    }
}
