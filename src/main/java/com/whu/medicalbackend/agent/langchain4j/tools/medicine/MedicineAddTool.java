package com.whu.medicalbackend.agent.langchain4j.tools.medicine;

import com.whu.medicalbackend.entity.Medicine;
import com.whu.medicalbackend.service.MedicineService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MedicineAddTool {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAddTool.class);

    private final MedicineService medicineService;

    public MedicineAddTool(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @Tool("向用户的药品库中添加新药品，需要提供药品名称，可选参数：药品名称、默认剂量、备注")
    public String addMedicine(@ToolMemoryId String memoryId, String medicineName, String defaultDosage, String remark) {
        try {
            Long userId = parseUserId(memoryId);
            logger.info("[addMedicine] userId={}, medicineName={}, defaultDosage={}", userId, medicineName,
                    defaultDosage);

            if (medicineName == null || medicineName.isBlank()) {
                return "药品名称不能为空！";
            }

            Medicine medicine = medicineService.findOrCreate(userId, medicineName, defaultDosage);

            if (remark != null && !remark.isBlank()
                    && (medicine.getRemark() == null || medicine.getRemark().isBlank())) {
                medicine.setRemark(remark);
            }

            return "已成功添加药品：" + medicineName + "！";
        } catch (Exception e) {
            logger.error("[addMedicine] 执行失败", e);
            return "添加药品失败：" + e.getMessage();
        }
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
