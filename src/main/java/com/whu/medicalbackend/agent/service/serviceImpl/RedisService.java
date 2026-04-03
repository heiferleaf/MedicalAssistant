package com.whu.medicalbackend.agent.service.serviceImpl;


import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisService{

    public static final String nullValue = "null";    // 用于缓存空值，避免缓存穿透

    // 使用 String 作为 Redis 缓存的数据结构
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 使用 Redission 分布式锁，实现可重入逻辑、读写锁、看门狗机制等
    @Autowired
    private RedissonClient redisson;

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void putWithHash(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void setWithExpire(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Object getWithHash(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public List<Object> valuesWithHash(String key) {
        return redisTemplate.opsForHash().values(key);
    }

    public Set<Object> keysWithHash(String key) {
        return redisTemplate.opsForHash().keys(key);    // 获取所有Field,也就是userId
    }

    public Boolean hasMember(String memberKey) {
        return redisTemplate.opsForHash().hasKey(RedisKeyBuilderUtil.ONLINE_MEMBER_CACHE_PREFIX, memberKey);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteWithHash(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 使用 Redisson 尝试加锁
     * @param lockKey 锁的唯一标识
     * @param waitTime 最多等待多久（秒）
     * @param leaseTime 锁自动释放的时间（秒）
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        // 1. 获取一个锁对象（此时还没加锁）
        RLock lock = redisson.getLock(lockKey);
        try {
            // 2. 尝试加锁
            // 参数1：等待时间。如果锁被占了，我愿意在这等多久？
            // 参数2：租约时间。拿到锁后，如果我不手动释放，多久后强制过期？
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        RLock lock = redisson.getLock(lockKey);
        // 只有锁还被持有，且是我自己持有的时候才释放
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
