package com.whu.medicalbackend.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TracingTestController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        log.info("Tracing test endpoint called");
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello from MedicalBackend with Tracing!");
        result.put("timestamp", System.currentTimeMillis());
        
        simulateBusinessLogic();
        
        return result;
    }

    @GetMapping("/chain")
    public Map<String, Object> chain() {
        log.info("Chain test started");
        
        Map<String, Object> result = new HashMap<>();
        result.put("step", "1");
        result.put("message", "Chain test endpoint");
        
        stepTwo(result);
        
        return result;
    }

    private void simulateBusinessLogic() {
        log.info("Simulating business logic");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Business logic completed");
    }

    private void stepTwo(Map<String, Object> result) {
        log.info("Executing step 2");
        result.put("step", "2");
        
        stepThree(result);
        
        log.info("Step 2 completed");
    }

    private void stepThree(Map<String, Object> result) {
        log.info("Executing step 3");
        result.put("step", "3");
        result.put("completed", true);
        log.info("Step 3 completed");
    }
}
