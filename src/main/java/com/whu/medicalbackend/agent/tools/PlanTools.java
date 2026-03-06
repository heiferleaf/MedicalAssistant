package com.whu.medicalbackend.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whu.medicalbackend.agent.memory.AgentMemoryRepository;
import com.whu.medicalbackend.dto.PlanCreateDTO;
import com.whu.medicalbackend.dto.PlanVO;
import com.whu.medicalbackend.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Plan 相关工具的定义和处理
 */
@Component
public class PlanTools {

    private static final Logger logger = LoggerFactory.getLogger(PlanTools.class);

    private final PlanService planService;
    private final AgentMemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;

    public PlanTools(
            PlanService planService,
            AgentMemoryRepository memoryRepository,
            ObjectMapper objectMapper
    ) {
        this.planService = planService;
        this.memoryRepository = memoryRepository;
        this.objectMapper = objectMapper;
    }

    // ========== 工具定义 ==========

    /**
     * 创建查询用药计划工具定义
     */
    public ObjectNode createPlanQueryTool() {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "spring_plan_query");
        funcDef.put("description", "查询用户的用药计划列表，获取所有正在进行的和历史的用药计划");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", objectMapper.createObjectNode());
        parameters.set("required", objectMapper.createArrayNode());
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    /**
     * 创建更新用药计划工具定义
     */
    public ObjectNode createPlanUpdateTool() {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "spring_plan_update");
        funcDef.put("description", "修改已有的用药计划，可以更改药品名称、剂量、时间等");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode planId = objectMapper.createObjectNode();
        planId.put("type", "integer");
        planId.put("description", "要修改的计划 ID");
        properties.set("planId", planId);
        
        ObjectNode medicineName = objectMapper.createObjectNode();
        medicineName.put("type", "string");
        medicineName.put("description", "新的药品名称，如'阿司匹林肠溶片'");
        properties.set("medicineName", medicineName);
        
        ObjectNode dosage = objectMapper.createObjectNode();
        dosage.put("type", "string");
        dosage.put("description", "新的用药剂量，如'100mg'或'每次 2 片'");
        properties.set("dosage", dosage);
        
