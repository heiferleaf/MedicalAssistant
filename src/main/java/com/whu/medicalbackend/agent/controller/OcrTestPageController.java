package com.whu.medicalbackend.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * OCR 测试页面控制器
 */
@Controller
@RequestMapping("/test")
public class OcrTestPageController {

    /**
     * 显示 OCR 测试页面
     */
    @GetMapping("/ocr-page")
    public String showOcrTestPage() {
        return "ocr-test";
    }
}
