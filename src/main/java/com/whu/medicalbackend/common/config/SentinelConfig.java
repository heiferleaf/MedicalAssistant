package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class SentinelConfig {

    @Value("${sentinel.dashboard.address:localhost:8858}")
    private String dashboardAddress;

    @Value("${sentinel.flow.agent-chat-qps:200}")
    private int agentChatQps;

    @Value("${sentinel.flow.agent-chat-stream-qps:200}")
    private int agentChatStreamQps;

    @Value("${sentinel.flow.ocr-qps:50}")
    private int ocrQps;

    @PostConstruct
    public void init() {
        System.setProperty("csp.sentinel.dashboard.server", dashboardAddress);
        System.setProperty("project.name", "medical-agent-service");

        initFlowRules();
    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule agentChatRule = new FlowRule();
        agentChatRule.setResource("/api/agent/chat");
        agentChatRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        agentChatRule.setCount(agentChatQps);
        agentChatRule.setLimitApp("default");
        rules.add(agentChatRule);

        FlowRule agentChatStreamRule = new FlowRule();
        agentChatStreamRule.setResource("/api/agent/chat/stream");
        agentChatStreamRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        agentChatStreamRule.setCount(agentChatStreamQps);
        agentChatStreamRule.setLimitApp("default");
        rules.add(agentChatStreamRule);

        FlowRule ocrRule = new FlowRule();
        ocrRule.setResource("/api/ocr/predict");
        ocrRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        ocrRule.setCount(ocrQps);
        ocrRule.setLimitApp("default");
        rules.add(ocrRule);

        FlowRuleManager.loadRules(rules);
    }
}
