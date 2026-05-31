package com.whu.medicalbackend.agent.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Session 和消息自动清理服务
 * <p>
 * 定期清理过期的 agent_sessions、agent_messages、agent_pending_actions，
 * 防止数据库无限膨胀导致压测卡死。
 */
@Component
public class SessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupService.class);

    private final JdbcTemplate jdbcTemplate;
    private final int retentionDays;

    public SessionCleanupService(JdbcTemplate jdbcTemplate,
                                 @Value("${agent.session.retention-days:3}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.retentionDays = retentionDays;
        log.info("SessionCleanupService 初始化, retentionDays={}", retentionDays);
    }

    /**
     * 每天凌晨 3 点执行清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        log.info("开始清理过期会话和消息, retentionDays={}", retentionDays);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 1. 清理过期 agent_pending_actions
        int deletedPendingActions = jdbcTemplate.update(
                "DELETE FROM agent_pending_actions WHERE expires_at < ?",
                cutoff
        );
        if (deletedPendingActions > 0) {
            log.info("清理过期待确认操作: {} 条", deletedPendingActions);
        }

        // 2. 清理过期 tool_execution_pending
        int deletedToolPending = jdbcTemplate.update(
                "DELETE FROM tool_execution_pending WHERE expires_at < ?",
                cutoff
        );
        if (deletedToolPending > 0) {
            log.info("清理过期工具执行待确认: {} 条", deletedToolPending);
        }

        // 3. 先清理过期 session 关联的消息
        int deletedMessages = jdbcTemplate.update(
                "DELETE FROM agent_messages WHERE session_id IN (" +
                        "SELECT session_id FROM agent_sessions WHERE updated_at < ?" +
                        ")",
                cutoff
        );
        if (deletedMessages > 0) {
            log.info("清理过期会话消息: {} 条", deletedMessages);
        }

        // 4. 清理过期 session
        int deletedSessions = jdbcTemplate.update(
                "DELETE FROM agent_sessions WHERE updated_at < ?",
                cutoff
        );
        if (deletedSessions > 0) {
            log.info("清理过期会话: {} 个", deletedSessions);
        }

        log.info("会话清理完成");
    }
}
