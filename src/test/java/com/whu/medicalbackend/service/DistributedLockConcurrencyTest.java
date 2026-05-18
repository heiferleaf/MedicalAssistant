package com.whu.medicalbackend.service;

import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分布式锁并发测试。
 * 使用 CountDownLatch 确保两个线程在完全相同的时刻竞争同一把锁。
 */
@SpringBootTest
public class DistributedLockConcurrencyTest {

    @Autowired
    private RedisService redisService;

    @Test
    public void testConcurrentLockOnlyOneSucceeds() throws InterruptedException {
        final String testLockKey = "lock:test:concurrency:1";
        final CountDownLatch startLatch = new CountDownLatch(1);  // 同时起跑
        final CountDownLatch finishLatch = new CountDownLatch(2); // 等两个线程都结束
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                startLatch.await(); // 等待发令枪
                if (redisService.tryLock(testLockKey, 0, 5)) {
                    try {
                        successCount.incrementAndGet();
                        Thread.sleep(100); // 模拟业务处理，确保锁持有足够长
                    } finally {
                        redisService.unlock(testLockKey);
                    }
                } else {
                    failCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();

        startLatch.countDown();  // 发令！两个线程同时起跑
        finishLatch.await();     // 等两个线程都结束

        assertEquals(1, successCount.get(), "应该只有1个线程成功获取锁");
        assertEquals(1, failCount.get(), "应该有1个线程获取锁失败");
    }
}
