package com.whu.medicalbackend.agent.langchain4j.tools.predict;

import com.whu.medicalbackend.predict.PredictService;
import com.whu.medicalbackend.predict.PredictRequest;
import com.whu.medicalbackend.predict.PredictResponse;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PredictTool {

    @Autowired
    private PredictService predictService;

    @Tool("根据患者临床信息预测可能的药物不良反应")
    public String predictAdverseReactions(String clinicalInfo, String patientAge, String weight, String medicalHistory) {
        try {
            System.out.println("PredictTool.predictAdverseReactions 被调用了！");
            
            // 构建完整的临床描述
            StringBuilder clinicalText = new StringBuilder();
            clinicalText.append("Clinical Summary: ");
            
            if (patientAge != null && !patientAge.isEmpty()) {
                clinicalText.append(patientAge).append("-year-old ");
            }
            
            if (weight != null && !weight.isEmpty()) {
                clinicalText.append("patient (").append(weight).append(") ");
            }
            
            if (medicalHistory != null && !medicalHistory.isEmpty()) {
                clinicalText.append("with ").append(medicalHistory).append(". ");
            }
            
            if (clinicalInfo != null && !clinicalInfo.isEmpty()) {
                clinicalText.append(clinicalInfo);
            }
            
            PredictRequest request = new PredictRequest();
            request.setText(clinicalText.toString());
            
            // 修改：使用正确的方法名 analyzeText
            PredictResponse response = predictService.analyzeText(request);
            
            return formatPredictResult(response);
        } catch (Exception e) {
            System.err.println("药物不良反应预测失败: " + e.getMessage());
            e.printStackTrace();
            return "药物不良反应预测服务暂时不可用，请稍后重试。错误信息：" + e.getMessage();
        }
    }

    @Tool("基于症状和病史预测药物不良反应风险")
    public String analyzeAdverseReactionRisk(String symptoms, String currentMedications, String patientProfile) {
        try {
            // 构建临床文本
            String clinicalText = String.format("Patient symptoms: %s. Current medications: %s. Patient profile: %s", 
                symptoms != null ? symptoms : "未提供", 
                currentMedications != null ? currentMedications : "未提供",
                patientProfile != null ? patientProfile : "未提供");
            
            PredictRequest request = new PredictRequest();
            request.setText(clinicalText);
            
            // 修改：使用正确的方法名 analyzeText
            PredictResponse response = predictService.analyzeText(request);
            
            return formatRiskAssessment(response);
        } catch (Exception e) {
            return "药物不良反应风险分析服务暂时不可用，请稍后重试。错误信息：" + e.getMessage();
        }
    }

    private String formatPredictResult(PredictResponse response) {
        StringBuilder result = new StringBuilder();
        result.append("💊 药物不良反应预测结果\n");
        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        if (response == null || !"success".equals(response.getStatus())) {
            result.append("❌ 预测服务异常，请稍后重试\n");
            return result.toString();
        }
        
        if (response.getPredictions() == null || response.getPredictions().isEmpty()) {
            result.append("ℹ️ 暂未检测到明显的药物不良反应风险\n");
            return result.toString();
        }
        
        result.append("⚠️ 检测到以下潜在不良反应风险：\n\n");
        
        // 按概率降序显示前5个最可能的不良反应
        response.getPredictions().stream()
                .limit(5)
                .forEach(prediction -> {
                    double percentage = prediction.getProbability() * 100;
                    String riskLevel = getRiskLevel(prediction.getProbability());
                    
                    result.append(String.format("🔸 %s\n", prediction.getReaction()));
                    result.append(String.format("   概率: %.1f%% (%s)\n", percentage, riskLevel));
                    result.append("\n");
                });
        
        result.append("💡 建议：\n");
        result.append("• 请咨询医生或药师关于用药安全\n");
        result.append("• 密切观察是否出现上述症状\n");
        result.append("• 如出现异常反应请立即就医\n");
        
        return result.toString();
    }

    private String formatRiskAssessment(PredictResponse response) {
        StringBuilder result = new StringBuilder();
        result.append("📋 药物不良反应风险评估\n");
        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        if (response == null || !"success".equals(response.getStatus())) {
            result.append("❌ 风险评估异常，请稍后重试\n");
            return result.toString();
        }
        
        if (response.getPredictions() == null || response.getPredictions().isEmpty()) {
            result.append("✅ 当前用药方案风险较低\n");
            return result.toString();
        }
        
        // 计算整体风险等级
        double maxProbability = response.getPredictions().stream()
                .mapToDouble(p -> p.getProbability())
                .max()
                .orElse(0.0);
        
        String overallRisk = getRiskLevel(maxProbability);
        result.append(String.format("📊 整体风险等级: %s\n", overallRisk));
        result.append(String.format("📈 最高风险概率: %.1f%%\n\n", maxProbability * 100));
        
        result.append("🎯 重点关注项：\n");
        response.getPredictions().stream()
                .filter(p -> p.getProbability() > 0.6) // 只显示高风险项
                .limit(3)
                .forEach(prediction -> {
                    result.append(String.format("• %s (%.1f%%)\n", 
                        prediction.getReaction(), 
                        prediction.getProbability() * 100));
                });
        
        return result.toString();
    }

    private String getRiskLevel(double probability) {
        if (probability >= 0.7) return "高风险";
        if (probability >= 0.6) return "中高风险";
        if (probability >= 0.5) return "中等风险";
        return "低风险";
    }
}