        ObjectNode timePoints = objectMapper.createObjectNode();
        timePoints.put("type", "array");
        timePoints.put("description", "新的每日用药时间，如['08:00', '20:00']");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "string");
        timePoints.set("items", items);
        properties.set("timePoints", timePoints);
        
        ObjectNode startDate = objectMapper.createObjectNode();
        startDate.put("type", "string");
        startDate.put("description", "开始日期，格式：YYYY-MM-DD");
        properties.set("startDate", startDate);
        
        ObjectNode endDate = objectMapper.createObjectNode();
        endDate.put("type", "string");
        endDate.put("description", "结束日期（可选），格式：YYYY-MM-DD");
        properties.set("endDate", endDate);
        
        ObjectNode remark = objectMapper.createObjectNode();
        remark.put("type", "string");
        remark.put("description", "备注信息（可选）");
        properties.set("remark", remark);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("planId");
        required.add("medicineName");
        required.add("dosage");
        required.add("timePoints");
        required.add("startDate");
        
        parameters.set("properties", properties);
        parameters.set("required", required);
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    /**
     * 创建删除用药计划工具定义
     */
    public ObjectNode createPlanDeleteTool() {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "spring_plan_delete");
        funcDef.put("description", "删除/停用用药计划。" +
            "如果要删除单个计划，请提供 planId 或 medicineName；" +
            "如果要删除多个计划，请多次调用此工具，每次删除一个计划；" +
            "不要使用 deleteAll 参数，因为这会一次性删除所有计划，无法体现多轮执行过程");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode planId = objectMapper.createObjectNode();
        planId.put("type", "integer");
        planId.put("description", "要删除的计划 ID（与 medicineName 互斥）");
        properties.set("planId", planId);
        
        ObjectNode medicineName = objectMapper.createObjectNode();
        medicineName.put("type", "string");
        medicineName.put("description", "要删除的药品名称（与 planId 互斥），如'阿司匹林'");
        properties.set("medicineName", medicineName);
        
        ObjectNode deleteAll = objectMapper.createObjectNode();
        deleteAll.put("type", "boolean");
        deleteAll.put("description", "是否删除所有用药计划（true/false，不推荐使用）");
        properties.set("deleteAll", deleteAll);
        
        ArrayNode required = objectMapper.createArrayNode();
        // 至少需要一个参数
        required.add("planId");  // 实际上应该用 oneOf，但简化处理
        
        parameters.set("properties", properties);
        parameters.set("required", required);
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    /**
     * 获取所有 Plan 工具定义
     */
    public ArrayNode getAllPlanTools() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(createPlanQueryTool());
        tools.add(createPlanUpdateTool());
        tools.add(createPlanDeleteTool());
        tools.add(createPlanCreateTool());
        return tools;
    }

    /**
     * 创建用药计划工具定义（无需确认版本）
     */
    public ObjectNode createPlanCreateTool() {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "spring_plan_create");
        funcDef.put("description", "直接创建用药计划，无需用户确认。适用于用户已提供完整信息的情况");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode medicineName = objectMapper.createObjectNode();
        medicineName.put("type", "string");
        medicineName.put("description", "药品名称，如'阿司匹林肠溶片'");
        properties.set("medicineName", medicineName);
        
        ObjectNode dosage = objectMapper.createObjectNode();
        dosage.put("type", "string");
        dosage.put("description", "用药剂量，如'100mg'或'每次 2 片'");
        properties.set("dosage", dosage);
        
        ObjectNode timePoints = objectMapper.createObjectNode();
        timePoints.put("type", "array");
        timePoints.put("description", "每日用药时间，如['08:00', '20:00']");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "string");
        timePoints.set("items", items);
        properties.set("timePoints", timePoints);
        
        ObjectNode startDate = objectMapper.createObjectNode();
        startDate.put("type", "string");
        startDate.put("description", "开始日期，格式：YYYY-MM-DD");
        properties.set("startDate", startDate);
        
        ObjectNode endDate = objectMapper.createObjectNode();
        endDate.put("type", "string");
        endDate.put("description", "结束日期（可选），格式：YYYY-MM-DD");
        properties.set("endDate", endDate);
        
        ObjectNode remark = objectMapper.createObjectNode();
        remark.put("type", "string");
        remark.put("description", "备注信息（可选）");
        properties.set("remark", remark);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("medicineName");
        required.add("dosage");
        required.add("timePoints");
        required.add("startDate");
        
        parameters.set("properties", properties);
        parameters.set("required", required);
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    // ========== 工具执行 ==========

    /**
     * 执行 Plan 相关工具
     */
    public Map<String, Object> executeTool(
            String toolName,
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        return switch (toolName) {
            case "spring_plan_query" -> handlePlanQuery(userId, sessionId, withTrace, withTiming, trace);
            case "spring_plan_update" -> handlePlanUpdate(toolArgs, userId, sessionId, withTrace, withTiming, trace);
            case "spring_plan_delete" -> handlePlanDelete(toolArgs, userId, sessionId, withTrace, withTiming, trace);
            case "spring_plan_create" -> handlePlanCreate(toolArgs, userId, sessionId, withTrace, withTiming, trace);
            default -> null;
        };
    }

    /**
     * 查询用药计划
     */
    private Map<String, Object> handlePlanQuery(
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        logger.info("查询用户用药计划，userId: {}", userId);
        
        try {
            Long uid = Long.valueOf(userId);
            List<PlanVO> plans = planService.getPlanList(uid);
            logger.info("查询到 {} 个用药计划", plans.size());
            
            String assistantMessage;
            if (plans.isEmpty()) {
                assistantMessage = "你目前还没有创建任何用药计划。如果需要，我可以帮你创建一个用药提醒计划。";
            } else {
                StringBuilder sb = new StringBuilder("你当前共有 ");
                sb.append(plans.size()).append(" 个用药计划：\n\n");
                
                for (int i = 0; i < plans.size(); i++) {
                    PlanVO plan = plans.get(i);
                    sb.append(i + 1).append(". ").append(plan.getMedicineName())
                      .append(" (").append(plan.getDosage()).append(")\n");
                    
                    // 将 LocalTime 列表转换为字符串列表
                    List<String> timeStrs = plan.getTimePoints().stream()
                            .map(LocalTime::toString)
                            .toList();
                    sb.append("   时间：").append(String.join(", ", timeStrs)).append("\n");
                    
                    sb.append("   期限：").append(plan.getStartDate())
                      .append(" 至 ").append(plan.getEndDate() != null ? plan.getEndDate() : "长期")
                      .append("\n");
                    if (plan.getRemark() != null && !plan.getRemark().isBlank()) {
                        sb.append("   备注：").append(plan.getRemark()).append("\n");
                    }
                    if (i < plans.size() - 1) {
                        sb.append("\n");
                    }
                }
                
                assistantMessage = sb.toString();
            }
            
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("plans_count", plans.size());
            result.put("plans", plans.stream().map(p -> Map.of(
                "id", p.getId(),
                "medicineName", p.getMedicineName(),
                "dosage", p.getDosage(),
                "timePoints", p.getTimePoints().stream().map(LocalTime::toString).toList()
            )).toList());
            result.put("need_continue", true); // 查询后需要继续，让 LLM 决定下一步操作
            
            if (withTrace) {
                trace.put("tools_called", List.of("spring_plan_query"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
            
        } catch (Exception e) {
            logger.error("查询用药计划失败", e);
            return errorResult("查询用药计划失败：" + e.getMessage(), userId, sessionId, withTrace, withTiming, trace);
        }
    }

    /**
     * 更新用药计划
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePlanUpdate(
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        logger.info("更新用药计划：{}", toolArgs);
        
        try {
            // 提取参数
            Integer planIdObj = (Integer) toolArgs.get("planId");
            String medicineName = str(toolArgs.get("medicineName"));
            String dosage = str(toolArgs.get("dosage"));
            List<String> timePoints = extractStringList(toolArgs.get("timePoints"));
            String startDateStr = str(toolArgs.get("startDate"));
            String endDateStr = str(toolArgs.get("endDate"));
            String remark = str(toolArgs.get("remark"));
            
            if (planIdObj == null) {
                return errorResult("缺少必需参数：planId", userId, sessionId, withTrace, withTiming, trace);
            }
            
            Long planId = planIdObj.longValue();
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = endDateStr != null && !endDateStr.isBlank() ? LocalDate.parse(endDateStr) : null;
            
            // 创建 DTO
            PlanCreateDTO dto = new PlanCreateDTO();
            dto.setMedicineName(medicineName);
            dto.setDosage(dosage);
            dto.setTimePoints(timePoints.stream().map(LocalTime::parse).toList());
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setRemark(remark);
            
            // 执行更新
            Long uid = Long.valueOf(userId);
            planService.updatePlan(uid, planId, dto);
            
            String assistantMessage = "已成功更新用药计划！从明天开始，系统将按新的时间提醒你用药。";
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("updated_plan_id", planId);
            
            if (withTrace) {
                trace.put("tools_called", List.of("spring_plan_update"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
            
        } catch (DateTimeParseException e) {
            logger.warn("日期格式错误", e);
            return errorResult("日期格式错误，请使用 YYYY-MM-DD 格式，例如 2026-03-05", userId, sessionId, withTrace, withTiming, trace);
        } catch (Exception e) {
            logger.error("更新用药计划失败", e);
            return errorResult("更新用药计划失败：" + e.getMessage(), userId, sessionId, withTrace, withTiming, trace);
        }
    }

    /**
     * 删除用药计划
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePlanDelete(
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        logger.info("删除用药计划：{}", toolArgs);
        
        try {
            Integer planIdObj = (Integer) toolArgs.get("planId");
            String medicineName = str(toolArgs.get("medicineName"));
            Boolean deleteAll = toolArgs.get("deleteAll") != null && (Boolean) toolArgs.get("deleteAll");
            
            Long uid = Long.valueOf(userId);
            int deletedCount = 0;
            
            if (deleteAll) {
                // 删除所有计划
                List<PlanVO> plans = planService.getPlanList(uid);
                if (plans.isEmpty()) {
                    String msg = "你目前没有任何用药计划，无需删除。";
                    memoryRepository.appendMessage(sessionId, userId, "assistant", msg);
                    return createDeleteResult(0, msg, userId, sessionId, withTrace, withTiming, trace, false);
                }
                
                for (PlanVO plan : plans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                String assistantMessage = "已成功删除你所有的 " + deletedCount + " 个用药计划，未来的用药提醒已全部取消。";
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                return createDeleteResult(deletedCount, assistantMessage, userId, sessionId, withTrace, withTiming, trace, false);
                
            } else if (planIdObj != null) {
                // 用户提供了 planId
                Long planId = planIdObj.longValue();
                planService.deletePlan(uid, planId);
                String assistantMessage = "已成功删除该用药计划，未来的用药提醒已取消。";
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                return createDeleteResult(1, assistantMessage, userId, sessionId, withTrace, withTiming, trace, false);
                
            } else if (medicineName != null && !medicineName.isBlank()) {
                // 用户提供了药品名，需要查找匹配的计划
                List<PlanVO> plans = planService.getPlanList(uid);
                
                // 查找匹配的药品
                List<PlanVO> matchedPlans = plans.stream()
                    .filter(p -> p.getMedicineName().contains(medicineName) || 
                                 medicineName.contains(p.getMedicineName()))
                    .toList();
                
                if (matchedPlans.isEmpty()) {
                    return errorResult("未找到名为\"" + medicineName + "\"的用药计划", userId, sessionId, withTrace, withTiming, trace);
                }
                
                // 删除所有匹配的计划
                for (PlanVO plan : matchedPlans) {
                    try {
                        planService.deletePlan(uid, plan.getId());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.warn("删除计划 {} 失败：{}", plan.getId(), e.getMessage());
                    }
                }
                
                logger.info("根据药品名\"{}\"匹配到 {} 个计划，已删除", medicineName, deletedCount);
                String assistantMessage = "已成功删除 " + deletedCount + " 个与\"" + medicineName + "\"相关的用药计划，未来的用药提醒已取消。";
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
                return createDeleteResult(deletedCount, assistantMessage, userId, sessionId, withTrace, withTiming, trace, false);
                
            } else {
                return errorResult("缺少必需参数：planId、medicineName 或 deleteAll", userId, sessionId, withTrace, withTiming, trace);
            }
            
        } catch (Exception e) {
            logger.error("删除用药计划失败", e);
            return errorResult("删除用药计划失败：" + e.getMessage(), userId, sessionId, withTrace, withTiming, trace);
        }
    }
    
    /**
     * 创建删除结果
     */
    private Map<String, Object> createDeleteResult(
            int deletedCount,
            String assistantMessage,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace,
            boolean needContinue
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("assistant_message", assistantMessage);
        result.put("need_confirm", false);
        result.put("actions", List.of());
        result.put("deleted_count", deletedCount);
        result.put("need_continue", needContinue);
        
        if (withTrace) {
            trace.put("tools_called", List.of("spring_plan_delete"));
            result.put("trace", trace);
        }
        if (withTiming) {
            result.put("timings", Map.of());
        }
        return result;
    }

    /**
     * 创建用药计划（无需确认版本）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePlanCreate(
            Map<String, Object> toolArgs,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        logger.info("创建用药计划：{}", toolArgs);
        
        try {
            // 提取参数
            String medicineName = str(toolArgs.get("medicineName"));
            String dosage = str(toolArgs.get("dosage"));
            List<String> timePoints = extractStringList(toolArgs.get("timePoints"));
            String startDateStr = str(toolArgs.get("startDate"));
            String endDateStr = str(toolArgs.get("endDate"));
            String remark = str(toolArgs.get("remark"));
            
            if (medicineName == null || medicineName.isBlank()) {
                return errorResult("缺少必需参数：medicineName", userId, sessionId, withTrace, withTiming, trace);
            }
            if (dosage == null || dosage.isBlank()) {
                dosage = "按医嘱";
            }
            if (timePoints.isEmpty()) {
                return errorResult("缺少必需参数：timePoints", userId, sessionId, withTrace, withTiming, trace);
            }
            if (startDateStr == null || startDateStr.isBlank()) {
                return errorResult("缺少必需参数：startDate", userId, sessionId, withTrace, withTiming, trace);
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = endDateStr != null && !endDateStr.isBlank() ? LocalDate.parse(endDateStr) : null;
            
            // 创建 DTO
            PlanCreateDTO dto = new PlanCreateDTO();
            dto.setMedicineName(medicineName);
            dto.setDosage(dosage);
            dto.setTimePoints(timePoints.stream().map(LocalTime::parse).toList());
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setRemark(remark);
            
            // 执行创建
            Long uid = Long.valueOf(userId);
            PlanVO planVO = planService.createPlan(uid, dto);
            
            String assistantMessage = "已成功为你创建用药计划！从明天开始，系统将按时提醒你用药。";
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("created_plan_id", planVO.getId());
            result.put("plan", Map.of(
                "id", planVO.getId(),
                "medicineName", planVO.getMedicineName(),
                "dosage", planVO.getDosage()
            ));
            
            if (withTrace) {
                trace.put("tools_called", List.of("spring_plan_create"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
            
        } catch (DateTimeParseException e) {
            logger.warn("日期格式错误", e);
            return errorResult("日期格式错误，请使用 YYYY-MM-DD 格式，例如 2026-03-05", userId, sessionId, withTrace, withTiming, trace);
        } catch (Exception e) {
            logger.error("创建用药计划失败", e);
            return errorResult("创建用药计划失败：" + e.getMessage(), userId, sessionId, withTrace, withTiming, trace);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 从 Object 提取 String 列表
     */
    private List<String> extractStringList(Object obj) {
        if (obj instanceof List<?>) {
            return ((List<?>) obj).stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    /**
     * 安全转换为 String
     */
    private String str(Object obj) {
        if (obj == null) return "";
        return obj.toString().trim();
    }

    /**
     * 创建错误结果
     */
    private Map<String, Object> errorResult(
            String errorMessage,
            String userId,
            String sessionId,
            boolean withTrace,
            boolean withTiming,
            Map<String, Object> trace
    ) {
        memoryRepository.appendMessage(sessionId, userId, "assistant", "抱歉，" + errorMessage);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("assistant_message", errorMessage);
        result.put("need_confirm", false);
        result.put("actions", List.of());
        
        if (withTrace) {
            result.put("trace", trace);
        }
        if (withTiming) {
            result.put("timings", Map.of());
        }
        return result;
    }
}
