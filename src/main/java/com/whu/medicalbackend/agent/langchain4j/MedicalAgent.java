package com.whu.medicalbackend.agent.langchain4j;

import com.whu.medicalbackend.agent.langchain4j.tools.PlanCreateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanDeleteTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.PlanUpdateTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
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

        @UserMessage("""
                你是一个医疗健康助手，负责帮助用户解答健康问题和管理用药计划。
                请根据用户的需求提供清晰的回答。
                """)
        String medical(@MemoryId String memoryId, @V("userMessage") String userMessage);
    }

    public String chat(String sessionId, String userMessage) {
        logger.info("执行医疗助手对话: sessionId={}, message={}", sessionId, userMessage);

        MedicalExpert medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(planQueryTool, planCreateTool, planUpdateTool, planDeleteTool)
                .build();

        return medicalExpert.medical(sessionId, userMessage);
    }

    public Map<String, Object> execute(String sessionId, String userId, String userMessage) {
        try {
            logger.info("执行 Agent 推理，sessionId: {}, userId: {}, message: {}", sessionId, userId, userMessage);
            
            String response = chat(sessionId, userMessage);
            
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
