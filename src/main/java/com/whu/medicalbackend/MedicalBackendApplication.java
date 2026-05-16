package com.whu.medicalbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan({
        "com.whu.medicalbackend.medical.mapper",
        "com.whu.medicalbackend.family.mapper",
        "com.whu.medicalbackend.user.mapper",
        "com.whu.medicalbackend.health.mapper",
        "com.whu.medicalbackend.common.infra.event.audit"
})
public class MedicalBackendApplication{

    public static void main(String[] args) {
        SpringApplication.run(MedicalBackendApplication.class, args);
    }

}
