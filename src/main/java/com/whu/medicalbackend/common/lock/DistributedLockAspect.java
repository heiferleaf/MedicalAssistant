package com.whu.medicalbackend.common.lock;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 分布式锁 AOP 切面，拦截 @DistributedLock 注解的方法，
 * 自动完成加锁 → 执行业务 → 释放锁的流程。
 */
@Aspect
@Component
@Order(0)
public class DistributedLockAspect {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    @Autowired
    private RedisService redisService;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String lockKey = LockKeyResolver.resolve(
                distributedLock.prefix(),
                distributedLock.key(),
                signature.getMethod(),
                joinPoint.getArgs()
        );

        log.debug("尝试获取分布式锁: {}", lockKey);

        if (!redisService.tryLock(lockKey, distributedLock.waitTime(), distributedLock.leaseTime())) {
            throw new BusinessException(distributedLock.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            redisService.unlock(lockKey);
            log.debug("释放分布式锁: {}", lockKey);
        }
    }
}
