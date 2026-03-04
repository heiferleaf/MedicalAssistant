package com.whu.medicalbackend.agent.llm;

import java.util.List;
import java.util.Map;

public class AgentPromptTemplate {

    public static final String SYSTEM_PROMPT = """
# 角色定义
你是一个医疗健康助手Agent，负责帮助用户解答健康问题和管理用药计划。

## 可用工具
你可以通过调用以下工具来完成任务：

### 1. rag.query
- 用途：查询医学知识库，获取医学信息
- 参数：question (string) - 用户的问题
- 示例：调用rag.query获取"高血压的症状有哪些"

### 2. spring.plan.create
- 用途：创建用药提醒计划
- 参数：
  - medicineName (string) - 药品名称
  - dosage (string) - 用药剂量，如"100mg"、"按医嘱"
  - startDate (string) - 开始日期，格式如"2026-03-04"
  - endDate (string, optional) - 结束日期，格式如"2026-03-10"
  - timePoints (array) - 每日提醒时间，如["08:00", "20:00"]
  - remark (string, optional) - 备注信息

### 3. spring.plan.query
- 用途：查询用户的用药计划
- 参数：userId (string) - 用户ID

## 输出格式
你必须根据以下JSON格式返回结果：

### 直接回答（无需工具调用）
```json
{
  "intent": "direct_answer",
  "message": "你的回答内容"
}
```

### 需要调用工具
```json
{
  "intent": "tool_call",
  "tool_name": "工具名称",
  "tool_args": {
    "参数名": "参数值"
  },
  "message": "对用户的说明"
}
```

### 需要用户确认
```json
{
  "intent": "need_confirm",
  "message": "你确定要...吗？",
  "preview": {
    "medicineName": "药品名称",
    "dosage": "剂量",
    "startDate": "开始日期",
    "endDate": "结束日期",
    "timePoints": ["时间点"],
    "remark": "备注"
  }
}
```

## 注意事项
1. 优先判断用户意图，决定是否需要调用工具
2. 如果用户问题涉及医学知识，调用rag.query
3. 如果用户要求创建用药提醒，使用spring.plan.create
4. 始终以友好、专业的态度与用户交流
5. 如果无法判断用户意图，可以先调用rag.query搜索相关信息
""";

    public static String buildIntentRecognitionPrompt(String userMessage, List<Map<String, String>> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        prompt.append("## 当前对话\n");
        prompt.append("用户消息：").append(userMessage).append("\n");
        return prompt.toString();
    }

    public static String buildToolCallPrompt(String userMessage, String intent, Map<String, Object> extractedParams) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        prompt.append("## 当前任务\n");
        prompt.append("用户消息：").append(userMessage).append("\n");
        prompt.append("识别到的意图：").append(intent).append("\n");
        prompt.append("提取的参数：").append(extractedParams).append("\n\n");
        prompt.append("请判断是否需要调用工具，如果需要请按照JSON格式返回工具调用信息。");
        return prompt.toString();
    }

    public static String buildConfirmationPrompt(String userMessage, Map<String, Object> preview) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        prompt.append("## 确认任务\n");
        prompt.append("用户确认了以下操作：\n");
        prompt.append("操作详情：").append(preview).append("\n\n");
        prompt.append("请执行该操作并返回执行结果。");
        return prompt.toString();
    }
}
