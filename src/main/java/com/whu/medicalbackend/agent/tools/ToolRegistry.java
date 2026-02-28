package com.whu.medicalbackend.agent.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ToolRegistry {
    private final Map<String, ToolInfo> tools = new HashMap<>();
    
    public record ToolInfo(String description, Function<Map<String, Object>, Map<String, Object>> function) {}
    
    public void register(String name, String description, Function<Map<String, Object>, Map<String, Object>> function) {
        tools.put(name, new ToolInfo(description, function));
    }
    
    public ToolInfo get(String name) {
        return tools.get(name);
    }
    
    public boolean has(String name) {
        return tools.containsKey(name);
    }
}