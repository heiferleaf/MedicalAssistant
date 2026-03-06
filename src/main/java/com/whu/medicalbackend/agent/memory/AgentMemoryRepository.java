package com.whu.medicalbackend.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class AgentMemoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AgentMemoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void touchSession(String sessionId, String userId) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO agent_sessions(session_id, user_id, created_at, updated_at, summary_text) " +
                        "VALUES(?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), updated_at=VALUES(updated_at)",
                sessionId,
                userId,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                null
        );
    }

    public void appendMessage(String sessionId, String userId, String role, String content) {
        touchSession(sessionId, userId);
        jdbcTemplate.update(
                "INSERT INTO agent_messages(session_id, user_id, role, content, created_at) VALUES(?,?,?,?,?)",
                sessionId,
                userId,
                role,
                content,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public List<Map<String, Object>> getRecentMessages(String sessionId, int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT role, content, created_at FROM agent_messages WHERE session_id=? ORDER BY id DESC LIMIT ?",
                sessionId,
                Math.max(1, limit)
        );
        Collections.reverse(rows);
        return rows;
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
