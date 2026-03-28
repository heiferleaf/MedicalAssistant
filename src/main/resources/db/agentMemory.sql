-- ====================================
-- Agent Memory (Scheme A) - MySQL schema
-- 默认使用现有业务库：MedicalAssistant
-- 说明：Spring Boot 统一负责 memory 的持久化与确认动作状态
-- ====================================

CREATE TABLE IF NOT EXISTS agent_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    summary_text TEXT NULL,
    INDEX idx_agent_sessions_user_id (user_id),
    INDEX idx_agent_sessions_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 会话表';

CREATE TABLE IF NOT EXISTS agent_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL COMMENT 'user|assistant|tool',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_messages_session_id_id (session_id, id),
    INDEX idx_agent_messages_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 消息表';

CREATE TABLE IF NOT EXISTS agent_pending_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    preview_json TEXT NOT NULL,
    tool_args_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'pending|done|failed|canceled',
    result_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    INDEX idx_agent_pending_actions_session_id (session_id),
    INDEX idx_agent_pending_actions_user_id_status (user_id, status),
    INDEX idx_agent_pending_actions_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 待确认动作表';

-- ====================================
-- 增量更新脚本
-- 添加 action_type 和 action_data 字段到 agent_messages 表
-- ====================================

-- 添加 action_type 字段（如果不存在）
ALTER TABLE agent_messages 
ADD COLUMN action_type VARCHAR(64) NULL COMMENT '操作类型：plan, medicine, task, family 等' AFTER content;

-- 添加 action_data 字段（如果不存在）
ALTER TABLE agent_messages 
ADD COLUMN action_data TEXT NULL COMMENT '操作数据（JSON 格式）' AFTER action_type;

ALTER TABLE agent_messages 
MODIFY COLUMN content MEDIUMTEXT NOT NULL;