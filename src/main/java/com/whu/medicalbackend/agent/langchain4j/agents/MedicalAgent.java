package com.whu.medicalbackend.agent.langchain4j.agents;

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
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryTodayTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskUpdateStatusTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryHistoryTool;
<<<<<<< HEAD
=======
import com.whu.medicalbackend.agent.langchain4j.tools.predict.PredictTool;
import com.whu.medicalbackend.agent.langchain4j.tools.rag.RagTool;
>>>>>>> 3533432c78b3d0a888a4729a91e5c8e407aada62
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MedicalAgent {

    private static final Logger logger = LoggerFactory.getLogger(MedicalAgent.class);

    private final MedicalExpert medicalExpert;

    @Autowired
    public MedicalAgent(ChatModel chatModel,
<<<<<<< HEAD
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
            MedicineAddTool medicineAddTool) {
=======
                        PlanQueryTool planQueryTool,
                        PlanCreateTool planCreateTool,
                        PlanUpdateTool planUpdateTool,
                        PlanDeleteTool planDeleteTool,
                        TaskQueryTodayTool taskQueryTodayTool,
                        TaskUpdateStatusTool taskUpdateStatusTool,
                        TaskQueryHistoryTool taskQueryHistoryTool,
                        PredictTool predictTool,
                        RagTool ragTool) {
>>>>>>> 3533432c78b3d0a888a4729a91e5c8e407aada62

        this.medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(planQueryTool, planCreateTool, planUpdateTool, planDeleteTool,
<<<<<<< HEAD
                        taskQueryTodayTool, taskUpdateStatusTool, taskQueryHistoryTool,
                        familyQueryTool, familyHealthSnapshotTool, familyAlarmTool, familyInviteTool,
                        medicineQueryTool, medicineAddTool)
=======
                       taskQueryTodayTool, taskUpdateStatusTool, taskQueryHistoryTool,
                       predictTool, ragTool)
>>>>>>> 3533432c78b3d0a888a4729a91e5c8e407aada62
                .build();
    }

    public interface MedicalExpert {

        @SystemMessage("""
                You are a medical health assistant responsible for helping users with health questions and managing medication plans and tasks.

                IMPORTANT INFORMATION:
                - The current user's ID is: {{userId}}
                - ALWAYS use this exact userId when calling any tools

                IMPORTANT: You have access to the following tools to help users:

                MEDICATION PLAN TOOLS:
                - queryPlans: Query the user's medication plans
                - createPlan: Create a new medication plan
                - updatePlan: Update an existing medication plan
                - deletePlan: Delete a medication plan

                MEDICATION TASK TOOLS:
                - getTodayTasks: Get today's medication tasks
                - updateTaskStatus: Update the status of a medication task (0=not taken, 1=taken, 2=missed)
                - getHistoryTasks: Query historical medication tasks
<<<<<<< HEAD

                FAMILY GROUP TOOLS:
                - queryMyFamilyGroup: Query the user's family group and member information
                - getFamilyHealthSnapshot: Query family members' health and medication data (today's snapshot)
                - getFamilyAlarms: Query today's family health alarms
                - inviteFamilyMember: Invite a member to join the family group by phone number

                MEDICINE TOOLS:
                - queryMyMedicines: Query all medicines saved by the user
                - addMedicine: Add a new medicine to the user's medicine library

=======
                
                DRUG ADVERSE REACTION PREDICTION TOOLS:
                - predictAdverseReactions: Predict potential drug adverse reactions based on clinical information
                - analyzeAdverseReactionRisk: Analyze drug adverse reaction risks based on symptoms and medications
                
                MEDICAL KNOWLEDGE BASE TOOLS:
                - queryMedicalKnowledge: Query medical knowledge base using RAG for professional medical information
                
>>>>>>> 3533432c78b3d0a888a4729a91e5c8e407aada62
                When should you use tools:
                - ALWAYS use queryPlans when the user asks about their medication plans
                - ALWAYS use createPlan when the user wants to create a new medication plan
                - ALWAYS use updatePlan when the user wants to modify an existing medication plan
                - ALWAYS use deletePlan when the user wants to delete a medication plan
                - ALWAYS use getTodayTasks when the user asks about today's tasks or daily schedule
                - ALWAYS use updateTaskStatus when the user wants to mark a task as taken or missed
                - ALWAYS use getHistoryTasks when the user asks about past medication history
<<<<<<< HEAD
                - ALWAYS use queryMyFamilyGroup when the user asks about their family group or family members
                - ALWAYS use getFamilyHealthSnapshot when the user asks about family health or medication status
                - ALWAYS use getFamilyAlarms when the user asks about family alarms or reminders
                - ALWAYS use inviteFamilyMember when the user wants to invite someone to the family group
                - ALWAYS use queryMyMedicines when the user asks about their medicine library
                - ALWAYS use addMedicine when the user wants to add a medicine to their library

                Guidelines for tool usage:
                1. Try to use tools FIRST before answering directly
                2. If you are unsure whether a tool is needed, ask the user for clarification
                3. After using a tool, summarize the result clearly to the user
                4. Always provide helpful and friendly responses
                5. ALWAYS use the provided userId {{userId}} when calling tools, DO NOT make up a userId
                6. ALWAYS give a clear summary to the user after completing any tool operations, DO NOT return an empty response

                Remember: You are a helpful medical assistant. Use the tools at your disposal to provide the best possible service to users.
=======
                - ALWAYS use predictAdverseReactions when the user asks about drug safety, side effects, or adverse reactions
                - ALWAYS use analyzeAdverseReactionRisk when the user wants to assess medication risks
                - ALWAYS use queryMedicalKnowledge when the user asks general medical questions, symptoms, diseases, treatments, or needs professional medical information
                
                Guidelines for tool usage:
                1. For medical questions, ALWAYS try queryMedicalKnowledge FIRST to get professional, evidence-based information
                2. Try to use tools FIRST before answering directly
                3. If you are unsure whether a tool is needed, ask the user for clarification
                4. After using a tool, summarize the result clearly to the user
                5. Always provide helpful and friendly responses
                6. ALWAYS use the provided userId {{userId}} when calling tools, DO NOT make up a userId
                7. For prediction tools, extract relevant clinical information, patient profile, and medication details from the conversation
                8. When users ask about drug safety or side effects, always use the prediction tools to provide evidence-based assessments
                9. For general medical knowledge questions, use queryMedicalKnowledge to provide accurate, professional information
                
                Remember: You are a helpful medical assistant with comprehensive capabilities including medication management, drug safety prediction, and access to professional medical knowledge. Use all available tools to provide the best possible medical assistance.
>>>>>>> 3533432c78b3d0a888a4729a91e5c8e407aada62
                """)
        String medical(@MemoryId String memoryId, @V("userId") String userId, @UserMessage String userMessage);
    }

    public String chat(String sessionId, String userId, String userMessage) {
        logger.info("执行医疗助手对话: sessionId={}, userId={}, message={}", sessionId, userId, userMessage);
        return medicalExpert.medical(sessionId, userId, userMessage);
    }

    public Map<String, Object> execute(String sessionId, String userId, String userMessage) {
        try {
            logger.info("执行 Agent 推理，sessionId: {}, userId: {}, message: {}", sessionId, userId, userMessage);

            String response = chat(sessionId, userId, userMessage);

            logger.info("Agent 执行结果: {}", response);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("assistant_message", response);
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
}
