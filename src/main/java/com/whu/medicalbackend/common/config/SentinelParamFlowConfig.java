package com.whu.medicalbackend.common.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class SentinelParamFlowConfig {

    @PostConstruct
    public void init() {
        List<ParamFlowRule> rules = new ArrayList<>();

        ParamFlowRule userChatStreamParamRule = new ParamFlowRule();
        userChatStreamParamRule.setResource("/api/agent/chat/stream");
        userChatStreamParamRule.setParamIdx(0);
        userChatStreamParamRule.setCount(5);
        userChatStreamParamRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userChatStreamParamRule.setLimitApp("default");
        rules.add(userChatStreamParamRule);

        ParamFlowRuleManager.loadRules(rules);
    }
}
