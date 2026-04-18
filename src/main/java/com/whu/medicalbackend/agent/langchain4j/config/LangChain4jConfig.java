package com.whu.medicalbackend.agent.langchain4j.config;

import com.whu.medicalbackend.agent.langchain4j.agents.MedicalAgent;
import com.whu.medicalbackend.agent.langchain4j.tools.predict.PredictTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanCreateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanDeleteTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanQueryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.plan.PlanUpdateTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryHistoryTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskQueryTodayTool;
import com.whu.medicalbackend.agent.langchain4j.tools.task.TaskUpdateStatusTool;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LangChain4jConfig implements WebMvcConfigurer {

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${dashscope.model:qwen-plus}")
    private String dashscopeModel;

    // 注入所有工具
    @Autowired
    private PlanCreateTool planCreateTool;
    
    @Autowired
    private PlanDeleteTool planDeleteTool;
    
    @Autowired
    private PlanQueryTool planQueryTool;
    
    @Autowired
    private PlanUpdateTool planUpdateTool;
    
    @Autowired
    private TaskQueryHistoryTool taskQueryHistoryTool;
    
    @Autowired
    private TaskQueryTodayTool taskQueryTodayTool;
    
    @Autowired
    private TaskUpdateStatusTool taskUpdateStatusTool;
    
    @Autowired
    private PredictTool predictTool;

    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModel)
                .enableSearch(true)  // 启用联网搜索功能
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // 删除这个方法，因为 MedicalAgent 已经自己处理了 AiServices 的构建
    // @Bean
    // public MedicalAgent medicalAgent() {
    //     ...
    // }
}
