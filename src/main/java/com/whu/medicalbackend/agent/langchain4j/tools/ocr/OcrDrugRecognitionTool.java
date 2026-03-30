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
            @P(value = "The image of the drug package, can be a file path (e.g., /images/drug_xxx.jpg) or base64 format") String imageBase64) {
        
        logger.info("OcrDrugRecognitionTool 被调用，开始识别药物图片");
        logger.info("输入参数：{}", imageBase64 != null ? imageBase64.substring(0, Math.min(100, imageBase64.length())) : "null");

        try {
            byte[] imageBytes = null;
            
            // 检查是否是文件路径
            if (imageBase64 != null && imageBase64.startsWith("/images/")) {
                logger.info("检测到文件路径，尝试从文件系统读取：{}", imageBase64);
                
                // 尝试从多个可能的位置读取文件
                java.io.File imageFile = null;
                String[] possiblePaths = {
                    imageBase64,
                    "./nginx/static/images" + imageBase64,
                    "nginx/static/images" + imageBase64,
                    "/usr/share/nginx/static/images" + imageBase64.substring(imageBase64.lastIndexOf("/"))
                };
                
                for (String path : possiblePaths) {
                    if (path != null && !path.isEmpty()) {
                        java.io.File testFile = new java.io.File(path);
                        logger.info("测试文件路径：{}，存在：{}", path, testFile.exists());
                        if (testFile.exists()) {
                            imageFile = testFile;
                            logger.info("找到文件：{}", path);
                            break;
                        }
                    }
                }
                
                if (imageFile != null && imageFile.exists()) {
                    imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
                    logger.info("从文件读取图片成功，大小：{} bytes", imageBytes.length);
                } else {
                    logger.error("图片文件不存在，尝试的路径：{}", String.join(", ", possiblePaths));
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "图片文件不存在");
                    return result;
                }
            } else if (imageBase64 != null && (imageBase64.startsWith("/9j/") || imageBase64.startsWith("data:image"))) {
                // 处理 Base64 数据（可能包含 data:image/jpeg;base64, 前缀）
                logger.info("检测到 Base64 数据");
                String base64Data = imageBase64;
                
                // 移除 data URL 前缀
                if (base64Data.startsWith("data:image")) {
                    int commaIndex = base64Data.indexOf(',');
                    if (commaIndex != -1) {
                        base64Data = base64Data.substring(commaIndex + 1);
                        logger.info("已移除 data URL 前缀");
                    }
                }
                
                // 解码 Base64
                try {
                    imageBytes = Base64.getDecoder().decode(base64Data);
                    logger.info("Base64 解码成功，大小：{} bytes", imageBytes.length);
                } catch (IllegalArgumentException e) {
                    logger.error("Base64 解码失败：{}", e.getMessage());
                    logger.error("Base64 数据前 100 字符：{}", base64Data.substring(0, Math.min(100, base64Data.length())));
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "Base64 解码失败：" + e.getMessage());
                    return result;
                }
            } else {
                logger.error("未知的图片格式：{}", imageBase64 != null ? imageBase64.substring(0, Math.min(50, imageBase64.length())) : "null");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "未知的图片格式");
                return result;
            }

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
