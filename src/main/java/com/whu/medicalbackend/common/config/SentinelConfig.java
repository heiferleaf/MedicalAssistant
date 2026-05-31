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

    /** 每秒允许通过的 /api/agent/chat 请求数（生产建议 500，压测可临时调大）。 */
    @Value("${agent.sentinel.chat-qps:500}")
    private int chatQps;

    /** 每秒允许通过的 /api/agent/chat/stream（SSE）请求数。 */
    @Value("${agent.sentinel.stream-qps:300}")
    private int streamQps;

    /** 每秒允许通过的 /api/ocr/predict 请求数。 */
    @Value("${agent.sentinel.ocr-qps:30}")
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
        agentChatRule.setCount(chatQps);
        agentChatRule.setLimitApp("default");
        rules.add(agentChatRule);

        FlowRule agentChatStreamRule = new FlowRule();
        agentChatStreamRule.setResource("/api/agent/chat/stream");
        agentChatStreamRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        agentChatStreamRule.setCount(streamQps);
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
