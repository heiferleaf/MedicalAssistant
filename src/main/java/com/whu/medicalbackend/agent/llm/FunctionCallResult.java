package com.whu.medicalbackend.agent.llm;

import java.util.Map;

/**
 * Function Call 调用结果
 */
public class FunctionCallResult {

    private final String name;
    private final Map<String, Object> arguments;
    private final String content;

    public FunctionCallResult(String name, Map<String, Object> arguments, String content) {
        this.name = name;
        this.arguments = arguments;
        this.content = content;
    }

    public static FunctionCallResult of(String name, Map<String, Object> arguments) {
        return new FunctionCallResult(name, arguments, null);
    }

    public static FunctionCallResult withContent(String content) {
        return new FunctionCallResult(null, null, content);
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public String getContent() {
        return content;
    }

    public boolean isFunctionCall() {
        return name != null && !name.isBlank();
    }

    @Override
    public String toString() {
        if (isFunctionCall()) {
            return "FunctionCall{name='" + name + "', arguments=" + arguments + "}";
        }
        return "Content{" + content + "}";
    }
}
