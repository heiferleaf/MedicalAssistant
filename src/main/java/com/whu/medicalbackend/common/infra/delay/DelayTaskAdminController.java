package com.whu.medicalbackend.common.infra.delay;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/delay-tasks")
public class DelayTaskAdminController {

    private final StringRedisTemplate redisTemplate;

    public DelayTaskAdminController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();

        Long zsetSize = redisTemplate.opsForZSet().size(RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY);
        result.put("pendingTasks", zsetSize != null ? zsetSize : 0);

        Long now = System.currentTimeMillis();
        Long overdueCount = redisTemplate.opsForZSet().count(
                RedisDelayTaskPublisher.DELAY_TASK_ZSET_KEY, 0, now);
        result.put("overdueTasks", overdueCount != null ? overdueCount : 0);

        Set<String> canceledSet = redisTemplate.opsForSet().members("infra:delay:canceled");
        result.put("canceledCount", canceledSet != null ? canceledSet.size() : 0);

        return result;
    }
}
