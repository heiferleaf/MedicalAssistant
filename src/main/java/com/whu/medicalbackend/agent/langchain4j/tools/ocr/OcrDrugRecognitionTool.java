package com.whu.medicalbackend.agent.langchain4j.tools.ocr;

import com.whu.medicalbackend.agent.service.OcrService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OcrDrugRecognitionTool {

    private static final Logger logger = LoggerFactory.getLogger(OcrDrugRecognitionTool.class);

    private final OcrService ocrService;

    public OcrDrugRecognitionTool(OcrService ocrService) {
        this.ocrService = ocrService;
        logger.info("OcrDrugRecognitionTool 初始化成功");
    }

    @Tool(value = "Recognize drug information from an image using OCR. Returns structured drug information including name, dosage, frequency, etc. IMPORTANT: This tool does NOT actually create any plan or add medicine to cabinet. It only returns the recognized information. The frontend will use this information to show a confirmation card to the user.")
    public Map<String, Object> recognizeDrugFromImage(
            @P(value = "The image of the drug package in base64 format") String imageBase64) {
        
        logger.info("OcrDrugRecognitionTool 被调用，开始识别药物图片");

        try {
            // 1. 解码 Base64 图片数据
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            logger.info("图片数据解码成功，大小：{} bytes", imageBytes.length);

            // 2. 调用 Flask OCR 接口
            Map<String, Object> ocrResult = ocrService.recognizeDrugImage(imageBytes);
            logger.info("OCR 识别成功：{}", ocrResult);

            // 3. 解析 OCR 结果
            String rawText = (String) ocrResult.get("ocr_result");
            String output = (String) ocrResult.get("output");

            // 4. 从输出中提取结构化信息
            Map<String, String> extractedInfo = extractDrugInfo(output);
            logger.info("提取的药物信息：{}", extractedInfo);

            // 5. 返回待确认标记（不真正创建计划或添加药品）
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("pending_confirmation", true);
            result.put("tool_name", "recognizeDrugFromImage");
            result.put("arguments", new LinkedHashMap<String, Object>() {{
                put("medicineName", extractedInfo.get("medicineName") != null ? extractedInfo.get("medicineName") : "未知药品");
                put("defaultDosage", extractedInfo.get("dosage") != null ? extractedInfo.get("dosage") : "按说明书");
                put("frequency", extractedInfo.get("frequency") != null ? extractedInfo.get("frequency") : "遵医嘱");
                put("rawText", rawText != null ? rawText : "");
                put("structuredOutput", output != null ? output : "");
            }});
            result.put("message", "请确认是否将识别到的药品添加到药箱或创建用药计划");

            return result;

        } catch (Exception e) {
            logger.error("OCR 识别失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "OCR 识别失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 从 OCR 输出中提取结构化信息
     */
    private Map<String, String> extractDrugInfo(String output) {
        Map<String, String> info = new LinkedHashMap<>();
        
        if (output == null || output.trim().isEmpty()) {
            return info;
        }

        // 解析输出文本，提取关键字段
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 提取药品名称
            if (line.startsWith("药品名称:") || line.startsWith("成份:") || line.startsWith("成分:")) {
                info.put("medicineName", extractValue(line));
            }
            // 提取规格/剂量
            else if (line.startsWith("规格:")) {
                info.put("dosage", extractValue(line));
            }
            // 提取用法用量
            else if (line.startsWith("用法用量:")) {
                String usage = extractValue(line);
                info.put("usage", usage);
                // 从用法用量中提取频次
                info.put("frequency", extractFrequency(usage));
            }
            // 提取适应症
            else if (line.startsWith("适应症:")) {
                info.put("indication", extractValue(line));
            }
            // 提取注意事项
            else if (line.startsWith("注意事项:")) {
                info.put("caution", extractValue(line));
            }
        }

        return info;
    }

    /**
     * 从行中提取值（去掉键名部分）
     */
    private String extractValue(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex >= 0 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return line;
    }

    /**
     * 从用法用量中提取频次
     */
    private String extractFrequency(String usage) {
        if (usage == null || usage.isEmpty()) {
            return "遵医嘱";
        }

        // 尝试提取频次信息
        if (usage.contains("一日") || usage.contains("每天")) {
            if (usage.contains("1 次") || usage.contains("一次")) {
                return "每日 1 次";
            } else if (usage.contains("2 次") || usage.contains("二次")) {
                return "每日 2 次";
            } else if (usage.contains("3 次") || usage.contains("三次")) {
                return "每日 3 次";
            }
        }

        return "遵医嘱";
    }
}
