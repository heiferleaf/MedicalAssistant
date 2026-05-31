package com.whu.medicalbackend.agent.langchain4j.agents;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.langchain4j.tools.ocr.OcrDrugRecognitionTool;
import com.whu.medicalbackend.agent.langchain4j.tools.family.FamilyAlarmTool;
import com.whu.medicalbackend.agent.langchain4j.tools.family.FamilyHealthSnapshotTool;
import com.whu.medicalbackend.agent.langchain4j.tools.family.FamilyInviteTool;
import com.whu.medicalbackend.agent.langchain4j.tools.family.FamilyQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.medicine.MedicineAddTool;
import com.whu.medicalbackend.agent.langchain4j.tools.medicine.MedicineQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanCreateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanDeleteTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanUpdateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.predict.PredictTool;
import com.whu.medicalbackend.agent.langchain4j.tools.rag.RagTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryHistoryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryTodayTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskUpdateStatusTool;
import com.whu.medicalbackend.agent.langchain4j.core.listener.ToolExecutionBroadcaster;
import com.whu.medicalbackend.agent.langchain4j.core.memory.RedisChatMemory;
import com.whu.medicalbackend.agent.service.ToolExecutionPendingService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 医疗助手 Agent - 支持 Human-in-the-loop + Redis 分布式记忆
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
@ConditionalOnExpression("!'${dashscope.api-key:}'.isBlank()")
public class MedicalAgent {

