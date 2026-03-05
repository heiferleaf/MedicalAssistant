package com.whu.medicalbackend.util;

/**
 * Redis Key 构造工具类
 * 逻辑：使用常量前缀进行直观拼接，防止业务代码中 Key 格式不一致
 */
public class RedisKeyBuilderUtil {

    // ================= 1. 常量定义 (保留原样) =================

    public static final String LOCK_FAMILY_CREATE_PREFIX   = "lock:family:create:";
    public static final String LOCK_FAMILY_APPLY_PREFIX    = "lock:family:apply:";
    public static final String LOCK_FAMILY_INVITE_PREFIX   = "lock:family:invite:";
    public static final String LOCK_FAMILY_APPROVE_PREFIX  = "lock:family:approve:";
    public static final String LOCK_FAMILY_SNAPSHOT_PREFIX = "lock:family:snapshot:";
    public static final String LOCK_FAMILY_ALARM_PREFIX    = "lock:family:alarm:";
    public static final String LOCK_FAMILY_LEAVE_PREFIX    = "lock:family:leave:";

    public static final String FAMILY_APPLY_LIMIT_PREFIX   = "family:apply:limit:";
    public static final String FAMILY_INVITE_LIMIT_PREFIX  = "family:invite:limit:";
    public static final String FAMILY_SNAPSHOT_PREFIX      = "family:snapshot:";
    public static final String FAMILY_ALARM_PREFIX         = "family:alarms:";
    public static final String MEMBER_CACHE_KEY_PREFIX     = "family:members:";
    public static final String ONLINE_MEMBER_CACHE_KEY     = "family:online:members";

    public static final String AUTH_REFRESHTOKEN_PREFIX    = "auth:rt:";

    // ================= 2. Key 构造方法 =================

    /** 完整 Key: lock:family:create:{userId} */
    public static String getFamilyCreateLockKey(Long userId) {
        return LOCK_FAMILY_CREATE_PREFIX + userId;
    }

    /** 完整 Key: lock:family:apply:{userId}:{groupId} */
    public static String getFamilyApplyLockKey(Long userId, Long groupId) {
        // 这里根据原常量末尾有冒号，中间变量需补齐冒号
        return LOCK_FAMILY_APPLY_PREFIX + userId + ":" + groupId;
    }

    /** 完整 Key: lock:family:invite:{phone}:{groupId} */
    public static String getFamilyInviteLockKey(String phone, Long groupId) {
        // 这里根据原常量末尾有冒号，中间变量需补齐冒号
        return LOCK_FAMILY_INVITE_PREFIX + phone + ":" + groupId;
    }

    /** 完整 Key: lock:family:approve:{applyId} */
    public static String getFamilyApproveLockKey(Long applyId) {
        // 原前缀末尾无冒号，此处手动补齐
        return LOCK_FAMILY_APPROVE_PREFIX + applyId;
    }

    /** 完整 Key: lock:family:snapshot:{groupId} */
    public static String getFamilySnapshotLockKey(Long groupId) {
        return LOCK_FAMILY_SNAPSHOT_PREFIX + groupId;
    }

    /** 完整 Key: lock:family:alarm:{groupId} */
    public static String getFamilyAlarmsLockKey(Long groupId) {
        return LOCK_FAMILY_ALARM_PREFIX + groupId;
    }

    /** 完整 Key: lock:family:leave:{userId} */
    public static String getFamilyLeaveLockKey(Long userId) {
        return LOCK_FAMILY_LEAVE_PREFIX + userId;
    }

    /** 完整 Key: family:apply:limit:{userId}:{groupId} */
    public static String getFamilyApplyLimitKey(Long userId, Long groupId) {
        return FAMILY_APPLY_LIMIT_PREFIX + userId + ":" + groupId;
    }

    /** 完整 Key: family:invite:limit:{phone}:{groupId} */
    public static String getFamilyInviteLimitKey(String phone, Long groupId) {
        return FAMILY_INVITE_LIMIT_PREFIX + phone + ":" + groupId;
    }

    /** 完整 Key: family:members:{groupId} */
    public static String getMemberHashKey(Long groupId) {
        return MEMBER_CACHE_KEY_PREFIX + groupId;
    }

    /** 完整 Key: family:snapshot:{groupId}:{date} */
    public static String getFamilySnapshotKey(Long groupId, String date) {
        return FAMILY_SNAPSHOT_PREFIX + groupId + ":" + date;
    }

    /** 完整 Key: family:online:members */
    public static String getOnlineMemberKey() {
        return ONLINE_MEMBER_CACHE_KEY;
    }

    /** 完整 Key: family:alarm:{groupId}:{date} */
    public static String getFamilyAlarmKey(Long groupId, String date) {
        return FAMILY_ALARM_PREFIX + groupId + ":" + date;
    }

    public static String getAuthRefreshTokenKey(Long userId) {
        return AUTH_REFRESHTOKEN_PREFIX + userId;
    }

    public static String getAuthRefreshTokenKey(String userId) {
        return AUTH_REFRESHTOKEN_PREFIX + userId;
    }
}