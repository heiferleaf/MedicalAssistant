package com.whu.medicalbackend.agent.predict;

import com.whu.medicalbackend.common.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predict")
public class PredictController {

    @Autowired
    private PredictService predictService;

    @PostMapping("/analyze")
    public Result<PredictResponse> analyze(@RequestBody PredictRequest request) {
        try {
            PredictResponse response = predictService.analyzeText(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(500, "调用预测模型失败：" + e.getMessage());
        }
    }
}