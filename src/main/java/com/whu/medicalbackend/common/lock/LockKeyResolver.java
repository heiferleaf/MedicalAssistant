package com.whu.medicalbackend.common.lock;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 锁 Key 解析器，将注解中的 SpEL 表达式 + 参数解析为实际的 Redis 锁 Key。
 */
public class LockKeyResolver {

    private static final SpelExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    public static String resolve(String prefix, String keyExpression, Method method, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = discoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = parser.parseExpression(keyExpression);
        Object value = exp.getValue(context);
        return prefix + (value != null ? value.toString() : "");
    }
}
