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
        "com.whu.medicalbackend.common",
        "com.whu.medicalbackend.family"
})
@MapperScan({
        "com.whu.medicalbackend.family.mapper",
        "com.whu.medicalbackend.user.mapper",
        "com.whu.medicalbackend.medical.mapper",
        "com.whu.medicalbackend.health.mapper"
})
@Import(RedisService.class)
public class FamilyServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FamilyServiceApplication.class);
        app.setAdditionalProfiles("family");
        app.run(args);
    }
}
