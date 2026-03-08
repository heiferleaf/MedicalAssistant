package com.whu.medicalbackend.agent.langchain4j.agents;

import com.whu.medicalbackend.agent.langchain4j.tools.PlanCreateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanDeleteTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanUpdateTool;
import dev.langchain4j.memory.ChatMemory;
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

    private final ChatModel chatModel;

    private final PlanQueryTool planQueryTool;

    private final PlanCreateTool planCreateTool;

    private final PlanUpdateTool planUpdateTool;

    private final PlanDeleteTool planDeleteTool;

    @Autowired
    public MedicalAgent(ChatModel chatModel,
                        PlanQueryTool planQueryTool,
                        PlanCreateTool planCreateTool,
                        PlanUpdateTool planUpdateTool,
                        PlanDeleteTool planDeleteTool) {
        this.chatModel = chatModel;
        this.planQueryTool = planQueryTool;
        this.planCreateTool = planCreateTool;
        this.planUpdateTool = planUpdateTool;
        this.planDeleteTool = planDeleteTool;
    }

    public interface MedicalExpert {

        @SystemMessage("""
                You are a medical health assistant responsible for helping users with health questions and managing medication plans.
                
                IMPORTANT INFORMATION:
                - The current user's ID is: {{userId}}
                - ALWAYS use this exact userId when calling any tools
                
                IMPORTANT: You have access to the following tools to help users:
                - queryPlans: Query the user's medication plans
                - createPlan: Create a new medication plan
                - updatePlan: Update an existing medication plan
                - deletePlan: Delete a medication plan
                
                When should you use tools:
                - ALWAYS use queryPlans when the user asks about their medication plans
                - ALWAYS use createPlan when the user wants to create a new medication plan
                - ALWAYS use updatePlan when the user wants to modify an existing medication plan
                - ALWAYS use deletePlan when the user wants to delete a medication plan
                
                Guidelines for tool usage:
                1. Try to use tools FIRST before answering directly
                2. If you are unsure whether a tool is needed, ask the user for clarification
                3. After using a tool, summarize the result clearly to the user
                4. Always provide helpful and friendly responses
                5. ALWAYS use the provided userId {{userId}} when calling tools, DO NOT make up a userId
                
                Remember: You are a helpful medical assistant. Use the tools at your disposal to provide the best possible service to users.
                """)
        String medical(@MemoryId String memoryId, @V("userId") String userId, @UserMessage String userMessage);
    }

    public String chat(String sessionId, String userId, String userMessage) {
        logger.info("执行医疗助手对话: sessionId={}, userId={}, message={}", sessionId, userId, userMessage);

        MedicalExpert medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(planQueryTool, planCreateTool, planUpdateTool, planDeleteTool)
                .build();

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
