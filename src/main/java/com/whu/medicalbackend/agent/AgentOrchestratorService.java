package com.whu.medicalbackend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.memory.AgentMemoryRepository;
import com.whu.medicalbackend.agent.router.AgentRouter;
import com.whu.medicalbackend.agent.tools.ToolRegistry;
import com.whu.medicalbackend.dto.PlanCreateDTO;
import com.whu.medicalbackend.dto.PlanVO;
import com.whu.medicalbackend.service.PlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AgentOrchestratorService {

    public static final String AGENT_VERSION = "schemeA_spring_memory_v0_2026-02-19";

    private final AgentMemoryRepository memoryRepository;
    private final FlaskRagProxyService flaskRagProxyService;
    private final PlanService planService;
    private final ObjectMapper objectMapper;
    private final String flaskBaseUrl;
    private final ToolRegistry toolRegistry;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            PlanService planService,
            ObjectMapper objectMapper,
            @Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl
    ) {
        this.memoryRepository = memoryRepository;
        this.flaskRagProxyService = flaskRagProxyService;
        this.planService = planService;
        this.objectMapper = objectMapper;
        this.flaskBaseUrl = flaskBaseUrl;
        this.toolRegistry = new ToolRegistry();
        
        // Register tools
        toolRegistry.register("rag.query", "Query RAG service for medical information", params -> {
            String question = str(params.get("question"));
            boolean withTrace = bool(params.get("with_trace"));
            boolean withTiming = bool(params.get("with_timing"));
            return flaskRagProxyService.query(question, withTrace, withTiming);
        });
        
        toolRegistry.register("spring.plan.create", "Create medication reminder plan", params -> {
            // Implementation will be called directly in confirm method
            return Map.of();
        });
    }

    public Map<String, Object> chat(Map<String, Object> payload) {
        String userId = str(payload.get("user_id"));
        String sessionId = str(payload.get("session_id"));
        String message = str(payload.get("message"));
        boolean withTrace = bool(payload.get("with_trace"));
        boolean withTiming = bool(payload.get("with_timing"));

        if (userId.isBlank() || sessionId.isBlank()) {
            return Map.of("success", false, "error", "user_id 和 session_id 不能为空", "status", 400);
        }
        if (message.isBlank()) {
            return Map.of("success", false, "error", "message 不能为空", "status", 400);
        }

        memoryRepository.appendMessage(sessionId, userId, "user", message);

        AgentRouter.Decision decision = AgentRouter.routeMessage(message, userId, sessionId);

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("agent_version", AGENT_VERSION);
        if (withTrace) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("intent", decision.intent());
            d.put("need_confirm", decision.needConfirm());
            trace.put("decision", d);
        }

        if (decision.clarification() != null && !decision.clarification().isBlank()) {
            String assistantMessage = decision.clarification();
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
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

        if ("rag.query".equals(decision.intent())) {
            String question = str(decision.toolArgs().get("question"));
            Map<String, Object> ragResp;
            
            // Use tool registry to get and call the rag.query tool
            ToolRegistry.ToolInfo toolInfo = toolRegistry.get("rag.query");
            if (toolInfo != null) {
                Map<String, Object> toolParams = new HashMap<>();
                toolParams.put("question", question);
                toolParams.put("with_trace", withTrace);
                toolParams.put("with_timing", withTiming);
                ragResp = toolInfo.function().apply(toolParams);
            } else {
                // Fallback to direct call
                ragResp = flaskRagProxyService.query(question, withTrace, withTiming);
            }
            
            String assistantMessage = str(ragResp.get("answer")).trim();
            if (assistantMessage.isBlank()) {
                assistantMessage = "我没能生成回答，请换个问法再试一次。";
            }

            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", bool(ragResp.getOrDefault("success", true)));
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("raw", Map.of("rag", ragResp));
            if (withTrace) {
                trace.put("tools_called", List.of("flask.rag.query"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
        }

        if ("spring.plan.create.preview".equals(decision.intent())) {
            String actionId = "act_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            ObjectNode preview = objectMapper.createObjectNode();
            preview.put("medicineName", str(decision.toolArgs().get("medicineName")));
            preview.put("dosage", str(decision.toolArgs().get("dosage")));
            preview.put("startDate", str(decision.toolArgs().get("startDate")));
            if (decision.toolArgs().get("endDate") != null) preview.put("endDate", str(decision.toolArgs().get("endDate")));
            preview.set("timePoints", objectMapper.valueToTree(decision.toolArgs().get("timePoints")));
            if (decision.toolArgs().get("remark") != null) preview.put("remark", str(decision.toolArgs().get("remark")));

            JsonNode toolArgs = objectMapper.valueToTree(decision.toolArgs());

            memoryRepository.savePendingAction(
                    actionId,
                    sessionId,
                    userId,
                    "spring.plan.create",
                    preview,
                    toolArgs,
                    Duration.ofSeconds(300)
            );

            String assistantMessage = "我将为你创建如下用药提醒/计划，请确认是否保存：";
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

            Map<String, Object> confirm = new LinkedHashMap<>();
            confirm.put("action_id", actionId);
            confirm.put("action_type", "spring.plan.create");
            confirm.put("preview", objectMapper.convertValue(preview, Map.class));
            confirm.put("expires_in_sec", 300);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", true);
            result.put("confirm", confirm);
            result.put("actions", List.of());
            if (withTrace) {
                trace.put("tools_called", List.of("spring.plan.create.preview"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
        }

        String assistantMessage = "我暂时无法处理这个请求。";
        memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("assistant_message", assistantMessage);
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

    public Map<String, Object> confirm(Map<String, Object> payload) {
        String userId = str(payload.get("user_id"));
        String sessionId = str(payload.get("session_id"));
        String actionId = str(payload.get("action_id"));
        Boolean confirmVal = payload.get("confirm") == null ? null : bool(payload.get("confirm"));
        boolean withTrace = bool(payload.get("with_trace"));
        boolean withTiming = bool(payload.get("with_timing"));

        if (userId.isBlank() || sessionId.isBlank()) {
            return Map.of("success", false, "error", "user_id 和 session_id 不能为空", "status", 400);
        }
        if (actionId.isBlank()) {
            return Map.of("success", false, "error", "action_id 不能为空", "status", 400);
        }
        if (confirmVal == null) {
            return Map.of("success", false, "error", "confirm 不能为空（true/false）", "status", 400);
        }

        var opt = memoryRepository.getPendingAction(actionId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "error", "action_id 不存在或已过期", "status", 404);
        }
        var pending = opt.get();

        if (!Objects.equals(pending.userId(), userId) || !Objects.equals(pending.sessionId(), sessionId)) {
            return Map.of("success", false, "error", "action_id 不匹配", "status", 403);
        }
        if (!"pending".equals(pending.status())) {
            return Map.of("success", false, "error", "action 状态不可确认: " + pending.status(), "status", 409);
        }

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("agent_version", AGENT_VERSION);
        if (withTrace) {
            trace.put("tools_called", List.of());
        }

        if (!confirmVal) {
            memoryRepository.updatePendingActionStatus(actionId, "canceled", null);
            String assistantMessage = "好的，已取消本次操作。";
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
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

        if ("spring.plan.create".equals(pending.actionType())) {
            JsonNode toolArgs = memoryRepository.parseJson(pending.toolArgsJson());

            Long userIdLong;
            try {
                userIdLong = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                memoryRepository.updatePendingActionStatus(actionId, "failed", objectMapper.createObjectNode().put("error", "user_id 必须是数字才能创建计划"));
                return Map.of("success", false, "error", "user_id 必须是数字才能创建计划", "status", 400);
            }

            PlanCreateDTO dto = new PlanCreateDTO();
            dto.setMedicineName(str(toolArgs.get("medicineName")));
            String dosage = toolArgs.hasNonNull("dosage") ? toolArgs.get("dosage").asText() : "";
            dosage = dosage == null ? "" : dosage.trim();
            dto.setDosage(dosage.isBlank() ? "按医嘱" : dosage);

            try {
                dto.setStartDate(LocalDate.parse(str(toolArgs.get("startDate"))));
            } catch (Exception e) {
                dto.setStartDate(LocalDate.now());
            }

            if (toolArgs.hasNonNull("endDate")) {
                try {
                    dto.setEndDate(LocalDate.parse(toolArgs.get("endDate").asText()));
                } catch (Exception ignored) {
                }
            }

            List<LocalTime> timePoints = new ArrayList<>();
            if (toolArgs.has("timePoints") && toolArgs.get("timePoints").isArray()) {
                for (JsonNode n : toolArgs.get("timePoints")) {
                    try {
                        timePoints.add(LocalTime.parse(n.asText()));
                    } catch (DateTimeParseException ignored) {
                    }
                }
            }
            dto.setTimePoints(timePoints);
            dto.setRemark(toolArgs.hasNonNull("remark") ? toolArgs.get("remark").asText() : null);

            try {
                PlanVO planVO = planService.createPlan(userIdLong, dto);

                ObjectNode resultJson = objectMapper.createObjectNode();
                resultJson.put("success", true);
                resultJson.set("data", objectMapper.valueToTree(planVO));
                memoryRepository.updatePendingActionStatus(actionId, "done", resultJson);

                String assistantMessage = "好的，已为你创建用药计划。";
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

                Map<String, Object> action = new LinkedHashMap<>();
                action.put("type", "spring.plan.create");
                action.put("status", "ok");
                action.put("data", planVO);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("assistant_message", assistantMessage);
                result.put("need_confirm", false);
                result.put("actions", List.of(action));
                if (withTrace) {
                    trace.put("tools_called", List.of("spring.plan.create"));
                    result.put("trace", trace);
                }
                if (withTiming) {
                    result.put("timings", Map.of());
                }
                return result;
            } catch (Exception e) {
                ObjectNode resultJson = objectMapper.createObjectNode();
                resultJson.put("success", false);
                resultJson.put("error", e.getMessage());
                memoryRepository.updatePendingActionStatus(actionId, "failed", resultJson);

                String assistantMessage = "创建失败：" + (e.getMessage() == null ? "unknown" : e.getMessage());
                memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

                Map<String, Object> action = new LinkedHashMap<>();
                action.put("type", "spring.plan.create");
                action.put("status", "error");
                action.put("data", objectMapper.convertValue(resultJson, Map.class));

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("assistant_message", assistantMessage);
                result.put("need_confirm", false);
                result.put("actions", List.of(action));
                result.put("status", 500);
                if (withTrace) {
                    trace.put("tools_called", List.of("spring.plan.create"));
                    result.put("trace", trace);
                }
                if (withTiming) {
                    result.put("timings", Map.of());
                }
                return result;
            }
        }

        return Map.of("success", false, "error", "不支持的 action_type: " + pending.actionType(), "status", 400);
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("module", "agent");
        out.put("agent_version", AGENT_VERSION);
        out.put("memory", Map.of("backend", "spring.jdbc.mysql", "tables", List.of("agent_sessions", "agent_messages", "agent_pending_actions")));
        out.put("rag", Map.of("target", flaskBaseUrl + "/rag/query"));
        out.put("server_time", java.time.OffsetDateTime.now().toString());
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }

    private static String str(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }
}
