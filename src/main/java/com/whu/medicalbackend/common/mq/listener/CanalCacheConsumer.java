package com.whu.medicalbackend.common.mq.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.medicalbackend.agent.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.enumField.EventLogEnum;
import com.whu.medicalbackend.common.mq.entity.CanalMessage;
import com.whu.medicalbackend.common.util.RedisKeyBuilderUtil;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.family.service.FamilyCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 监听 Canal Binlog 消息，精准处理 Redis 缓存更新与删除
 */
@Slf4j
@Component
public class CanalCacheConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FamilyCacheService familyCacheService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private FamilyMemberMapper memberMapper; // 需要用到它来反查 user_id 对应的 group_id

    /**
     * 监听 Topic/Queue: redis_data_change
     * 接收 String 避免泛型擦除问题，然后手动精准反序列化
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "canal_cache_queue", durable = "true"),
            exchange = @Exchange(value = "canal.exchange", type = "direct"),
            key = "redis_data_change"
    ))
    public void handleCanalMessage(String messagePayload) {
        try {
            // 1. 精准反序列化为 CanalMessage<Map<String, String>>
            JavaType type = objectMapper.getTypeFactory().constructParametricType(CanalMessage.class, Map.class);
            CanalMessage<Map<String, String>> canalMsg = objectMapper.readValue(messagePayload, type);

            // 2. 数据库名过滤 (只处理 medicalassistant)
            if (!"medicalassistant".equals(canalMsg.getDatabase())) {
                return; // 非目标数据库，直接丢弃
            }

            log.info("🛠️ 准备处理缓存 -> 表: {}, 操作: {}", canalMsg.getTable(), canalMsg.getType());

            String table = canalMsg.getTable();
            String opType = canalMsg.getType(); // INSERT, UPDATE, DELETE
            List<Map<String, String>> dataList = canalMsg.getData();

            if (dataList == null || dataList.isEmpty()) {
                return;
            }

            // 3. 根据表名路由到不同的缓存处理逻辑
            switch (table) {
                case "family_member" -> handleFamilyMemberChange(opType, dataList);
                case "family_event_log" -> handleEventLogChange(opType, dataList);
                case "medication_task" ->
                    // 健康数据和服药任务的变化，都会直接影响当天的健康快照
                    handleTaskChange(opType, dataList);
                case "health_data" -> handleHealthDataChange(opType, dataList);
                case "user" -> handleUserChange(opType, dataList);
                case "medicine" -> handleUserMedicineChange(opType, dataList);
                default -> {
                    // 其他无关表，直接忽略
                }
            }

        } catch (Exception e) {
            // 捕获所有异常，防止 MQ 消息无限重试阻塞队列 (根据实际业务配置决定是否抛出)
            log.error("处理 Canal 缓存更新消息失败! payload: {}", messagePayload, e);
        }
    }

    // ======================== 具体表的处理逻辑 ========================

    /**
     * 1. 处理 family_member (家庭成员) 表变更
     */
    private void handleFamilyMemberChange(String opType, List<Map<String, String>> dataList) {
        String today = LocalDate.now().toString();

        for (Map<String, String> data : dataList) {
            try {
                Long groupId = parseLong(data.get("group_id"));
                Long userId = parseLong(data.get("user_id"));
                String status = data.get("status");

                if (groupId == null || userId == null) continue;

                if ("INSERT".equals(opType) || ("UPDATE".equals(opType) && "active".equals(status))) {
                    // 加入或重新激活：同步该成员信息到 Hash
                    familyCacheService.syncSingleMemberToCache(groupId, userId);
                } else if ("DELETE".equals(opType) || ("UPDATE".equals(opType) && "quit".equals(status))) {
                    // 退出或被移出：从 Hash 中移除该成员
                    familyCacheService.removeMemberFromCache(groupId, userId);
                }

                // 只要成员发生变动（进出），今天的快照和今日警报就必须失效重建
                redisService.delete(RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, today));
                redisService.delete(RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, today));

            } catch (JsonProcessingException e) {
                log.error("FamilyMember JSON 序列化写入缓存失败", e);
            }
        }
    }

    /**
     * 2. 处理 family_event_log (家庭事件日志/警报) 表变更
     */
    private void handleEventLogChange(String opType, List<Map<String, String>> dataList) {
        if (!"INSERT".equals(opType)) return; // 日志通常只有 INSERT

        String today = LocalDate.now().toString();
        for (Map<String, String> data : dataList) {
            Long groupId = parseLong(data.get("group_id"));
            String eventType = data.get("event_type");
            if (groupId != null && eventType.equals(EventLogEnum.ALARM_MISSED.name())) {
                // 有新警报产生，使今天的警报列表缓存失效
                redisService.delete(RedisKeyBuilderUtil.getFamilyAlarmKey(groupId, today));
            }
        }
    }

    /**
     * 3. 处理 health_data (健康数据) 和 medication_task (服药任务) 变更
     */
    private void handleTaskChange(String opType, List<Map<String, String>> dataList) {
        String today = LocalDate.now().toString();

        for (Map<String, String> data : dataList) {
            Long userId = parseLong(data.get("user_id"));
            if (userId != null) {
                // 注意：这两张表通常只有 user_id，没有 group_id，需要反查
                Long groupId = memberMapper.getGroupIdByUserId(userId);
                if (groupId != null) {
                    // 只要有健康数据或任务状态变更，删除所在家庭组的今日快照，让其重查 DB
                    redisService.delete(RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, today));
                }
            }
        }
    }

    /**
     * 4. 处理 user (用户) 表变更 (主要是昵称/头像修改)
     */
    private void handleUserChange(String opType, List<Map<String, String>> dataList) {
        if (!"UPDATE".equals(opType)) return;

        String today = LocalDate.now().toString();
        for (Map<String, String> data : dataList) {
            Long userId = parseLong(data.get("id")); // 假设 user 表主键叫 id
            if (userId != null) {
                Long groupId = memberMapper.getGroupIdByUserId(userId);
                if (groupId != null) {
                    try {
                        // 1. 用户昵称改了，重新同步他在家庭组里的 Hash 详情
                        familyCacheService.syncSingleMemberToCache(groupId, userId);
                        // 2. 昵称改了，家庭快照里的昵称也要变，所以删除今日快照
                        redisService.delete(RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, today));
                    } catch (JsonProcessingException e) {
                        log.error("User更新时同步Member缓存失败", e);
                    }
                }
            }
        }
    }

    /**
     * 5. 处理 user_medicine (用户药品) 变更
     */
    private void handleUserMedicineChange(String opType, List<Map<String, String>> dataList) {
        for (Map<String, String> data : dataList) {
            Long userId = parseLong(data.get("user_id"));
            if (userId != null) {
                // 增删改药品，直接删除该用户的药品列表缓存
                redisService.delete(RedisKeyBuilderUtil.getUserMedicineKey(userId));
            }
        }
    }

    private void handleHealthDataChange(String opType, List<Map<String, String>> dataList) {
        String today = LocalDate.now().toString();

        for (Map<String, String> data : dataList) {
            Long userId = parseLong(data.get("user_id"));
            if (userId != null) {
                Long groupId = memberMapper.getGroupIdByUserId(userId);
                if (groupId != null) {
                    // 只要有健康数据变更，删除所在家庭组的今日快照，让其重查 DB
                    redisService.delete(RedisKeyBuilderUtil.getFamilySnapshotKey(groupId, today));
                }
                String cacheKey = RedisKeyBuilderUtil.getHealthDataCachePrefix(userId);
                redisService.delete(cacheKey);
            }
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 安全转换 String 为 Long，防止 Canal 推送空数据报错
     */
    private Long parseLong(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}