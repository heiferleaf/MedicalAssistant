package com.whu.medicalbackend.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Function Call 工具定义
 */
public class ToolDefinition {

    /**
     * 创建 RAG 查询工具定义
     */
    public static ObjectNode createRagQueryTool(ObjectMapper objectMapper) {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "rag_query");
        funcDef.put("description", "查询医学知识库，获取药物、症状、疾病等医学信息");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode questionProp = objectMapper.createObjectNode();
        questionProp.put("type", "string");
        questionProp.put("description", "用户的问题，例如'高血压的症状有哪些'");
        properties.set("question", questionProp);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("question");
        
        parameters.set("properties", properties);
        parameters.set("required", required);
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    /**
     * 创建用药计划工具定义
     */
    public static ObjectNode createPlanCreateTool(ObjectMapper objectMapper) {
        ObjectNode function = objectMapper.createObjectNode();
        function.put("type", "function");
        
        ObjectNode funcDef = objectMapper.createObjectNode();
        funcDef.put("name", "spring_plan_create");
        funcDef.put("description", "创建用药提醒计划，帮助用户设置定时用药提醒");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode medicineName = objectMapper.createObjectNode();
        medicineName.put("type", "string");
        medicineName.put("description", "药品名称，如'阿司匹林'");
        properties.set("medicineName", medicineName);
        
        ObjectNode dosage = objectMapper.createObjectNode();
        dosage.put("type", "string");
        dosage.put("description", "用药剂量，如'100mg'或'按医嘱'");
        properties.set("dosage", dosage);
        
        ObjectNode startDate = objectMapper.createObjectNode();
        startDate.put("type", "string");
        startDate.put("description", "开始日期，格式：YYYY-MM-DD");
        properties.set("startDate", startDate);
        
        ObjectNode endDate = objectMapper.createObjectNode();
        endDate.put("type", "string");
        endDate.put("description", "结束日期（可选），格式：YYYY-MM-DD");
        properties.set("endDate", endDate);
        
        ObjectNode timePoints = objectMapper.createObjectNode();
        timePoints.put("type", "array");
        timePoints.put("description", "每日提醒时间列表，如['08:00', '20:00']");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "string");
        timePoints.set("items", items);
        properties.set("timePoints", timePoints);
        
        ObjectNode remark = objectMapper.createObjectNode();
        remark.put("type", "string");
        remark.put("description", "备注信息（可选）");
        properties.set("remark", remark);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("medicineName");
        required.add("dosage");
        required.add("startDate");
        required.add("timePoints");
        
        parameters.set("properties", properties);
        parameters.set("required", required);
        
        funcDef.set("parameters", parameters);
        function.set("function", funcDef);
        
        return function;
    }

    /**
     * 获取所有工具定义
     */
    public static ArrayNode getAllTools(ObjectMapper objectMapper) {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(createRagQueryTool(objectMapper));
        tools.add(createPlanCreateTool(objectMapper));
        return tools;
    }
}
