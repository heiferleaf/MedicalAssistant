package com.whu.medicalbackend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whu.medicalbackend.agent.engine.MultiTurnExecutionEngine;
import com.whu.medicalbackend.agent.flask.FlaskRagProxyService;
import com.whu.medicalbackend.agent.llm.AgentDecision;
import com.whu.medicalbackend.agent.llm.AgentPromptTemplate;
import com.whu.medicalbackend.agent.llm.DashScopeApiService;
import com.whu.medicalbackend.agent.llm.FunctionCallResult;
import com.whu.medicalbackend.agent.llm.ToolDefinition;
import com.whu.medicalbackend.agent.memory.AgentMemoryRepository;
import com.whu.medicalbackend.agent.router.AgentRouter;
import com.whu.medicalbackend.agent.tools.PlanTools;
import com.whu.medicalbackend.agent.tools.ToolRegistry;
import com.whu.medicalbackend.dto.PlanCreateDTO;
import com.whu.medicalbackend.dto.PlanVO;
import com.whu.medicalbackend.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AgentOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestratorService.class);

    public static final String AGENT_VERSION = "schemeA_spring_memory_v1_llm_2026-03-04";

    private final AgentMemoryRepository memoryRepository;
    private final FlaskRagProxyService flaskRagProxyService;
    private final PlanService planService;
    private final ObjectMapper objectMapper;
    private final String flaskBaseUrl;
    private final ToolRegistry toolRegistry;
    private final PlanTools planTools;
    private final DashScopeApiService llmService;
    private final MultiTurnExecutionEngine multiTurnEngine;
    private final boolean llmEnabled;

    public AgentOrchestratorService(
            AgentMemoryRepository memoryRepository,
            FlaskRagProxyService flaskRagProxyService,
            PlanService planService,
            ObjectMapper objectMapper,
            DashScopeApiService llmService,
            MultiTurnExecutionEngine multiTurnEngine,
            @Value("${flask.base-url:http://127.0.0.1:8001}") String flaskBaseUrl,
            @Value("${agent.llm.enabled:false}") boolean llmEnabled
    ) {
        this.memoryRepository = memoryRepository;
        this.flaskRagProxyService = flaskRagProxyService;
        this.planService = planService;
        this.objectMapper = objectMapper;
        this.flaskBaseUrl = flaskBaseUrl;
        this.llmService = llmService;
        this.multiTurnEngine = multiTurnEngine;
        this.llmEnabled = llmEnabled;
        this.toolRegistry = new ToolRegistry();
        this.planTools = new PlanTools(planService, memoryRepository, objectMapper);
        
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

        logger.info("AgentOrchestratorService initialized, LLM enabled: {}", llmEnabled);
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

        // 使用LLM进行意图识别（如果启用）
        if (llmEnabled && llmService.isConfigured()) {
            return handleLlmChat(userId, sessionId, message, withTrace, withTiming);
        }

        // 回退到正则匹配
        return handleRuleBasedChat(userId, sessionId, message, withTrace, withTiming);
    }

    private Map<String, Object> handleLlmChat(String userId, String sessionId, String message, boolean withTrace, boolean withTiming) {
        try {
            // 获取所有工具定义（包括 RAG、PlanCreate、WebSearch 和 Plan 工具）
            ArrayNode tools = ToolDefinition.getAllTools(objectMapper);
            // 添加 Plan 相关工具
            ArrayNode allPlanTools = this.planTools.getAllPlanTools();
            for (int i = 0; i < allPlanTools.size(); i++) {
                tools.add(allPlanTools.get(i));
            }
            
            // 使用多轮执行引擎
            logger.info("使用多轮执行引擎处理请求");
            return multiTurnEngine.execute(userId, sessionId, message, tools, withTrace, withTiming);
            
        } catch (Exception e) {
            logger.error("多轮执行引擎失败，回退到单轮执行", e);
            return handleSingleTurnLlmChat(userId, sessionId, message, withTrace, withTiming);
        }
    }
    
    /**
     * 单轮 LLM 调用（回退方案）
     */
    private Map<String, Object> handleSingleTurnLlmChat(String userId, String sessionId, String message, boolean withTrace, boolean withTiming) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("agent_version", AGENT_VERSION);
        trace.put("control", "llm_single_turn");

        try {
            List<Map<String, String>> history = getConversationHistory(sessionId, 5);

            // 获取所有工具定义（包括 RAG、PlanCreate、WebSearch 和 Plan 工具）
            ArrayNode tools = ToolDefinition.getAllTools(objectMapper);
            // 添加 Plan 相关工具
            ArrayNode allPlanTools = this.planTools.getAllPlanTools();
            for (int i = 0; i < allPlanTools.size(); i++) {
                tools.add(allPlanTools.get(i));
            }
            
            // 使用 Function Call 调用 LLM
            FunctionCallResult llmResult = llmService.chatWithFunction(
                AgentPromptTemplate.SYSTEM_PROMPT, 
                message, 
                history,
                tools
            );
            
            logger.info("LLM FunctionCall Result: {}", llmResult);

            if (withTrace) {
                trace.put("llm_result", llmResult.toString());
            }

            // 根据 Function Call 结果执行相应操作
            if (llmResult.isFunctionCall()) {
                return handleToolCall(llmResult.getName(), llmResult.getArguments(), null,
                        userId, sessionId, withTrace, withTiming, trace);
            } else {
                return handleDirectAnswer(llmResult.getContent(), userId, sessionId, withTrace, withTiming, trace);
            }

        } catch (Exception e) {
            logger.error("LLM 调用失败，回退到正则匹配", e);
            return handleRuleBasedChat(userId, sessionId, message, withTrace, withTiming);
        }
    }

    private List<Map<String, String>> getConversationHistory(String sessionId, int limit) {
        List<Map<String, Object>> rows = memoryRepository.getRecentMessages(sessionId, limit);
        List<Map<String, String>> history = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            history.add(Map.of(
                    "role", str(row.get("role")),
                    "content", str(row.get("content"))
            ));
        }
        return history;
    }

    private AgentDecision parseLlmResponse(String llmResponse) {
        try {
            // 尝试解析JSON格式的响应
            if (llmResponse.contains("{") && llmResponse.contains("}")) {
                int start = llmResponse.indexOf("{");
                int end = llmResponse.lastIndexOf("}");
                String jsonStr = llmResponse.substring(start, end + 1);
                Map<String, Object> json = objectMapper.readValue(jsonStr, Map.class);

                String intent = str(json.get("intent"));
                String msg = str(json.get("message"));

                switch (intent) {
                    case "direct_answer":
                        return AgentDecision.directAnswer(msg);
                    case "tool_call":
                        String toolName = str(json.get("tool_name"));
                        Map<String, Object> toolArgs = (Map<String, Object>) json.get("tool_args");
                        return AgentDecision.toolCall(toolName, toolArgs, msg);
                    case "need_confirm":
                        Map<String, Object> preview = (Map<String, Object>) json.get("preview");
                        return AgentDecision.needConfirm(preview, msg);
                    default:
                        return AgentDecision.directAnswer(msg);
                }
            }
            // 如果不是JSON格式，直接作为回答返回
            return AgentDecision.directAnswer(llmResponse);
        } catch (Exception e) {
            logger.warn("解析LLM响应失败: {}", e.getMessage());
            return AgentDecision.directAnswer(llmResponse);
        }
    }

    private Map<String, Object> executeDecision(AgentDecision decision, String userId, String sessionId,
                                                  String message, boolean withTrace, boolean withTiming,
                                                  Map<String, Object> trace) {
        switch (decision.getIntentType()) {
            case DIRECT_ANSWER:
                return handleDirectAnswer(decision.getMessage(), userId, sessionId, withTrace, withTiming, trace);
            case TOOL_CALL:
                return handleToolCall(decision.getToolName(), decision.getToolArgs(), decision.getMessage(),
                        userId, sessionId, withTrace, withTiming, trace);
            case NEED_CONFIRM:
                return handleNeedConfirm(decision.getPreview(), decision.getMessage(),
                        userId, sessionId, withTrace, withTiming, trace);
            case CLARIFICATION:
                return handleClarification(decision.getMessage(), userId, sessionId, withTrace, withTiming, trace);
            default:
                return handleDirectAnswer("我暂时无法理解你的请求，请换个说法。",
                        userId, sessionId, withTrace, withTiming, trace);
        }
    }

    private Map<String, Object> handleDirectAnswer(String answer, String userId, String sessionId,
                                                    boolean withTrace, boolean withTiming, Map<String, Object> trace) {
        memoryRepository.appendMessage(sessionId, userId, "assistant", answer);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("assistant_message", answer);
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

    private Map<String, Object> handleToolCall(String toolName, Map<String, Object> toolArgs, String message,
                                                String userId, String sessionId,
                                                boolean withTrace, boolean withTiming, Map<String, Object> trace) {
        if ("rag.query".equals(toolName)) {
            String question = str(toolArgs.get("question"));
            
            String assistantMessage;
            Map<String, Object> rawData;
            
            try {
                Map<String, Object> ragResp = flaskRagProxyService.query(question, withTrace, withTiming);
                assistantMessage = str(ragResp.get("answer")).trim();
                rawData = Map.of("rag", ragResp);
            } catch (Exception e) {
                logger.warn("RAG服务调用失败: {}", e.getMessage());
                assistantMessage = "抱歉，知识库服务暂时不可用。请稍后再试，或尝试其他问题。";
                rawData = Map.of("rag_error", e.getMessage());
            }
            
            if (assistantMessage.isBlank()) {
                assistantMessage = "我没能生成回答，请换个问法再试一次。";
            }

            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("raw", rawData);

            if (withTrace) {
                trace.put("tools_called", List.of("rag.query"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
        }
        
        if ("web_search".equals(toolName)) {
            String query = str(toolArgs.get("query"));
            logger.info("执行联网搜索：{}", query);
            
            // 注意：由于已经启用了 enable_search=true，LLM 会直接获取搜索结果并生成回答
            // 这里我们再次调用 LLM，让它基于搜索结果生成最终回答
            String assistantMessage;
            try {
                // 构造搜索提示
                String searchPrompt = "请搜索\"" + query + "\"并提供详细信息。";
                
                // 直接调用 LLM（已经启用搜索功能）
                FunctionCallResult searchResult = llmService.chatWithFunction(
                    "你是一个专业的信息搜索助手，请提供准确、详细的信息。",
                    searchPrompt,
                    Collections.emptyList(),
                    null  // 不需要工具，直接让 LLM 搜索并回答
                );
                
                if (searchResult.isFunctionCall()) {
                    // 如果还返回工具调用，直接回复
                    assistantMessage = "我正在为您搜索\"" + query + "\"的相关信息...";
                } else {
                    assistantMessage = searchResult.getContent();
                }
                
            } catch (Exception e) {
                logger.warn("联网搜索失败：{}", e.getMessage());
                assistantMessage = "抱歉，联网搜索服务暂时不可用，请稍后再试。";
            }
            
            memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", assistantMessage);
            result.put("need_confirm", false);
            result.put("actions", List.of());
            result.put("search_query", query);
            
            if (withTrace) {
                trace.put("tools_called", List.of("web_search"));
                result.put("trace", trace);
            }
            if (withTiming) {
                result.put("timings", Map.of());
            }
            return result;
        }

        // ========== Plan 相关工具 ==========
        Map<String, Object> planResult = planTools.executeTool(
            toolName, toolArgs, userId, sessionId, withTrace, withTiming, trace
        );
        if (planResult != null) {
            return planResult;
        }

        // 其他工具调用，生成确认
        return handleNeedConfirm(toolArgs, message, userId, sessionId, withTrace, withTiming, trace);
    }

    private Map<String, Object> handleNeedConfirm(Map<String, Object> preview, String message,
                                                   String userId, String sessionId,
                                                   boolean withTrace, boolean withTiming, Map<String, Object> trace) {
        String actionId = "act_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ObjectNode previewNode = objectMapper.createObjectNode();
        if (preview != null) {
            for (Map.Entry<String, Object> entry : preview.entrySet()) {
                previewNode.putPOJO(entry.getKey(), entry.getValue());
            }
        }

        JsonNode toolArgs = objectMapper.valueToTree(preview);

        memoryRepository.savePendingAction(
                actionId,
                sessionId,
                userId,
                "spring.plan.create",
                previewNode,
                toolArgs,
                Duration.ofSeconds(300)
        );

        String assistantMessage = message != null ? message : "我将为你创建如下用药提醒/计划，请确认是否保存：";
        memoryRepository.appendMessage(sessionId, userId, "assistant", assistantMessage);

        Map<String, Object> confirm = new LinkedHashMap<>();
        confirm.put("action_id", actionId);
        confirm.put("action_type", "spring.plan.create");
        confirm.put("preview", preview);
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

    private Map<String, Object> handleClarification(String message, String userId, String sessionId,
                                                      boolean withTrace, boolean withTiming, Map<String, Object> trace) {
        memoryRepository.appendMessage(sessionId, userId, "assistant", message);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("assistant_message", message);
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

    // 回退到正则匹配的方法（原逻辑）
    private Map<String, Object> handleRuleBasedChat(String userId, String sessionId, String message,
                                                     boolean withTrace, boolean withTiming) {

        AgentRouter.Decision decision = AgentRouter.routeMessage(message, userId, sessionId);

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("agent_version", AGENT_VERSION);
        trace.put("control", "rule");
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
        out.put("control_mode", llmEnabled ? "llm" : "rule");
        out.put("llm_configured", llmService.isConfigured());
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
