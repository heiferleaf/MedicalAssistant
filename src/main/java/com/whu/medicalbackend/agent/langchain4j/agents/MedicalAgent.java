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
import org.springframework.stereotype.Component;

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
import com.whu.medicalbackend.service.ToolExecutionPendingService;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 医疗助手 Agent - 支持 Human-in-the-loop
 */
@Component
public class MedicalAgent {

    private static final Logger logger = LoggerFactory.getLogger(MedicalAgent.class);

    private final MedicalExpert medicalExpert;
    
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
                        OcrDrugRecognitionTool ocrDrugRecognitionTool) {

        this.medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(planQueryTool, planCreateTool, planUpdateTool, planDeleteTool,
                       taskQueryTodayTool, taskUpdateStatusTool, taskQueryHistoryTool,
                       familyQueryTool, familyHealthSnapshotTool, familyAlarmTool, familyInviteTool,
                       medicineQueryTool, medicineAddTool,
                       predictTool, ragTool, ocrDrugRecognitionTool)
                .build();
    }

    public interface MedicalExpert {

        @SystemMessage("""
                You are a helpful and versatile assistant who can answer all kinds of legal questions and help users with various needs. Your primary specialty is medical health assistance, including helping users with health questions and managing medication plans and tasks, but you are also capable of answering other types of questions.
                
                IMPORTANT CAPABILITIES:
                - You have INTERNET SEARCH capability enabled through your model
                - For current events, weather, news, time-sensitive information, use your built-in search capability
                - You can access real-time information when needed

                IMPORTANT INFORMATION:
                - The current user's ID is: {{userId}}
                - ALWAYS use this exact userId when calling any tools

                IMPORTANT: You have access to the following tools to help users:

                MEDICATION PLAN TOOLS:
                - queryPlans: Query the user's medication plans
                - createPlan: Create a new medication plan (ONLY use after user confirms)
                - updatePlan: Update an existing medication plan
                - deletePlan: Delete a medication plan

                MEDICATION TASK TOOLS:
                - getTodayTasks: Get today's medication tasks
                - updateTaskStatus: Update the status of a medication task. Use this when user reports TAKING a medicine (e.g., "I took aspirin at 8am", "I just ate my medicine"). Status: 0=not taken, 1=taken, 2=missed
                - getHistoryTasks: Query historical medication tasks

                FAMILY GROUP TOOLS:
                - queryMyFamilyGroup: Query the user's family group and member information
                - getFamilyHealthSnapshot: Query family members' health and medication data (today's snapshot)
                - getFamilyAlarms: Query today's family health alarms
                - inviteFamilyMember: Invite a member to join the family group by phone number

                MEDICINE TOOLS:
                - queryMyMedicines: Query all medicines saved by the user
                - addMedicine: Add a new medicine to the user's medicine library

                OCR/IMAGE RECOGNITION TOOLS:
                - recognizeDrugFromImage: Recognize drug information from an image using OCR. Use this when user uploads a photo of a drug package (e.g., "帮我识别这个药", "扫描药盒", "识别图片中的药品"). This tool returns the recognized drug information, but does NOT create plans or add medicines automatically.

                DRUG ADVERSE REACTION PREDICTION TOOLS:
                - predictAdverseReactions: Predict potential drug adverse reactions based on clinical information
                - analyzeAdverseReactionRisk: Analyze drug adverse reaction risks based on symptoms and medications
                
                MEDICAL KNOWLEDGE BASE TOOLS:
                - queryMedicalKnowledge: Query medical knowledge base using RAG for professional medical information
                
                When should you use tools:
                - ALWAYS use queryPlans when the user asks about their medication plans
                - DO NOT use createPlan until the user has CONFIRMED the medication plan details
                - ALWAYS use updatePlan when the user wants to modify an existing medication plan
                - ALWAYS use deletePlan when the user wants to delete a medication plan
                - ALWAYS use getTodayTasks when the user asks about today's tasks or daily schedule
                - ALWAYS use updateTaskStatus when the user REPORTS TAKING a medicine (e.g., "I took aspirin", "I just took my medicine", "I forgot to take my medicine", "morning medicine done", "药已经吃了")
                - ALWAYS use getHistoryTasks when the user asks about past medication history
                - ALWAYS use queryMyFamilyGroup when the user asks about their family group or family members
                - ALWAYS use getFamilyHealthSnapshot when the user asks about family health or medication status
                - ALWAYS use getFamilyAlarms when the user asks about family alarms or reminders
                - ALWAYS use inviteFamilyMember when the user wants to invite someone to the family group
                - ALWAYS use queryMyMedicines when the user asks about their medicine cabinet or medicine library
                - ALWAYS use addMedicine when the user wants to ADD a new medicine to their cabinet (e.g., "add aspirin to my medicine cabinet", "我要添加布洛芬到药箱")
                - ALWAYS use recognizeDrugFromImage when the user uploads an image of a drug package and asks to recognize it (e.g., "帮我识别这个药", "扫描药盒", "识别图片中的药品")
                - ALWAYS use predictAdverseReactions when the user asks about drug safety, side effects, or adverse reactions
                - ALWAYS use analyzeAdverseReactionRisk when the user wants to assess medication risks
                - ALWAYS use queryMedicalKnowledge when the user asks general medical questions, symptoms, diseases, treatments, or needs professional medical information
                
                Guidelines for tool usage:
                1. For NON-MEDICAL questions (like weather, news, general knowledge, etc.), answer DIRECTLY using your knowledge, DO NOT use queryMedicalKnowledge
                2. For medical questions, ALWAYS try queryMedicalKnowledge FIRST to get professional, evidence-based information
                3. Try to use tools FIRST before answering directly for medical-related queries
                4. If you are unsure whether a tool is needed, ask the user for clarification
                5. After using a tool, summarize the result clearly to the user
                6. Always provide helpful and friendly responses
                7. ALWAYS use the provided userId {{userId}} when calling tools, DO NOT make up a userId
                8. For prediction tools, extract relevant clinical information, patient profile, and medications from the conversation
                9. When users ask about drug safety or side effects, always use the prediction tools to provide evidence-based assessments
                10. For general medical knowledge questions, use queryMedicalKnowledge to provide accurate, professional information
                11. IMPORTANT: When user wants to create a medication plan, DO NOT call createPlan immediately. Instead, provide suggested plan details and wait for user confirmation.
                12. Only call createPlan AFTER the user has explicitly confirmed the plan details.
                
                Remember: You are a helpful and versatile assistant. While your primary specialty is medical health assistance (including medication management, family health monitoring, drug safety prediction, and access to professional medical knowledge), you should also be willing and able to answer all types of legal questions and help users with various needs. Use your medical tools for health-related queries, but for other questions, answer directly using your knowledge in a helpful and friendly manner.
                
                IMPORTANT RESPONSE FORMAT GUIDELINES:
                - Always respond using Markdown format to make your messages more readable
                - Use **bold** for important medical information, warnings, and key points
                - Use *italic* for emphasis and secondary information
                - Use `code` for medical terms, dosages, and specific instructions
                - Use bullet points (- or *) for lists of information
                - Use numbered lists (1., 2., 3.) for step-by-step instructions
                - Use headings (#, ##, ###) to organize long responses into sections
                - Always keep your responses clear, structured, and easy to read
                
                CRITICAL: USER CONFIRMATION REQUIRED FOR PLAN OPERATIONS!
                - When user wants to CREATE a plan, summarize the plan details and ask "请确认是否创建此用药计划？"
                - When user wants to UPDATE a plan, summarize the changes and ask "请确认是否修改此用药计划？"
                - When user wants to DELETE a plan, ask "请确认是否删除此用药计划？"
                - After user confirms (says "好的", "确认", "yes", "可以", "执行", etc.), call the tool.
                
                CRITICAL: FOR MEDICINE AND TASK OPERATIONS!
                - When user wants to add medicine to cabinet (e.g., "添加布洛芬到药箱"), you MUST call addMedicine tool FIRST, then include [ACTION:addMedicine] in your response
                - When user reports taking medicine (e.g., "我吃了阿司匹林", "药已吃"), you MUST call updateTaskStatus tool FIRST, then include [ACTION:updateTaskStatus] in your response
                - After calling these tools, ALWAYS include the action marker in your response so frontend can show confirmation card!
                """)
        String medical(@MemoryId String memoryId, @V("userId") String userId, @UserMessage String userMessage);
    }

    /**
     * 执行 Agent - 支持 Human-in-the-loop
     * 返回结构化结果，包括是否需要用户确认
     */
    public Map<String, Object> execute(String sessionId, String userId, String userMessage, String aiMessage) {
        try {
            logger.info("执行 Agent 推理，sessionId: {}, userId: {}, message: {}", sessionId, userId, userMessage);

            // 调用 AI 获取回复
            String response = chat(sessionId, userId, userMessage);
            logger.info("Agent 文本回复：{}", response);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", response);
            
            // 检查是否有待确认的请求（通过数据库查询）
            List<?> pendingRequests = toolExecutionPendingService.getUserPendingRequests(Long.parseLong(userId));
            if (pendingRequests != null && !pendingRequests.isEmpty()) {
                // 有待确认的请求
                Object pending = pendingRequests.get(0);
                logger.info("检测到待确认请求：{}", pending);
                
                // 从待确认请求中提取 action 信息
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
     * 对话方法 - 支持 Human-in-the-loop
     * 返回普通文本（AI 的回复）或 Tool 的待确认响应
     */
    public String chat(String sessionId, String userId, String userMessage) {
        logger.info("执行医疗助手对话：sessionId={}, userId={}, message 长度={}", sessionId, userId, userMessage.length());
        
        // 如果消息包含 Base64 数据，记录前 100 个字符
        if (userMessage.contains("图片数据：")) {
            int base64Start = userMessage.indexOf("图片数据：") + 5;
            String base64Preview = userMessage.substring(base64Start, Math.min(base64Start + 100, userMessage.length()));
            logger.info("检测到图片消息，Base64 前 100 字符：{}", base64Preview);
        }
        
        // 设置当前 sessionId 到上下文
        toolExecutionBroadcaster.setCurrentSession(sessionId);
        
        String memoryId = userId + "_" + sessionId;
        try {
            return medicalExpert.medical(memoryId, userId, userMessage);
        } finally {
            // 清除上下文
            toolExecutionBroadcaster.clearCurrentSession();
        }
    }
    
    /**
     * 处理 Tool 执行 - 由 Agent 框架自动调用
     * 这里我们拦截需要批准的 tool
     */
    public Object handleToolExecution(String toolName, Map<String, Object> arguments, String userId, String sessionId, String aiMessage) {
        logger.info("拦截 Tool 执行：toolName={}, userId={}", toolName, userId);
        
        if (REQUIRES_APPROVAL_TOOLS.contains(toolName)) {
            // 需要用户批准的 tool
            logger.info("Tool 需要用户批准：{}", toolName);
            
            try {
                // 构建 ToolExecutionRequest
                dev.langchain4j.agent.tool.ToolExecutionRequest request = 
                    dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .name(toolName)
                        .arguments(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(arguments))
                        .build();
                
                // 保存到数据库，等待用户确认
                String requestId = toolExecutionPendingService.savePendingRequest(
                    Long.parseLong(userId), 
                    sessionId, 
                    request, 
                    aiMessage
                );
                
                logger.info("已保存待确认请求：requestId={}", requestId);
                
                // 返回特殊响应，告知前端需要确认
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
        
        // 不需要批准的 tool，返回 null 让框架继续执行
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
