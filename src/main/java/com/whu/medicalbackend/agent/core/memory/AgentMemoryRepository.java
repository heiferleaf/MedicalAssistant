package com.whu.medicalbackend.agent.core.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class AgentMemoryRepository {

    private static final String SESSION_SEEN_PREFIX   = "agent:session:seen:";
    private static final String RECENT_MSG_PREFIX     = "agent:memory:recent:";
    // session-seen: 24h covers a full conversation lifetime
    private static final Duration SESSION_SEEN_TTL    = Duration.ofHours(24);
    // recent-messages: short TTL — invalidated on every write anyway, this is just a safety cap
    private static final Duration RECENT_MSG_TTL      = Duration.ofSeconds(30);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public AgentMemoryRepository(JdbcTemplate jdbcTemplate,
                                  ObjectMapper objectMapper,
                                  StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public void touchSession(String sessionId, String userId) {
        jdbcTemplate.update(
                "INSERT INTO agent_sessions(session_id, user_id, summary_text) " +
                        "VALUES(?,?,NULL) " +
                        "ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), updated_at=CURRENT_TIMESTAMP",
                sessionId,
                userId
        );
    }

    /**
     * touchSession that skips the DB upsert when Redis confirms the session was already created.
     * Saves one DB round-trip on every message after session creation.
     */
    private void touchSessionCached(String sessionId, String userId) {
        String key = SESSION_SEEN_PREFIX + sessionId;
        Boolean seen = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(seen)) {
            return;
        }
        touchSession(sessionId, userId);
        redisTemplate.opsForValue().set(key, "1", SESSION_SEEN_TTL);
    }

    /** Mark a session as known in Redis (called by AgentSessionController on explicit creation). */
    public void markSessionSeen(String sessionId) {
        redisTemplate.opsForValue().set(SESSION_SEEN_PREFIX + sessionId, "1", SESSION_SEEN_TTL);
    }

    /** Evict the session-seen flag (e.g. after delete). */
    public void evictSessionSeen(String sessionId) {
        redisTemplate.delete(SESSION_SEEN_PREFIX + sessionId);
    }

    // Cache up to this many messages per session — covers all callers (LLM=5, list=1, display=50 bypasses cache)
    private static final int RECENT_MSG_CACHE_MAX = 20;

    public void appendMessage(String sessionId, String userId, String role, String content) {
        touchSessionCached(sessionId, userId);
        jdbcTemplate.update(
                "INSERT INTO agent_messages(session_id, user_id, role, content, created_at) VALUES(?,?,?,?,?)",
                sessionId,
                userId,
                role,
                content,
                Timestamp.valueOf(LocalDateTime.now())
        );
        evictRecentMessages(sessionId);
    }

    /**
     * 保存带 action 的消息
     */
    public void appendMessageWithAction(String sessionId, String userId, String role, String content,
                                        String actionType, String actionData) {
        touchSessionCached(sessionId, userId);
        jdbcTemplate.update(
                "INSERT INTO agent_messages(session_id, user_id, role, content, action_type, action_data, created_at) " +
                "VALUES(?,?,?,?,?,?,?)",
                sessionId,
                userId,
                role,
                content,
                actionType,
                actionData,
                Timestamp.valueOf(LocalDateTime.now())
        );
        evictRecentMessages(sessionId);
    }

    public List<Map<String, Object>> getRecentMessages(String sessionId, int limit) {
        // For small limits, try the per-session Redis cache first
        if (limit <= RECENT_MSG_CACHE_MAX) {
            String key = RECENT_MSG_PREFIX + sessionId;
            try {
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    List<Map<String, Object>> all = objectMapper.readValue(
                            cached, objectMapper.getTypeFactory()
                                    .constructCollectionType(List.class, Map.class));
                    return all.size() <= limit ? all : all.subList(all.size() - limit, all.size());
                }
            } catch (Exception ignored) {
                // cache read failure is non-fatal — fall through to DB
            }
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT role, content, action_type, action_data, created_at FROM agent_messages WHERE session_id=? ORDER BY id DESC LIMIT ?",
                sessionId,
                Math.max(1, Math.max(limit, RECENT_MSG_CACHE_MAX))
        );
        Collections.reverse(rows);

        // Populate cache when the fetched batch fits within the cache window
        if (limit <= RECENT_MSG_CACHE_MAX) {
            try {
                String key = RECENT_MSG_PREFIX + sessionId;
                redisTemplate.opsForValue().set(key,
                        objectMapper.writeValueAsString(rows), RECENT_MSG_TTL);
            } catch (Exception ignored) {
                // cache write failure is non-fatal
            }
        }

        return rows.size() <= limit ? rows : rows.subList(rows.size() - limit, rows.size());
    }

    private void evictRecentMessages(String sessionId) {
        try {
            redisTemplate.delete(RECENT_MSG_PREFIX + sessionId);
        } catch (Exception ignored) {
            // eviction failure is non-fatal
        }
    }

    /**
     * Returns sessions for a user, each row already containing the last message content.
     * Single query replaces the previous N+1 pattern in AgentSessionController.
     */
    public List<Map<String, Object>> getUserSessionsWithLastMessage(String userId) {
        return jdbcTemplate.queryForList(
                "SELECT s.session_id, s.user_id, s.created_at, s.updated_at, s.summary_text, " +
                "       lm.content AS last_message_content " +
                "FROM agent_sessions s " +
                "LEFT JOIN agent_messages lm ON lm.id = (" +
                "    SELECT id FROM agent_messages WHERE session_id = s.session_id ORDER BY id DESC LIMIT 1" +
                ") " +
                "WHERE s.user_id = ? " +
                "ORDER BY s.updated_at DESC",
                userId
        );
    }

    public List<Map<String, Object>> getUserSessions(String userId) {
        return jdbcTemplate.queryForList(
                "SELECT session_id, user_id, created_at, updated_at, summary_text " +
                "FROM agent_sessions WHERE user_id=? ORDER BY updated_at DESC",
                userId
        );
    }

    public void deleteSession(String sessionId) {
        jdbcTemplate.update("DELETE FROM agent_messages WHERE session_id=?", sessionId);
        jdbcTemplate.update("DELETE FROM agent_sessions WHERE session_id=?", sessionId);
    }

    public void deleteSessionMessages(String sessionId) {
        jdbcTemplate.update("DELETE FROM agent_messages WHERE session_id=?", sessionId);
    }

    /**
     * 更新会话摘要
     */
    public void updateSessionSummary(String sessionId, String summary) {
        jdbcTemplate.update(
            "UPDATE agent_sessions SET summary_text = ? WHERE session_id = ?",
            summary,
            sessionId
        );
    }

    public void savePendingAction(
            String actionId,
            String sessionId,
            String userId,
            String actionType,
            JsonNode preview,
            JsonNode toolArgs,
            Duration expiresIn
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(Math.max(1, expiresIn.getSeconds()));

        jdbcTemplate.update(
                "INSERT INTO agent_pending_actions(" +
                        "action_id, session_id, user_id, action_type, preview_json, tool_args_json, status, result_json, created_at, expires_at" +
                        ") VALUES(?,?,?,?,?,?,?,?,?,?)",
                actionId,
                sessionId,
                userId,
                actionType,
                toJson(preview),
                toJson(toolArgs),
                "pending",
                null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(expiresAt)
        );
    }

    public Optional<PendingActionRecord> getPendingAction(String actionId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT * FROM agent_pending_actions WHERE action_id=?",
                    actionId
            );

            LocalDateTime expiresAt = toLocalDateTime(row.get("expires_at"));
            if (expiresAt.isBefore(LocalDateTime.now())) {
                jdbcTemplate.update("DELETE FROM agent_pending_actions WHERE action_id=?", actionId);
                return Optional.empty();
            }

            PendingActionRecord record = new PendingActionRecord(
                    (String) row.get("action_id"),
                    (String) row.get("session_id"),
                    (String) row.get("user_id"),
                    (String) row.get("action_type"),
                    (String) row.get("preview_json"),
                    (String) row.get("tool_args_json"),
                    (String) row.get("status"),
                    (String) row.get("result_json"),
                toLocalDateTime(row.get("created_at")),
                    expiresAt
            );

            return Optional.of(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updatePendingActionStatus(String actionId, String status, JsonNode result) {
        jdbcTemplate.update(
                "UPDATE agent_pending_actions SET status=?, result_json=? WHERE action_id=?",
                status,
                result == null ? null : toJson(result),
                actionId
        );
    }

    private String toJson(JsonNode node) {
        if (node == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialize failed", e);
        }
    }

    public JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return LocalDateTime.MIN;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.util.Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime();
        }
        if (value instanceof CharSequence cs) {
            try {
                return LocalDateTime.parse(cs.toString());
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Unsupported datetime value type: " + value.getClass());
    }

    public record PendingActionRecord(
            String actionId,
            String sessionId,
            String userId,
            String actionType,
            String previewJson,
            String toolArgsJson,
            String status,
            String resultJson,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
    }
}
