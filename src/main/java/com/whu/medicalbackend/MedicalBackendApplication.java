package com.whu.medicalbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedicalBackendApplication{

    public static void main(String[] args) {
        SpringApplication.run(MedicalBackendApplication.class, args);
    }

}
