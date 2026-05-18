package com.whu.medicalbackend.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解，基于 Redisson RLock 实现。
 * 用在需要并发控制的方法上，AOP 切面自动加锁/释放。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** 锁 Key 前缀，例如 "lock:task:update:" */
    String prefix() default "";

    /** SpEL 表达式，解析出锁 Key 的动态部分，例如 "#taskId" */
    String key();

    /** 获取锁的最大等待时间（秒），0 表示获取不到立即失败 */
    long waitTime() default 5;

    /** 锁自动释放时间（秒） */
    long leaseTime() default 10;

    /** 获取锁失败时抛出的提示信息 */
    String message() default "操作处理中，请稍后重试";
}
