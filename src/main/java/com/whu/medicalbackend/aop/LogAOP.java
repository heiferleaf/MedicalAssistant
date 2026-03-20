package com.whu.medicalbackend.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
public class LogAOP{

    @Pointcut("execution(* com.whu.medicalbackend.controller.UserController.refresh(..))")
    public void checkRefresh() {

    }

    @Before("checkRefresh()")
    public void adviceBefore(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // 获取参数
        Object[] args = joinPoint.getArgs();
        String params = "";
        if (args != null && args.length > 0) {
            params = Arrays.toString(args);
        }

        System.out.println("==== AOP 日志开始 ====");
        System.out.println("调用位置: " + className + "." + methodName);
        System.out.println("传入参数: " + params);
        System.out.println("==== AOP 日志结束 ====");
    }
}