    private static final Logger logger = LoggerFactory.getLogger(MedicalAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are a medical health assistant. You help users with health questions, medication management, and family health monitoring.

            CAPABILITIES: Internet search enabled through model. Use it for current events, weather, news.

            USER ID: {{userId}} — ALWAYS use this exact userId when calling tools.

            === TOOLS ===

            PLANS: queryPlans / createPlan / updatePlan / deletePlan
            TASKS: getTodayTasks / updateTaskStatus(0=not_taken,1=taken,2=missed) / getHistoryTasks
            FAMILY: queryMyFamilyGroup / getFamilyHealthSnapshot / getFamilyAlarms / inviteFamilyMember
            MEDICINE: queryMyMedicines / addMedicine
            OCR: recognizeDrugFromImage — use when user uploads drug package photo
            PREDICT: predictAdverseReactions / analyzeAdverseReactionRisk
            RAG: queryMedicalKnowledge — for professional medical info

            === GUIDELINES ===

            - Non-medical questions: answer directly, DO NOT use queryMedicalKnowledge.
            - Medical questions: use queryMedicalKnowledge FIRST for professional info.
            - Drug safety/side effects: use predict tools for evidence-based assessment.
            - OCR images: use recognizeDrugFromImage when user uploads drug photo.
            - Summary: after using any tool, summarize results clearly to the user.
            - User ID: always use {{userId}}, never make up a userId.

            === CONFIRMATION REQUIREMENTS ===

            Plans (create/update/delete): DO NOT call the tool immediately. Present details and ask user to confirm (e.g., "\\u8bf7\\u786e\\u8ba4\\u662f\\u5426\\u521b\\u5efa/\\u4fee\\u6539/\\u5220\\u9664\\u6b64\\u7528\\u836f\\u8ba1\\u5212\\uff1f"). Only call the tool after user says yes (\\u597d\\u7684/\\u786e\\u8ba4/yes/\\u53ef\\u4ee5/\\u6267\\u884c).

            === ACTION MARKERS (frontend integration) ===

            - addMedicine: call addMedicine tool, include [ACTION:addMedicine] in response.
            - updateTaskStatus: call updateTaskStatus tool, include [ACTION:updateTaskStatus] in response.
            - After these tools, ALWAYS include the action marker so frontend can show confirmation card.

            === RESPONSE FORMAT ===

            Always use Markdown: **bold** for important info/warnings, *italic* for emphasis, `code` for medical terms/dosages, bullet/numbered lists for organization, headings for long responses.
            """;

    private final MedicalExpert medicalExpert;
    private final StreamingMedicalExpert streamingMedicalExpert;

    @Autowired
    private ToolExecutionPendingService toolExecutionPendingService;

    @Autowired
    private ToolExecutionBroadcaster toolExecutionBroadcaster;

    // 需要用户批准的 tool 名称
    private static final Set<String> REQUIRES_APPROVAL_TOOLS = new HashSet<>(Arrays.asList(
        "createPlan", "updatePlan", "deletePlan",
        "addMedicine", "updateTaskStatus"
    ));

    @Autowired
    public MedicalAgent(ChatModel chatModel,
                        StreamingChatModel streamingChatModel,
                        PlanQueryTool planQueryTool,
                        PlanCreateTool planCreateTool,
                        PlanUpdateTool planUpdateTool,
                        PlanDeleteTool planDeleteTool,
                        TaskQueryTodayTool taskQueryTodayTool,
                        TaskUpdateStatusTool taskUpdateStatusTool,
                        TaskQueryHistoryTool taskQueryHistoryTool,
                        FamilyQueryTool familyQueryTool,
                        FamilyHealthSnapshotTool familyHealthSnapshotTool,
                        FamilyAlarmTool familyAlarmTool,
                        FamilyInviteTool familyInviteTool,
                        MedicineQueryTool medicineQueryTool,
                        MedicineAddTool medicineAddTool,
                        PredictTool predictTool,
                        RagTool ragTool,
                        OcrDrugRecognitionTool ocrDrugRecognitionTool,
                        StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper) {

        Object[] allTools = new Object[]{
                planQueryTool, planCreateTool, planUpdateTool, planDeleteTool,
                taskQueryTodayTool, taskUpdateStatusTool, taskQueryHistoryTool,
                familyQueryTool, familyHealthSnapshotTool, familyAlarmTool, familyInviteTool,
                medicineQueryTool, medicineAddTool,
                predictTool, ragTool, ocrDrugRecognitionTool
        };

        this.medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> new RedisChatMemory(
                        memoryId, redisTemplate, objectMapper, 10))
                .tools(allTools)
                .build();

        this.streamingMedicalExpert = AiServices.builder(StreamingMedicalExpert.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> new RedisChatMemory(
                        memoryId, redisTemplate, objectMapper, 10))
                .tools(allTools)
                .build();
    }

    public interface MedicalExpert {
        @SystemMessage(SYSTEM_PROMPT)
        String medical(@MemoryId String memoryId, @V("userId") String userId, @UserMessage String message);
    }

    public interface StreamingMedicalExpert {
        @SystemMessage(SYSTEM_PROMPT)
        TokenStream medical(@MemoryId String memoryId, @V("userId") String userId, @UserMessage String message);
    }

    /**
     * 执行 Agent - 支持 Human-in-the-loop
     */
    public Map<String, Object> execute(String sessionId, String userId, String userMessage, String aiMessage) {
        try {
            logger.info("执行 Agent 推理，sessionId: {}, userId: {}, message: {}", sessionId, userId, userMessage);

            String response = chat(sessionId, userId, userMessage);
            logger.info("Agent 文本回复：{}", response);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", response);

            // 检查是否有待确认的请求
            List<?> pendingRequests = toolExecutionPendingService.getUserPendingRequests(Long.parseLong(userId));
            if (pendingRequests != null && !pendingRequests.isEmpty()) {
                Object pending = pendingRequests.get(0);
                logger.info("检测到待确认请求：{}", pending);

                if (pending instanceof Map) {
                    Map<?, ?> pendingMap = (Map<?, ?>) pending;
                    String actionType = (String) pendingMap.get("action_type");
                    String toolArgsJson = (String) pendingMap.get("tool_args_json");

                    if (actionType != null && toolArgsJson != null) {
                        result.put("action_type", actionType);
                        result.put("action_data", toolArgsJson);
                        logger.info("返回 action 信息：actionType={}, actionData={}", actionType, toolArgsJson);
                    }
                }
            }

            result.put("need_confirm", false);
            result.put("actions", List.of());

            return result;
        } catch (Exception e) {
            logger.error("Agent 执行失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "执行失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 对话方法 - 使用 Redis 分布式记忆
     */
    public String chat(String sessionId, String userId, String message) {
        logger.info("执行医疗助手对话：sessionId={}, userId={}, message 长度={}", sessionId, userId, message.length());

        if (message.contains("图片数据：")) {
            int base64Start = message.indexOf("图片数据：") + 5;
            String base64Preview = message.substring(base64Start, Math.min(base64Start + 100, message.length()));
            logger.info("检测到图片消息，Base64 前 100 字符：{}", base64Preview);
        }

        toolExecutionBroadcaster.setCurrentSession(sessionId);

        String memoryId = userId + "_" + sessionId;
        try {
            return medicalExpert.medical(memoryId, userId, message);
        } finally {
            toolExecutionBroadcaster.clearCurrentSession();
        }
    }

    /**
     * 流式对话方法 - 使用 StreamingChatModel 进行真实流式输出
     */
    public void chatStream(String sessionId, String userId, String message,
                           java.util.function.Consumer<String> onPartialResponse,
                           java.util.function.Consumer<dev.langchain4j.data.message.AiMessage> onCompleteResponse,
                           java.util.function.Consumer<Throwable> onError) {
        logger.info("执行医疗助手流式对话：sessionId={}, userId={}", sessionId, userId);

        if (message.contains("图片数据：")) {
            int base64Start = message.indexOf("图片数据：") + 5;
            String base64Preview = message.substring(base64Start, Math.min(base64Start + 100, message.length()));
            logger.info("检测到图片消息，Base64 前 100 字符：{}", base64Preview);
        }

        toolExecutionBroadcaster.setCurrentSession(sessionId);
        String memoryId = userId + "_" + sessionId;

        streamingMedicalExpert.medical(memoryId, userId, message)
                .onPartialResponse(onPartialResponse != null ? onPartialResponse : t -> {})
                .onCompleteResponse(response -> {
                    try {
                        if (onCompleteResponse != null) {
                            onCompleteResponse.accept(response.aiMessage());
                        }
                    } finally {
                        toolExecutionBroadcaster.clearCurrentSession();
                    }
                })
                .onError(error -> {
                    try {
                        logger.error("医疗助手流式对话错误", error);
                        if (onError != null) {
                            onError.accept(error);
                        }
                    } finally {
                        toolExecutionBroadcaster.clearCurrentSession();
                    }
                })
                .start();
    }

    /**
     * 处理 Tool 执行 - 需要批准的拦截
     */
    public Object handleToolExecution(String toolName, Map<String, Object> arguments, String userId, String sessionId, String aiMessage) {
        logger.info("拦截 Tool 执行：toolName={}, userId={}", toolName, userId);

        if (REQUIRES_APPROVAL_TOOLS.contains(toolName)) {
            logger.info("Tool 需要用户批准：{}", toolName);

            try {
                dev.langchain4j.agent.tool.ToolExecutionRequest request =
                    dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .name(toolName)
                        .arguments(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(arguments))
                        .build();

                String requestId = toolExecutionPendingService.savePendingRequest(
                    Long.parseLong(userId),
                    sessionId,
                    request,
                    aiMessage
                );

                logger.info("已保存待确认请求：requestId={}", requestId);

                Map<String, Object> pendingResponse = new LinkedHashMap<>();
                pendingResponse.put("success", true);
                pendingResponse.put("pending_confirmation", true);
                pendingResponse.put("request_id", requestId);
                pendingResponse.put("tool_name", toolName);
                pendingResponse.put("arguments", arguments);
                pendingResponse.put("message", "请确认是否" + getActionMessage(toolName));

                return pendingResponse;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                logger.error("序列化 Tool 参数失败", e);
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("success", false);
                error.put("message", "参数序列化失败：" + e.getMessage());
                return error;
            }
        }

        return null;
    }

    private String getActionMessage(String toolName) {
        switch (toolName) {
            case "createPlan": return "创建此用药计划";
            case "updatePlan": return "修改此用药计划";
            case "deletePlan": return "删除此用药计划";
            case "addMedicine": return "添加此药品到药箱";
            case "updateTaskStatus": return "更新此用药任务状态";
            default: return "执行此操作";
        }
    }
}
