package com.whu.medicalbackend.agent.langchain4j.tools.medicine;

import com.whu.medicalbackend.medical.entity.Medicine;
import com.whu.medicalbackend.medical.service.MedicineService;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionBroadcaster;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionWrapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MedicineQueryTool {

    private static final Logger logger = LoggerFactory.getLogger(MedicineQueryTool.class);

    private final MedicineService medicineService;
    private final ToolExecutionWrapper wrapper;

    public MedicineQueryTool(MedicineService medicineService, ToolExecutionBroadcaster broadcaster) {
        this.medicineService = medicineService;
        this.wrapper = new ToolExecutionWrapper(broadcaster);
    }

    @Tool("查询用户保存的所有药品信息")
    public String queryMyMedicines(@ToolMemoryId String memoryId) {
        Long userId = parseUserId(memoryId);
        logger.info("[queryMyMedicines] userId={}", userId);

        return wrapper.execute("queryMyMedicines", 
            Map.of("userId", userId, "memoryId", memoryId),
            "查询用户药品库",
            () -> {
                try {
                    List<Medicine> medicines = medicineService.getMedicinesByUserId(userId);

                    if (medicines == null || medicines.isEmpty()) {
                        return "您还没有保存任何药品。";
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("您的药品库（共 " + medicines.size() + " 种）:\n\n");

                    for (int i = 0; i < medicines.size(); i++) {
                        Medicine medicine = medicines.get(i);
                        sb.append((i + 1) + ". " + medicine.getName());
                        if (medicine.getDefaultDosage() != null && !medicine.getDefaultDosage().isBlank()) {
                            sb.append(" (" + medicine.getDefaultDosage() + ")");
                        }
                        if (medicine.getRemark() != null && !medicine.getRemark().isBlank()) {
                            sb.append(" - " + medicine.getRemark());
                        }
                        sb.append("\n");
                    }

                    return sb.toString();
                } catch (Exception e) {
                    logger.error("[queryMyMedicines] 执行失败", e);
                    return "查询药品库失败：" + e.getMessage();
                }
            }
        );
    }

    private Long parseUserId(String memoryId) {
        String[] parts = memoryId.split("_");
        return Long.parseLong(parts[0]);
    }
}
